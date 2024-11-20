import("core.base.option")
import("core.project.depend")
import("core.base.task")

function emu_comp()
  local abs_base = os.curdir()
  local chisel_dep_srcs = os.iorun("find " .. abs_base .. " -name \"*.scala\""):split('\n')
  table.join2(chisel_dep_srcs, {path.join(abs_base, "build.sc")})
  table.join2(chisel_dep_srcs, {path.join(abs_base, "xmake.lua")})

  local build_dir = path.join(abs_base, "build")
  depend.on_changed(function ()
    if os.exists(build_dir) then os.rmdir(build_dir) end
    task.run("soc", {
      sim = true, config = option.get("config"),
      dramsim3 = option.get("dramsim3"), enable_perf = not option.get("no_perf"),
      cpu_sync = option.get("cpu_sync")
    })
  end,{
    files = chisel_dep_srcs,
    dependfile = path.join("out", "chisel.verilator.dep"),
    dryrun = option.get("rebuild")
  })
  local comp_dir = path.join(abs_base, "sim", "emu", "comp")
  local comp_target = path.join(comp_dir, "emu")
  if not os.exists(comp_dir) then os.mkdir(comp_dir) end
  local design_vsrc = path.join(abs_base, "build", "rtl")
  local design_csrc = path.join(abs_base, "build", "generated-src")
  local difftest = path.join(abs_base, "dependencies", "difftest")
  local difftest_vsrc = path.join(difftest, "src", "test", "vsrc")
  local difftest_vsrc_common = path.join(difftest_vsrc, "common")
  local difftest_csrc = path.join(difftest, "src", "test", "csrc")
  local difftest_csrc_common = path.join(difftest_csrc, "common")
  local difftest_csrc_difftest = path.join(difftest_csrc, "difftest")
  local difftest_csrc_spikedasm = path.join(difftest_csrc, "plugin", "spikedasm")
  local difftest_csrc_verilator = path.join(difftest_csrc, "verilator")
  local difftest_config = path.join(difftest, "config")

  local vsrc = os.files(path.join(design_vsrc, "*v"))
  table.join2(vsrc, os.files(path.join(difftest_vsrc_common, "*v")))

  local csrc = os.files(path.join(design_csrc, "*.cpp"))
  table.join2(csrc, os.files(path.join(difftest_csrc_common, "*.cpp")))
  table.join2(csrc, os.files(path.join(difftest_csrc_difftest, "*.cpp")))
  table.join2(csrc, os.files(path.join(difftest_csrc_spikedasm, "*.cpp")))
  table.join2(csrc, os.files(path.join(difftest_csrc_verilator, "*.cpp")))

  local headers = os.files(path.join(design_csrc, "*.h"))
  table.join2(headers, os.files(path.join(difftest_csrc_common, "*.h")))
  table.join2(headers, os.files(path.join(difftest_csrc_difftest, "*.h")))
  table.join2(headers, os.files(path.join(difftest_csrc_spikedasm, "*.h")))
  table.join2(headers, os.files(path.join(difftest_csrc_verilator, "*.h")))

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

  local cxx_flags = "-std=c++17 -DVERILATOR -DNUM_CORES=1"
  local cxx_ldflags = "-ldl -lrt -lpthread -lsqlite3 -lz"
  cxx_flags = cxx_flags .. " -I" .. difftest_config
  cxx_flags = cxx_flags .. " -I" .. design_csrc
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_common
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_difftest
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_spikedasm
  cxx_flags = cxx_flags .. " -I" .. difftest_csrc_verilator
  if option.get("ref") == "Spike" then
    cxx_flags = cxx_flags .. " -DREF_PROXY=SpikeProxy -DSELECTEDSpike"
  else
    cxx_flags = cxx_flags .. " -DREF_PROXY=NemuProxy -DSELECTEDNemu"
  end
  if option.get("sparse_mem") then
    cxx_flags = cxx_flags .. " -DCONFIG_USE_SPARSEMM"
  end
  local dramsim_a = ""
  if option.get("dramsim3") then
    local ds_cxx_flags, ds_cxx_ldflags = import("dramsim").dramsim(option.get("dramsim3_home"), build_dir)
    cxx_flags = cxx_flags .. ds_cxx_flags
    dramsim_a = ds_cxx_ldflags
    cxx_ldflags = cxx_ldflags .. ds_cxx_ldflags
  end
  if option.get("threads") then
    cxx_flags = cxx_flags .. " -DEMU_THREAD=" .. option.get("threads")
  end

  local verilator_flags = "verilator --exe --cc --top-module SimTop --assert --x-assign unique --trace"
  verilator_flags = verilator_flags .. " +define+VERILATOR=1 +define+PRINTF_COND=1 +define+DIFFTEST"
  verilator_flags = verilator_flags .. " +define+RANDOMIZE_REG_INIT +define+RANDOMIZE_MEM_INIT"
  verilator_flags = verilator_flags .. " +define+RANDOMIZE_GARBAGE_ASSIGN +define+RANDOMIZE_DELAY=0"
  verilator_flags = verilator_flags .. " -Wno-UNOPTTHREADS -Wno-STMTDLY -Wno-WIDTH --no-timing"
  verilator_flags = verilator_flags .. " --stats-vars --output-split 30000 -output-split-cfuncs 30000"
  if option.get("threads") then
    verilator_flags = verilator_flags .. " --threads " .. option.get("threads") .. " --threads-dpi all"
  end
  verilator_flags = verilator_flags .. " -CFLAGS \"" .. cxx_flags .. "\""
  verilator_flags = verilator_flags .. " -LDFLAGS \"" .. cxx_ldflags .. "\""
  verilator_flags = verilator_flags .. " -Mdir " .. comp_dir
  verilator_flags = verilator_flags .. " -I" .. design_csrc
  verilator_flags = verilator_flags .. " -f " .. vsrc_filelist_path
  verilator_flags = verilator_flags .. " -f " .. csrc_filelist_path
  verilator_flags = verilator_flags .. " -o " .. comp_target

  os.cd(comp_dir)
  io.writefile("verilator_cmd.sh", verilator_flags)

  local verilator_depends_files = vsrc
  table.join2(verilator_depends_files, { path.join(abs_base, "scripts", "xmake", "verilator.lua") })
  table.join2(verilator_depends_files, { path.join(abs_base, "scripts", "xmake", "dramsim.lua") })

  depend.on_changed(function()
    print(verilator_flags)
    os.execv(os.shell(), { "verilator_cmd.sh" })
  end, {
    files = verilator_depends_files,
    dependfile = path.join(comp_dir, "verilator.ln.dep")
  })

  local gmake_depend_files = csrc
  table.join2(gmake_depend_files, headers)
  table.join2(gmake_depend_files, {path.join(comp_dir, "VSimTop.mk"), dramsim_a})

  depend.on_changed(function()
    local make_opts = {"VM_PARALLEL_BUILDS=1",  "OPT_FAST=-O3"}
    table.join2(make_opts, {"-f", "VSimTop.mk", "-j", option.get("jobs")})
    os.execv("make", make_opts)
  end, {
    files = gmake_depend_files,
    dependfile = path.join(comp_dir, "emu.ln.dep")
  })

  local emu_target = path.join(build_dir, "emu")
  if not os.exists(emu_target) then
    os.ln(path.join(comp_dir, "emu"), emu_target)
  end
  
