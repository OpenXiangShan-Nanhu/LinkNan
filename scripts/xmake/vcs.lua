import("core.base.option")
import("core.project.depend")
import("core.base.task")

function simv_comp(num_cores)
  if not option.get("no_fsdb") then
    if not os.getenv("VERDI_HOME") then
      print("error: VERDI_HOME is not set!")
      os.exit(1, true)
    end
  end
  local abs_base = os.curdir()
  local chisel_dep_srcs = os.filedirs(path.join(abs_base, "src", "**.scala"))
  table.join2(chisel_dep_srcs, os.filedirs(path.join(abs_base, "dependencies", "**.scala")))
  table.join2(chisel_dep_srcs, {path.join(abs_base, "build.sc")})
  table.join2(chisel_dep_srcs, {path.join(abs_base, "xmake.lua")})

  local vtop = "SimTop"
  local build_dir = path.join(abs_base, "build")
  local comp_dir = path.join(abs_base, "sim", "simv", "comp")
  if not os.exists(comp_dir) then os.mkdir(comp_dir) end
  local dpi_export_dir = path.join(comp_dir, "dpi_export")
  local design_vsrc = path.join(abs_base, "build", "rtl")
  local design_gen_dir = path.join(abs_base, "build", "generated-src")
  local difftest = path.join(abs_base, "dependencies", "difftest")
  local difftest_vsrc = path.join(difftest, "src", "test", "vsrc")
  local difftest_vsrc_common = path.join(difftest_vsrc, "common")
  local difftest_vsrc_top = path.join(difftest_vsrc, "vcs")
  local difftest_csrc = path.join(difftest, "src", "test", "csrc")
  local difftest_csrc_common = path.join(difftest_csrc, "common")
  local difftest_csrc_difftest = path.join(difftest_csrc, "difftest")
  local difftest_csrc_spikedasm = path.join(difftest_csrc, "plugin", "spikedasm")
  local difftest_csrc_vcs = path.join(difftest_csrc, "vcs")
  local difftest_config = path.join(difftest, "config")

  depend.on_changed(function ()
    if os.exists(build_dir) then os.rmdir(build_dir) end
    task.run("soc", {
      vcs = true, sim = true, config = option.get("config"),
      socket = option.get("socket"), lua_scoreboard = option.get("lua_scoreboard"),
      core = option.get("core")
    })
    local vsrc = os.files(path.join(design_vsrc, "*v"))
    table.join2(vsrc, os.files(path.join(difftest_vsrc_common, "*v")))
    table.join2(vsrc, os.files(path.join(difftest_vsrc_top, "*v")))

    if option.get("lua_scoreboard") then
      local dpi_cfg_lua = path.join(abs_base, "scripts", "verilua", "dpi_cfg.lua")
      if os.exists(dpi_export_dir) then os.rmdir(dpi_export_dir) end
      os.mkdir(dpi_export_dir)
      local dpi_exp_opts =  {"dpi_exporter"}
      table.join2(dpi_exp_opts, {"--config", dpi_cfg_lua})
      table.join2(dpi_exp_opts, {"--out-dir", dpi_export_dir})
      table.join2(dpi_exp_opts, {"--work-dir", dpi_export_dir})
      table.join2(dpi_exp_opts, {"-I", design_gen_dir})
      table.join2(dpi_exp_opts, {"--quiet"})
      table.join2(dpi_exp_opts, {"--top", vtop})
      table.join2(dpi_exp_opts, vsrc)
      local cmd_file = path.join(comp_dir, "dpi_exp_cmd.sh")
      io.writefile(cmd_file, table.concat(dpi_exp_opts, " "))
      os.execv(os.shell(), { cmd_file })
    end
  end,{
    files = chisel_dep_srcs,
    dependfile = path.join("out", "chisel.simv.dep"),
    dryrun = option.get("rebuild")
  })

  local vsrc = os.files(path.join(design_vsrc, "*v"))
  table.join2(vsrc, os.files(path.join(difftest_vsrc_common, "*v")))
  table.join2(vsrc, os.files(path.join(difftest_vsrc_top, "*v")))
  if option.get("lua_scoreboard") then
    vsrc = os.files(path.join(dpi_export_dir, "*v"))
  end

  local csrc = os.files(path.join(design_gen_dir, "*.cpp"))
  table.join2(csrc, os.files(path.join(difftest_csrc_common, "*.cpp")))
  table.join2(csrc, os.files(path.join(difftest_csrc_spikedasm, "*.cpp")))
  table.join2(csrc, os.files(path.join(difftest_csrc_vcs, "*.cpp")))
  if option.get("lua_scoreboard") then
    table.join2(csrc, path.join(dpi_export_dir, "dpi_func.cpp"))
  end

  local headers = os.files(path.join(design_gen_dir, "*.h"))
  table.join2(headers, os.files(path.join(difftest_csrc_common, "*.h")))
  table.join2(headers, os.files(path.join(difftest_csrc_spikedasm, "*.h")))
  table.join2(headers, os.files(path.join(difftest_csrc_vcs, "*.h")))

  if not option.get("no_diff") then
    table.join2(csrc, os.files(path.join(difftest_csrc_difftest, "*.cpp")))
    table.join2(headers, os.files(path.join(difftest_csrc_difftest, "*.h")))
  end

  local vsrc_filelist_path = path.join(comp_dir, "vsrc.f")
  local vsrc_filelist_contents = ""
  for _, f in ipairs(vsrc) do
    vsrc_filelist_contents = vsrc_filelist_contents .. f .. "\n"
  end
  io.writefile(vsrc_filelist_path, vsrc_filelist_contents)

  local csrc_filelist_path = path.join(comp_dir, "csrc.f")
  local csrc_filelist_contents = ""
  for _, f in ipairs(csrc) do
    csrc_filelist_contents = csrc_filelist_contents .. f .. "\n"
  end
  io.writefile(csrc_filelist_path, csrc_filelist_contents)

  local cxx_flags = "-std=c++17 -static -Wall -DNUM_CORES=" .. num_cores
  cxx_flags = cxx_flags .. " -I" .. design_gen_dir
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_common
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_difftest
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_spikedasm
  cxx_flags = cxx_flags .. " -I" .. difftest_config
  if option.get("ref") == "Spike" then
    cxx_flags = cxx_flags .. " -DREF_PROXY=SpikeProxy"
  else
    cxx_flags = cxx_flags .. " -DREF_PROXY=NemuProxy"
  end
  if option.get("sparse_mem") then
    cxx_flags = cxx_flags .. " -DCONFIG_USE_SPARSEMM"
  end
  if option.get("no_diff") then
    cxx_flags = cxx_flags .. " -DCONFIG_NO_DIFFTEST"
  end

  local cxx_ldflags = "-Wl,--no-as-needed -lpthread -lSDL2 -ldl -lz -lzstd"

  local vcs_flags = "-cm_dir " .. path.join(comp_dir, "simv")
  vcs_flags = vcs_flags .. " -full64 +v2k -timescale=1ns/1ns -sverilog -j200"
  vcs_flags = vcs_flags .. " -debug_access +lint=TFIPC-L -l vcs.log -top tb_top"
  vcs_flags = vcs_flags .. " -fgp -lca -kdb +nospecify +notimingcheck -no_save"
  vcs_flags = vcs_flags .. " +define+PRINTF_COND=1 +define+VCS"
  vcs_flags = vcs_flags .. " +define+CONSIDER_FSDB +define+SIM_TOP_MODULE_NAME=tb_top.sim"
  if not option.get("no_fsdb") then
    novas = path.join(os.getenv("VERDI_HOME"), "share", "PLI", "VCS", "LINUX64")
    vcs_flags = vcs_flags .. " -P " .. path.join(novas, "novas.tab")
    vcs_flags = vcs_flags .. " " .. path.join(novas, "pli.a")
  end
  if not option.get("no_diff") then
    vcs_flags = vcs_flags .. " +define+DIFFTEST"
  end
  vcs_flags = vcs_flags .. " -CFLAGS \"" .. cxx_flags .. "\""
  vcs_flags = vcs_flags .. " -LDFLAGS \"" .. cxx_ldflags .. "\""
  vcs_flags = vcs_flags .. " -f " .. vsrc_filelist_path
  vcs_flags = vcs_flags .. " -f " .. csrc_filelist_path
  vcs_flags = vcs_flags .. " +incdir+" .. design_gen_dir
  if option.get("lua_scoreboard") then
    vcs_flags = "vl-vcs-dpi " .. vcs_flags
  else
    vcs_flags = "vcs " .. vcs_flags
  end

  if option.get("core") == "boom" then
    vcs_flags = vcs_flags .. " +define+RANDOMIZE_GARBAGE_ASSIGN +define+RANDOMIZE_DELAY=0"
    vcs_flags = vcs_flags .. " +define+RANDOMIZE_REG_INIT +define+RANDOMIZE_MEM_INIT"
  else
    if option.get("no_xprop") then
      vcs_flags = vcs_flags .. " +vcs+initreg+random"
    else
      vcs_flags = vcs_flags .. " -xprop"
    end
  end

  local cmd_file = path.join(comp_dir, "vcs_cmd.sh")
  if os.exists(cmd_file) then
    local fileStr = io.readfile(cmd_file)
    if fileStr ~= vcs_flags then
      io.writefile(cmd_file, vcs_flags)
    end
  else
    io.writefile(cmd_file, vcs_flags)
  end

  local depend_srcs = vsrc
  table.join2(depend_srcs, csrc)
  table.join2(depend_srcs, headers)
  table.join2(depend_srcs, { path.join(abs_base, "scripts", "xmake", "vcs.lua") })
  table.join2(depend_srcs, { cmd_file })

  os.cd(comp_dir)
  depend.on_changed(function()
    print(vcs_flags)
    os.execv(os.shell(), { "vcs_cmd.sh" })
  end, {
    files = depend_srcs,
    dependfile = path.join(comp_dir, "simv.ln.dep"),
    dryrun = option.get("rebuild")
  })
