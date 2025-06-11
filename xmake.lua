local prj_dir = os.curdir()

task("soc" , function()
  set_menu {
    usage = "xmake soc [options]",
    description = "Generate soc rtl",
    options = {
      {'a', "all_in_one", "k", nil, "do not split generated rtl"},
      {'A', "hardware_assertion", "k", nil, "enable hardware assertion"},
      {'b', "block_test_l2l3", "k", nil, "leave core interfaces to the top"},
      {'k', "keep_l1", "k", nil, "keep dcache interfaces to the top"},
      {'c', "clean_difftest", "k", nil, "generate verilog without any difftest components"},
      {'d', "dramsim3", "k", nil, "use dramsim3 as simulation main memory"},
      {'e', "enable_perf", "k", nil, "generate verilog with perf debug components"},
      {'g', "vcs", "k", nil, "alter assertions info to be vcs style"},
      {'r', "release", "k", nil, "export release pack"},
      {'s', "sim", "k", nil, "generate simulation top"},
      {'m', "mbist", "k", nil, "generate simulation top"},
      {'p', "pldm_verilog", "k", nil, "enable only basic difftest function"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'x', "prefix", "kv", "", "assign a prefix for rtl modules"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "full", "define L3 config"},
      {'N', "noc", "kv", "full", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"},
      {'o', "out_dir", "kv", "build/rtl", "assign build dir"},
      {'j', "jobs", "kv", "16", "post-compile process jobs"}
    }
  }
  local chisel_opts =  {"-i"}

  on_run(function()
    import("core.base.option")
    print(option.get("options"))
    if option.get("sim") then table.join2(chisel_opts, {"linknan.test.runMain"}) else table.join2(chisel_opts, {"linknan.runMain"}) end
    if option.get("sim") then table.join2(chisel_opts, {"lntest.top.SimGenerator"}) else table.join2(chisel_opts, {"linknan.generator.SocGenerator"}) end
    if not option.get("all_in_one") or option.get("release") then table.join2(chisel_opts, {"--split-verilog"}) end
    if option.get("block_test_l2l3") then table.join2(chisel_opts, {"--no-core"}) end
    if option.get("keep_l1") then table.join2(chisel_opts, {"--keep-l1c"}) end
    if option.get("mbist") then table.join2(chisel_opts, {"--enable-mbist"}) end
    if option.get("legacy") then table.join2(chisel_opts, {"--legacy"}) end
    if not option.get("clean_difftest") and option.get("pldm_verilog") then table.join2(chisel_opts, {"--basic-difftest"}) end
    if not option.get("clean_difftest") and not option.get("pldm_verilog") then table.join2(chisel_opts, {"--enable-difftest"}) end
    if not option.get("enable_perf") then table.join2(chisel_opts, {"--fpga-platform"}) end
    if option.get("lua_scoreboard") then table.join2(chisel_opts, {"--lua-scoreboard"}) end
    if option.get("hardware_assertion") then table.join2(chisel_opts, {"--enable-hardware-assertion"}) end
    if option.get("sim") and option.get("dramsim3") then table.join2(chisel_opts, {"--dramsim3"}) end
    if option.get("prefix") ~= "" then table.join2(chisel_opts, {"--prefix", option.get("prefix")}) end
    local build_dir = path.join("build", "rtl")
    if not option.get("sim") and not option.get("release") then build_dir = option.get("out_dir") end
    if option.get("sim") then os.setenv("NOOP_HOME", os.curdir()) end
    table.join2(chisel_opts, {"--core", option.get("core")})
    table.join2(chisel_opts, {"--l3", option.get("l3")})
    table.join2(chisel_opts, {"--noc", option.get("noc")})
    table.join2(chisel_opts, {"--socket", option.get("socket")})
    table.join2(chisel_opts, {"--throw-on-first-error", "--target", "systemverilog", "--full-stacktrace", "-td", build_dir})
    if os.host() == "windows" then
      os.execv(os.shell(), table.join({"mill"}, chisel_opts))
    else
      os.execv("mill", chisel_opts)
    end

    os.rm(path.join(build_dir, "firrtl_black_box_resource_files.f"))
    os.rm(path.join(build_dir, "filelist.f"))
    os.rm(path.join(build_dir, "extern_modules.sv"))

    local py_exec = "python3"
    if os.host() == "windows" then py_exec = "python" end
    local pcmp_scr_path = path.join("scripts", "linknan", "postcompile.py")
    local postcompile_opts = {pcmp_scr_path, build_dir, "-j", option.get("jobs")}
    if option.get("vcs") then table.join2(postcompile_opts, {"--vcs"}) end
    os.execv(py_exec, postcompile_opts)

    local harden_table = {"LNTop", "NanhuCoreWrapper"}
    local rel_opts = {}
    if option.get("prefix") ~= "" then table.join2(rel_opts, {"-x", option.get("prefix")}) end
    if option.get("sim") then table.join2(rel_opts, {"-s", "SimTop"}) end
    local rel_scr_path = { path.join("scripts", "linknan", "release.py") }
    table.join2(rel_opts, harden_table)
    if option.get("release") then os.execv(py_exec, table.join2(rel_scr_path, rel_opts)) end
  end)
end)

task("emu", function()
  set_menu {
    usage = "xmake emu [options]",
    description = "Compile with verilator",
    options = {
      {'b', "rebuild", "k", nil, "forcely rebuild"},
      {'s', "sparse_mem", "k", nil, "use sparse mem"},
      {'d', "dramsim3", "k", nil, "use dramsim3"},
      {'p', "no_perf", "k", nil, "disable perf counter"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {'f', "fast", "k", nil, "disable trace to improve simulation speed"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'h', "dramsim3_home", "kv", path.join(os.curdir(), "dependencies", "dramsim"), "dramsim3 home dir"},
      {'t', "threads", "kv", "16", "simulation threads"},
      {'j', "jobs", "kv", "16", "compilation jobs"},
      {'r', "ref", "kv", "Nemu", "reference model"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "small", "define L3 config"},
      {'N', "noc", "kv", "small", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"}
    }
  }

  on_run(function()
    import("core.base.option")
    local num_cores = "1"
    if option.get("noc") == "full" then num_cores = 4 end
    if option.get("noc") == "reduced" then num_cores = 2 end
    import("scripts.xmake.verilator").emu_comp(num_cores)
  end)
end)

task("emu-run", function ()
  set_menu {
    usage = "xmake emu_run [options]",
    description = "Run emu",
    options = {
      {'d', "dump", "k", nil, "dump full wave and disable fork"},
      {'i', "image", "kv", nil, "bin image bin name"},
      {'z', "imagez", "kv", nil, "gz image name"},
      {'c', "cycles", "kv", "0", "simlation max cycles"},
      {'X', "fork", "kv", "50", "lightSSS fork interval in seconds, ignored when --dump is assigned"},
      {'b', "begin", "kv", "0", "begin time of waveform, ignored when --dump is not assigned"},
      {'e', "end", "kv", "0", "end time of waveform, ignored when --dump is not assigned"},
      {'g', "gcpt_restore", "kv", "", "overwrite gcptrestore img with this image file"},
      {'W', "warmup", "kv", "0", "warm up instruction count"},
      {'I', "instr", "kv", "0", "max instruction count"},
      {nil, "case_dir", "kv", "ready-to-run", "image base dir"},
      {'r', "ref", "kv", "riscv64-nemu-interpreter-so", "reference model"},
      {nil, "ref_dir", "kv", "ready-to-run", "reference model base dir"},
      {'s', "seed", "kv", "1234", "random seed"},
    }
  }

  on_run(function()
    import("scripts.xmake.verilator").emu_run()
  end)
end)

task("simv", function()
  set_menu {
    usage = "xmake simv [options]",
    description = "Compile with vcs",
    options = {
      {'b', "rebuild", "k", nil, "forcely rebuild"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {'d', "no_fsdb", "k", nil, "do not dump wave"},
      {'x', "no_xprop", "k", nil, "do not set xprop"},
      {'s', "sparse_mem", "k", nil, "use sparse mem"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'r', "ref", "kv", "Nemu", "reference model"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "small", "define L3 config"},
      {'N', "noc", "kv", "small", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"}
    }
  }

  on_run(function()
    import("core.base.option")
    local num_cores = "1"
    if option.get("noc") == "full" then num_cores = 4 end
    if option.get("noc") == "reduced" then num_cores = 2 end
    import("scripts.xmake.vcs").simv_comp(num_cores)
  end)
end)

task("simv-run", function ()
  set_menu {
    usage = "xmake simv-run [options]",
    description = "Run simv",
    options = {
      {nil, "no_dump", "k", nil, "do not dump waveform"},
      {'I', "init_reg", "kv", nil, "init reg by 0/1/random"},
      {'i', "image", "kv", nil, "bin image bin name"},
      {'z', "imagez", "kv", nil, "gz image name"},
      {'c', "cycles", "kv", "0", "gz image name"},
      {nil, "case_dir", "kv", "ready-to-run", "image base dir"},
      {'r', "ref", "kv", "riscv64-nemu-interpreter-so", "reference model"},
      {nil, "ref_dir", "kv", "ready-to-run", "reference model base dir"}
    }
  }

  on_run(function()
    import("scripts.xmake.vcs").simv_run()
  end)
end)

task("verdi", function ()
  set_menu {
    usage = "xmake verdi [options]",
    description = "Display waveform with verdi",
    options = {
      {'e', "verilator", "k", nil, "display emu .vcd waveform"},
      {'i', "image", "kv", nil, "image name"},
    }
  }

  on_run(function ()
    import("scripts.xmake.verdi").verdi()
  end)
end)

task("idea", function()
  on_run(function()
    if os.host() == "windows" then
      os.execv(os.shell(), {"mill", "-i", "mill.idea.GenIdea/idea"})
    else
      os.execv("mill", {"-i", "mill.idea.GenIdea/idea"})
    end
  end)
  set_menu {}
end)

task("init", function()
  on_run(function()
    os.exec("git submodule update --init")
    os.cd(path.join("dependencies", "nanhu"))
    os.exec("git submodule update --init YunSuan")
  end)
  set_menu {}
end)

task("comp", function()
  on_run(function()
    if os.host() == "windows" then
      os.execv(os.shell(), {"mill", "-i", "linknan.compile"})
      os.execv(os.shell(), {"mill", "-i", "linknan.test.compile"})
    else
      os.execv("mill", {"-i", "linknan.compile"})
      os.execv("mill", {"-i", "linknan.test.compile"})
    end
  end)
  set_menu {}
end)

task("clean", function()
  on_run(function()
    os.rm(path.join("out", "*.dep"))
    os.rmdir(path.join("build", "*"))
    os.rmdir(path.join("sim", "*"))
  end)
  set_menu {}
end)