end

function emu_run()
  assert(option.get("image") or option.get("imagez"))
  local abs_dir = os.curdir()
  local image_file = ""
  local abs_case_base_dir = path.join(abs_dir, option.get("case_dir"))
  local abs_ref_base_dir = path.join(abs_dir, option.get("ref_dir"))
  if option.get("imagez") then image_file = path.join(abs_case_base_dir, option.get("imagez") .. ".gz") end
  if option.get("image") then image_file = path.join(abs_case_base_dir, option.get("image") .. ".bin") end
  local warmup = option.get("warmup")
  local instr = option.get("instr")
  local wave_begin = option.get("begin")
  local wave_end = option.get("end")
  local image_basename = path.basename(image_file)
  local sim_dir = path.join("sim", "emu", image_basename)
  local ref_so = path.join(abs_ref_base_dir, option.get("ref"))
  local sim_emu = path.join(sim_dir, "emu")
  if not os.exists(sim_dir) then os.mkdir(sim_dir) end
  if os.exists(sim_emu) then os.rm(sim_emu) end
  os.ln(path.join(abs_dir, "sim", "emu", "comp", "emu"), sim_emu)
  os.cd(sim_dir)
  local sh_str = "chmod +x emu" .. " && ( ./emu"
  if not option.get("dump") then
    sh_str = sh_str .. " --enable-fork -X " .. option.get("fork")
  else
    sh_str = sh_str .. " --dump-wave"
    if(wave_begin ~= "0") then sh_str = sh_str .. " -b " .. wave_begin end
    if(wave_end ~= "0") then sh_str = sh_str .. " -e " .. wave_end end
  end
  if(warmup ~= "0") then sh_str = sh_str .. " -W " .. warmup end
  if(instr ~= "0") then sh_str = sh_str .. " -I " .. instr end
  sh_str = sh_str .. " --diff " .. ref_so
  sh_str = sh_str .. " -i " .. image_file
  sh_str = sh_str .. " -s " .. option.get("seed")
  sh_str = sh_str .. " --wave-path " .. image_basename .. ".vcd"
  sh_str = sh_str .. " ) 2>assert.log |tee run.log"
  io.writefile("tmp.sh", sh_str)
  print(sh_str)
  os.execv(os.shell(), {"tmp.sh"})
  os.rm("tmp.sh")
end