end

-- option: no_dump
-- option: no_diff
-- option: image
-- option: imagez
-- option: case_dir
-- option: ref
-- option: ref_dir

function simv_run()
  assert(option.get("image") or option.get("imagez"))
  local abs_dir = os.curdir()
  local image_file = ""
  local abs_case_base_dir = path.join(abs_dir, option.get("case_dir"))
  local abs_ref_base_dir = path.join(abs_dir, option.get("ref_dir"))
  if option.get("imagez") then image_file = path.join(abs_case_base_dir, option.get("imagez") .. ".gz") end
  if option.get("image") then image_file = path.join(abs_case_base_dir, option.get("image") .. ".bin") end
  local image_basename = path.basename(image_file)
  local sim_dir = path.join("sim", "simv", image_basename)
  local ref_so = path.join(abs_ref_base_dir, option.get("ref"))
  local simv = path.join(sim_dir, "simv")
  local daidir = path.join(sim_dir, "simv.daidir")
  if not os.exists(sim_dir) then os.mkdir(sim_dir) end
  if os.exists(simv) then os.rm(simv) end
  if os.exists(daidir) then os.rm(daidir) end
  os.ln(path.join(abs_dir, "sim", "simv", "comp", "simv"), simv)
  os.ln(path.join(abs_dir, "sim", "simv", "comp", "simv.daidir"), daidir)
  os.cd(sim_dir)
  local sh_str = "chmod +x simv" .. " && ( ./simv"
  if not option.get("no_dump") then
    sh_str = sh_str .. " +dump-wave=fsdb"
  end
  if option.get("init_reg") ~= nil then
    sh_str = sh_str .. " +vcs+initreg+" .. option.get("init_reg")
  end
  sh_str = sh_str .. " +diff=" .. ref_so
  sh_str = sh_str .. " +max-cycles=" .. option.get("cycles")
  sh_str = sh_str .. " +workload=" .. image_file
  sh_str = sh_str .. " -fgp=num_threads:4,num_fsdb_threads:4"
  sh_str = sh_str .. " -assert finish_maxfail=30"
  sh_str = sh_str .. " -assert global_finish_maxfail=10000"
  sh_str = sh_str .. " ) 2>assert.log |tee run.log"
  io.writefile("tmp.sh", sh_str)
  print(sh_str)
  os.execv(os.shell(), {"tmp.sh"})
  os.rm("tmp.sh")
end
