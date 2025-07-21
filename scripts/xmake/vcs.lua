---@diagnostic disable

import("core.base.option")
import("core.project.depend")
import("core.base.task")

local tb_top = "tb_top"
local cov_param = "line+cond+fsm+tgl"

function simv_comp(num_cores)
  if not option.get("no_fsdb") then
    if not os.getenv("VERDI_HOME") then
      raise("[vcs.lua] [simv_comp] error: VERDI_HOME is not set!")
    end
  end

  local abs_base = os.curdir()
  local build_dir = path.join(abs_base, "build")
  local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
  if new_build_dir then build_dir = path.absolute(new_build_dir) end

  local chisel_dep_srcs = os.filedirs(path.join(abs_base, "src", "**.scala"))
  table.join2(chisel_dep_srcs, os.filedirs(path.join(abs_base, "dependencies", "**.scala")))
  table.join2(chisel_dep_srcs, {path.join(abs_base, "build.sc")})
  table.join2(chisel_dep_srcs, {path.join(abs_base, "xmake.lua")})
  if option.get("jar") ~= "" then chisel_dep_srcs = option.get("jar") end

  local sim_dir = path.join(abs_base, "sim")
  local new_sim_dir = option.get("sim_dir") or os.getenv("SIM_DIR")
  if new_sim_dir then sim_dir = path.absolute(new_sim_dir) end

  local comp_dir = path.join(sim_dir, "simv", "comp")
  if not os.exists(comp_dir) then os.mkdir(comp_dir) end
  local dpi_export_dir = path.join(comp_dir, "dpi_export")
  local design_vsrc = path.join(build_dir, "rtl")

  local design_gen_dir = path.join(build_dir, "generated-src")
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

  ---------------------------------
  -- Add verilog file paths
  ---------------------------------
  local vsrc = {}
  local vsrc_dirs = {
    design_vsrc,
    difftest_vsrc_common,
    difftest_vsrc_top,
    -- Add more paths here
  }
  for _, p in ipairs(vsrc_dirs) do
    table.join2(vsrc, os.files(path.join(p, "*v")))
  end

  ---------------------------------
  -- Add c file paths
  ---------------------------------
  local csrc = {}
  local csrc_dirs = {
    design_gen_dir,
    difftest_csrc_common,
    difftest_csrc_spikedasm,
    difftest_csrc_vcs,
    -- Add more paths here
  }
  for _, p in ipairs(csrc_dirs) do
    table.join2(csrc, os.files(path.join(p, "*.cpp")))
    table.join2(csrc, os.files(path.join(p, "*.c")))
  end

  ---------------------------------
  -- Add header file paths
  ---------------------------------
  local headers = {}
  local headers_dirs = {
    design_gen_dir,
    difftest_csrc_common,
    difftest_csrc_spikedasm,
    difftest_csrc_vcs,
    -- Add more paths here
  }
  for _, p in ipairs(headers_dirs) do
    table.join2(headers, os.files(path.join(p, "*.h")))
  end

  depend.on_changed(function ()
    if os.exists(build_dir) then os.rmdir(build_dir) end
    task.run("soc", {
      vcs = true, sim = true, config = option.get("config"),
      socket = option.get("socket"), lua_scoreboard = option.get("lua_scoreboard"),
      core = option.get("core"), l3 = option.get("l3"), noc = option.get("noc"),
      legacy = option.get("legacy"), jar = option.get("jar"),
      build_dir = build_dir
    })
    vsrc = {}
    for _, p in ipairs(vsrc_dirs) do
      table.join2(vsrc, os.files(path.join(p, "*v")))
    end

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
      table.join2(dpi_exp_opts, {"--top", tb_top})
      table.join2(dpi_exp_opts, vsrc)
      local cmd_file = path.join(comp_dir, "dpi_exp_cmd.sh")
      io.writefile(cmd_file, table.concat(dpi_exp_opts, " "))
      os.execv(os.shell(), { cmd_file })
    end
  end,{
    files = chisel_dep_srcs,
    dependfile = path.join("out", "chisel.simv.dep." .. (build_dir):gsub("/", "_"):gsub(" ", "_")),
    dryrun = option.get("rebuild"),
    values = table.join2({build_dir}, xmake.argv())
  })

  assert(#os.files(path.join(design_vsrc, "*v")) > 0, "[vcs.lua] [simv_comp] rtl dir(`%s`) is empty!", design_vsrc)

  if option.get("lua_scoreboard") then
    vsrc = os.files(path.join(dpi_export_dir, "*v"))
    table.join2(csrc, path.join(dpi_export_dir, "dpi_func.cpp"))
  end

  if not option.get("no_diff") then
    table.join2(csrc, os.files(path.join(difftest_csrc_difftest, "*.cpp")))
    table.join2(headers, os.files(path.join(difftest_csrc_difftest, "*.h")))
  end

  local vsrc_filelist_path = path.join(comp_dir, "vsrc.f")
  local vsrc_filelist_contents = ""
  for _, f in ipairs(vsrc) do
    vsrc_filelist_contents = vsrc_filelist_contents .. f .. "\n"
  end
  os.tryrm(vsrc_filelist_path)
  io.writefile(vsrc_filelist_path, vsrc_filelist_contents)

  local csrc_filelist_path = path.join(comp_dir, "csrc.f")
  local csrc_filelist_contents = ""
  for _, f in ipairs(csrc) do
    csrc_filelist_contents = csrc_filelist_contents .. f .. "\n"
  end
  os.tryrm(csrc_filelist_path)
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
  if option.get("lua_scoreboard") then
    cxx_flags = cxx_flags .. " -DDPI_EXP_CALL_VERILUA_ENV_STEP"
  end

  local cxx_ldflags = "-Wl,--no-as-needed -lpthread -lSDL2 -ldl -lz -lzstd"

  local vcs_flags = "-cm_dir " .. path.join(comp_dir, "simv")
  vcs_flags = vcs_flags .. " -full64 +v2k -timescale=1ns/10ps -sverilog -j200"
  vcs_flags = vcs_flags .. " -debug_access +lint=TFIPC-L -l vcs.log -top " .. tb_top
  vcs_flags = vcs_flags .. " -fgp -lca -kdb +nospecify +notimingcheck -no_save"
  vcs_flags = vcs_flags .. " +define+PRINTF_COND=1 +define+VCS"
  vcs_flags = vcs_flags .. " +define+CONSIDER_FSDB +define+SIM_TOP_MODULE_NAME=" .. tb_top .. ".sim"
  if option.get("bypass_clockgate") then
        vcs_flags = vcs_flags .. "  +define+BYPASS_CLOCKGATE"
  end
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
    vcs_flags = vcs_flags .. " +define+MANUALLY_CALL_DPI_EXPORTER_TICK"
    vcs_flags = vcs_flags .. " +define+DUT_CLEAN"
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

  if option.get("cov") then
    vcs_flags = vcs_flags .. " -cm " .. cov_param
  end

  if option.get("vcs_args") then
    vcs_flags = vcs_flags .. " " .. option.get("vcs_args")
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
    dependfile = path.join(comp_dir, "simv.ln.dep." .. (sim_dir):gsub("/", "_"):gsub(" ", "_")),
    dryrun = option.get("rebuild"),
    values = table.join2({sim_dir}, xmake.argv())
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
  assert(
    option.get("image") or option.get("imagez") or option.get("workload"),
    "[vcs.lua] [simv_run] must set one of `image(-i)`, `imagez(-z)` or `workload(-w)`"
  )

  local abs_dir = os.curdir()
  local image_file = ""
  local flash_file = ""
  local abs_case_base_dir = path.join(abs_dir, option.get("case_dir"))
  local abs_ref_base_dir = path.join(abs_dir, option.get("ref_dir"))

  if option.get("imagez") then image_file = path.join(abs_case_base_dir, option.get("imagez") .. ".gz") end
  if option.get("image") then image_file = path.join(abs_case_base_dir, option.get("image") .. ".bin") end
  if option.get("workload") then image_file = path.absolute(option.get("workload")) end
  if option.get("flash") then flash_file = path.join(abs_case_base_dir, option.get("flash") .. ".bin") end

  local w_cnt = 0
  if option.get("image") then w_cnt = w_cnt + 1 end
  if option.get("imagez") then w_cnt = w_cnt + 1 end
  if option.get("workload") then w_cnt = w_cnt + 1 end
  if w_cnt ~= 1 then
    raise("[vcs.lua] [simv_run] `image(-i)`, `imagez(-z)` and `workload(-w)` cannot be both set")
  end

  local case_name = path.basename(image_file)
  if option.get("case_name") ~= nil then case_name = option.get("case_name") end

  local sim_dir = path.join(abs_dir, "sim")
  local new_sim_dir = option.get("sim_dir") or os.getenv("SIM_DIR")
  if new_sim_dir then sim_dir = path.absolute(new_sim_dir) end

  local simv_sim_dir = path.join(sim_dir, "simv")
  local simv_case_dir = path.join(simv_sim_dir, case_name)
  local simv_comp_dir = path.join(simv_sim_dir, "comp")
  local ref_so = path.join(abs_ref_base_dir, option.get("ref"))
  local simv = path.join(simv_case_dir, "simv")
  local daidir = path.join(simv_case_dir, "simv.daidir")

  if not os.exists(simv_comp_dir) then 
    raise(format(
      "[vcs.lua] [simv_run] comp_dir(`%s`) does not exist, maybe you should run `xmake vcs <flags>` first", 
      simv_comp_dir
    ))
  end

  if not os.exists(simv_case_dir) then os.mkdir(simv_case_dir) end
  if os.exists(simv) then os.rm(simv) end
  if os.exists(daidir) then os.rm(daidir) end

  os.ln(path.join(simv_comp_dir, "simv"), simv)
  os.ln(path.join(simv_comp_dir, "simv.daidir"), daidir)
  os.cd(simv_case_dir)

  local sh_str = "chmod +x simv" .. " && ( ./simv"
  if not option.get("no_dump") then
    sh_str = sh_str .. " +dump-wave=fsdb"
  end
  if option.get("init_reg") ~= nil then
    sh_str = sh_str .. " +vcs+initreg+" .. option.get("init_reg")
  end
  if option.get("cov") then
    sh_str = sh_str .. " -cm " .. cov_param
  end
  if option.get("simv_args") then 
    sh_str = sh_str .. " " .. option.get("simv_args")
  end
  if flash_file ~= "" then 
    sh_str = sh_str .. " +flash=" .. flash_file 
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
