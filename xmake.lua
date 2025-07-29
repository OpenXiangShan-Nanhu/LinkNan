---@diagnostic disable

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
      {'f', "fpga", "k", nil, "generate fpga top"},
      {'m', "mbist", "k", nil, "enable mbist"},
      {'p', "pldm_verilog", "k", nil, "enable only basic difftest function"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'z', "with_tfb", "k", nil, "enable traffic board of ring bus"},
      {'x', "prefix", "kv", "", "assign a prefix for rtl modules"},
      {'J', "jar", "kv", "", "use jar to generate artifacts"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "full", "define L3 config"},
      {'N', "noc", "kv", "full", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {'j', "jobs", "kv", "16", "post-compile process jobs"}
    }
  }
  local chisel_opts = {}

  on_run(function()
    import("core.base.option")

    local exec = "mill"
    if option.get("jar") ~= "" then
      exec = "java"
      table.join2(chisel_opts, {"-jar", option.get("jar")})
    elseif option.get("sim") then
      table.join2(chisel_opts, {"-i", "linknan.test.runMain", "lntest.top.SimGenerator"})
    elseif option.get("fpga") then
      table.join2(chisel_opts, {"-i", "linknan.test.runMain", "lntest.top.FpgaGenerator"})
    else
      table.join2(chisel_opts, {"-i", "linknan.runMain", "linknan.generator.SocGenerator"})
    end
    if not option.get("all_in_one") or option.get("release") then table.join2(chisel_opts, {"--split-verilog"}) end
    if option.get("block_test_l2l3") then table.join2(chisel_opts, {"--no-core"}) end
    if option.get("keep_l1") then table.join2(chisel_opts, {"--keep-l1c"}) end
    if option.get("mbist") then table.join2(chisel_opts, {"--enable-mbist"}) end
    if option.get("legacy") then table.join2(chisel_opts, {"--legacy"}) end
    if not option.get("with_tfb") then table.join2(chisel_opts, {"--no-tfb"}) end
    if not option.get("clean_difftest") and option.get("pldm_verilog") then table.join2(chisel_opts, {"--basic-difftest"}) end
    if not option.get("clean_difftest") and option.get("pldm_verilog") then table.join2(chisel_opts, {"--difftest-config", "EBINH"}) end
    if not option.get("clean_difftest") and not option.get("pldm_verilog") then table.join2(chisel_opts, {"--enable-difftest"}) end
    if not option.get("enable_perf") or option.get("release") then table.join2(chisel_opts, {"--fpga-platform"}) end
    if option.get("lua_scoreboard") then table.join2(chisel_opts, {"--lua-scoreboard"}) end
    if option.get("hardware_assertion") then table.join2(chisel_opts, {"--enable-hardware-assertion"}) end
    if option.get("sim") and option.get("dramsim3") then table.join2(chisel_opts, {"--dramsim3"}) end
    if option.get("prefix") ~= "" then table.join2(chisel_opts, {"--prefix", option.get("prefix")}) end
    os.setenv("NOOP_HOME", os.curdir())

    local build_dir = path.join("build")
    local rtl_dir = path.join(build_dir, "rtl")

    table.join2(chisel_opts, {"--core", option.get("core")})
    table.join2(chisel_opts, {"--l3", option.get("l3")})
    table.join2(chisel_opts, {"--noc", option.get("noc")})
    table.join2(chisel_opts, {"--socket", option.get("socket")})
    table.join2(chisel_opts, {"--throw-on-first-error", "--target", "systemverilog", "--full-stacktrace", "-td", rtl_dir})
    if os.host() == "windows" then
      os.execv(os.shell(), table.join({exec}, chisel_opts))
    else
      os.execv(exec, chisel_opts)
    end

    os.rm(path.join(rtl_dir, "firrtl_black_box_resource_files.f"))
    os.rm(path.join(rtl_dir, "filelist.f"))
    os.rm(path.join(rtl_dir, "extern_modules.sv"))

    local py_exec = "python3"
    if os.host() == "windows" then py_exec = "python" end
    local pcmp_scr_path = path.join("scripts", "linknan", "postcompile.py")
    local postcompile_opts = {pcmp_scr_path, rtl_dir, "-j", option.get("jobs")}
    if option.get("vcs") then table.join2(postcompile_opts, {"--vcs"}) end
    os.execv(py_exec, postcompile_opts)

    local harden_table = {"LNTop", "NanhuCoreWrapper"}
    local rel_opts = {}
    if option.get("fpga") then harden_table = {"XlnFpgaTop", "CpuCluster", "UncoreTop"} end
    if option.get("prefix") ~= "" then table.join2(rel_opts, {"-x", option.get("prefix")}) end
    if option.get("sim") then table.join2(rel_opts, {"-s", "SimTop"}) end
    local rel_scr_path = { path.join("scripts", "linknan", "release.py") }
    table.join2(rel_opts, harden_table)
    if option.get("release") then os.execv(py_exec, table.join2(rel_scr_path, rel_opts)) end

    -- new_build_dir can be set by `--build_dir=<xxx>` or by setting environment variable `BUILD_DIR`
    -- To make this easier, you can set environment variable `BUILD_DIR` using `export BUILD_DIR=<xxx>` and 
    -- the subsequent `xmake` command will use the environment variable.
    local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
    if not option.get("release") and new_build_dir and path.absolute(build_dir) ~= path.absolute(new_build_dir) then
      assert(new_build_dir ~= "", "build_dir(`%s`) is not a valid value!", new_build_dir)

      -- Rename build dir to user assigned dir
      os.mv("build", new_build_dir)
    end
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
      {'G', "bypass_clockgate", "k", nil, "force enable all clock gates"},
      {'J', "jar", "kv", "", "use jar to generate artifacts"},
      {'h', "dramsim3_home", "kv", path.join(os.curdir(), "dependencies", "dramsim"), "dramsim3 home dir"},
      {'t', "threads", "kv", "16", "simulation threads"},
      {'j', "jobs", "kv", "16", "compilation jobs"},
      {'r', "ref", "kv", "Nemu", "reference model"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "small", "define L3 config"},
      {'N', "noc", "kv", "small", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
      {nil, "dump_fst", "k", nil, "set wave type xxx.fst"},
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
      {'f', "flash", "kv", nil, "flash image bin name"},
      {'z', "imagez", "kv", nil, "gz image name"},
      {'w', "workload", "kv", nil, "workload name(.bin/.gz)(fullpath)"},
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
      {nil, "case_name", "kv", nil, "user defined case name"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
      {nil, "jtag_debug", "kv", nil, "set --remote-jtag-port={value} && --enable-jtag"},
      {nil, "dump_wave_full", "k", nil, "set --dump-wave-full"},
      {nil, "dump_fst", "k", nil, "set wave type xxx.fst"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {nil, "no_scb", "k", nil, "disable lua scoreboard"},
    }
  }

  on_run(function()
    import("core.base.option")

    -- Set verilua env
    os.setenv("VERILUA_CFG", path.join(os.scriptdir(), "scripts", "verilua", "cfg.lua"))
    os.setenv("LUA_SCRIPT", path.join(os.scriptdir(), "scripts", "verilua", "main.lua"))
    os.setenv("SIM", "verilator")
    os.setenv("PRJ_TOP", os.scriptdir())
    os.setenv("SOC_CFG_FILE", path.join(os.scriptdir(), "build", "generated-src", "soc.lua"))
    local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
    if new_build_dir then
      os.setenv("SOC_CFG_FILE", path.absolute(path.join(new_build_dir, "generated-src", "soc.lua")))
    end
    if option.get("no_scb") then
      os.setenv("LUA_SCB_DISABLE", 1)
    end

    import("scripts.xmake.verilator").emu_run()
  end)
end)

task("simv", function()
  set_menu {
    usage = "xmake simv [options]",
    description = "Compile with vcs",
    options = {
      {'b', "rebuild", "k", nil, "forcely rebuild"},
      {'B', "rebuild_comp", "k", nil, "forcely rebuild vcs simv"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {'d', "no_fsdb", "k", nil, "do not dump wave"},
      {'x', "no_xprop", "k", nil, "do not set xprop"},
      {'s', "sparse_mem", "k", nil, "use sparse mem"},
      {'G', "bypass_clockgate", "k", nil, "force enable all clock gates"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'J', "jar", "kv", "", "use jar to generate artifacts"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'r', "ref", "kv", "Nemu", "reference model"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "small", "define L3 config"},
      {'N', "noc", "kv", "small", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
      {nil, "cov", "k", nil, "enable coverage collection"},
      {'E', "extra_filelist", "kv", nil, "extra filelist"},
      {nil, "no_fgp", "k", nil, "disable fgp multithread"},
      {nil, "vcs_args", "kv", nil, "additional arguments for vcs"},
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
      {'f', "flash", "kv", nil, "flash image bin name"},
      {'z', "imagez", "kv", nil, "gz image name"},
      {'w', "workload", "kv", nil, "workload name(.bin/.gz)(fullpath)"},
      {'b', "bootrom", "kv", nil, "bootrom-file/flash-file name(fullpath)"},
      {'c', "cycles", "kv", "0", "max simulation cycles"},
      {nil, "case_dir", "kv", "ready-to-run", "image base dir"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {'r', "ref", "kv", "riscv64-nemu-interpreter-so", "reference model"},
      {nil, "ref_dir", "kv", "ready-to-run", "reference model base dir"},
      {nil, "case_name", "kv", nil, "user defined case name"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
      {nil, "cov", "k", nil, "enable coverage collection"},
      {nil, "no_fgp", "k", nil, "disable fgp multithread"},
      {nil, "fgp_threads", "kv", "4", "fgp threads"},
      {nil, "simv_args", "kv", nil, "additional arguments for simv"},
      {nil, "no_scb", "k", nil, "disable lua scoreboard"},
    }
  }

  on_run(function()
    import("core.base.option")

    -- Set verilua env
    os.setenv("VERILUA_CFG", path.join(os.scriptdir(), "scripts", "verilua", "cfg.lua"))
    os.setenv("LUA_SCRIPT", path.join(os.scriptdir(), "scripts", "verilua", "main.lua"))
    os.setenv("SIM", "vcs")
    os.setenv("PRJ_TOP", os.scriptdir())
    os.setenv("SOC_CFG_FILE", path.join(os.scriptdir(), "build", "generated-src", "soc.lua"))
    local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
    if new_build_dir then
      os.setenv("SOC_CFG_FILE", path.absolute(path.join(new_build_dir, "generated-src", "soc.lua")))
    end
    if option.get("no_scb") then
      os.setenv("LUA_SCB_DISABLE", 1)
    end

    import("scripts.xmake.vcs").simv_run()
  end)
end)

task("pldm", function()
  set_menu {
    usage = "xmake pldm [options]",
    description = "Compile with pldm(Cadence Palladium)",
    options = {
      {'b', "rebuild", "k", nil, "forcely rebuild"},
      {'n', "no_diff", "k", nil, "disable difftest"},
      {'d', "no_fsdb", "k", nil, "do not dump wave"},
      {nil, "synthesis", "k", nil, "synthesis compilation mode"},
      -- {'s', "sparse_mem", "k", nil, "use sparse mem"},
      {'G', "bypass_clockgate", "k", nil, "force enable all clock gates"},
      {'l', "lua_scoreboard", "k", nil, "use lua scoreboard for cache debug"},
      {'J', "jar", "kv", "", "use jar to generate artifacts"},
      {'Y', "legacy", "k", nil, "use XS legacy memory map"},
      {'r', "ref", "kv", "Nemu", "reference model"},
      {'C', "core", "kv", "full", "define cpu core config in soc"},
      {'L', "l3", "kv", "small", "define L3 config"},
      {'N', "noc", "kv", "small", "define noc config"},
      {'S', "socket", "kv", "sync", "define how cpu cluster connect to noc"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
    }
  }

  on_run(function()
    import("core.base.option")
    local num_cores = "1"
    if option.get("noc") == "full" then num_cores = 4 end
    if option.get("noc") == "reduced" then num_cores = 2 end
    import("scripts.xmake.pldm").pldm_comp(num_cores)
  end)
end)

task("pldm-run", function ()
  set_menu {
    usage = "xmake pldm-run [options]",
    description = "Run pldm(Cadence Palladium)",
    options = {
      {'i', "image", "kv", nil, "bin image bin name"},
      {'f', "flash", "kv", nil, "flash image bin name"},
      {'z', "imagez", "kv", nil, "gz image name"},
      {'c', "cycles", "kv", "0", "max simulation cycles"},
      {'w', "workload", "kv", nil, "workload name(.bin/.gz)(fullpath)"},
      {'r', "ref", "kv", "riscv64-nemu-interpreter-so", "reference model"},
      {nil, "ref_dir", "kv", "ready-to-run", "reference model base dir"},
      {nil, "case_dir", "kv", "ready-to-run", "image base dir"},
      {nil, "case_name", "kv", nil, "user defined case name"},
      {'o', "build_dir", "kv", nil, "assign build dir"},
      {nil, "sim_dir", "kv", nil, "assign simulation dir"},
      {nil, "no_scb", "k", nil, "disable lua scoreboard"},
    }
  }

  on_run(function()
    import("core.base.option")

    -- Set verilua env
    os.setenv("VERILUA_CFG", path.join(os.scriptdir(), "scripts", "verilua", "cfg.lua"))
    os.setenv("LUA_SCRIPT", path.join(os.scriptdir(), "scripts", "verilua", "main.lua"))
    os.setenv("SIM", "vcs")
    os.setenv("PRJ_TOP", os.scriptdir())
    os.setenv("SOC_CFG_FILE", path.join(os.scriptdir(), "build", "generated-src", "soc.lua"))
    local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
    if new_build_dir then
      os.setenv("SOC_CFG_FILE", path.absolute(path.join(new_build_dir, "generated-src", "soc.lua")))
    end
    if option.get("no_scb") then
      os.setenv("LUA_SCB_DISABLE", 1)
    end

    import("scripts.xmake.pldm").pldm_run()
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

task("view_db", function ()
  -- Example:
  --    xmake view_db --addr="%deadbeef%" --db_file="test.db,test2.db,1__*.db"
  --    xmake view_db -a "deadbeef" -d "test.db"
  --    xmake view_db -a "%abab%" -d test.db
  set_menu {
    usage = "xmake view_db [options]",
    description = "View database generated by lua scb",
    options = {
      {'a', "addr", "kv", nil, "address(or address espression) to be viewed(e.g. --addr=deadbeef, --addr=\"%abab%\")"},
      {'d', "db_file", "kv", nil, "input database files, support wildcard and comma espression(e.g. --db_file=test.db,test2.db,1__*.db"},
    }
  }

  on_run(function ()
    import("core.base.option")

    local db_files = {}
    local addr = option.get("addr")
    local _db_files = option.get("db_file")
    assert(addr, "[view_db] addr is required, use `--addr` to specify it")
    assert(db_files, "[view_db] db_file is required, use `--db_file` to specify it")

    _db_files = _db_files:split(",", {plain = true})

    local should_remove_idx = {}
    for i, db_file in ipairs(_db_files) do
      local files = os.files(db_file)
      if #files > 0 then
        table.join2(db_files, files)
        table.insert(should_remove_idx, i)
      end
    end

    for i, db_file in ipairs(_db_files) do
      if not table.contains(should_remove_idx, i) then
        table.insert(db_files, db_file)
      end
    end

    db_files = table.unique(db_files)

    for _, db_file in ipairs(db_files) do
      assert(os.isfile(db_file), "[view_db] " .. db_file .. " is not a file")
    end

    print("[view_db] addr:", addr)
    print("[view_db] all files:", db_files)

    for _, db_file in ipairs(db_files) do
      local tables = os.iorun([[sqlite3 "%s" "SELECT name FROM sqlite_master WHERE type='table';"]], db_file)
      for _, t in ipairs(tables:split("\n")) do
        local result = os.iorun([[sqlite3 -header "%s" "SELECT * FROM '%s' WHERE address like '%s';"]], db_file, t, addr)
        if result and result ~= "" then
          print("----------------------------------------------------------")
          print("| file: <%s>, table: <%s>", db_file, t)
          print("----------------------------------------------------------")
          print(result:gsub("|", "  |  "):gsub("/", "--"))
          print("----------------------------------------------------------")
          print()
        end
      end
    end
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
    os.cd(os.scriptdir())
    os.exec("git submodule update --init")
    os.cd(path.join(os.scriptdir(), "dependencies", "nanhu"))
    os.exec("git submodule update --init YunSuan")
  end)
  set_menu {}
end)

task("comp", function()
  on_run(function()
    local abs_base = os.curdir()
    local ln_out_dir = path.join(abs_base, "out", "linknan")
    local jar_path = path.join(ln_out_dir, "assembly.dest", "out.jar")
    local test_jar_path = path.join(ln_out_dir, "test", "assembly.dest", "out.jar")
    local build_dir = path.join(abs_base, "build")
    local asmb_json = path.join(ln_out_dir, "assembly.json")
    local test_asmb_json = path.join(ln_out_dir, "test", "assembly.json")
    if os.exists(asmb_json) then os.rm(asmb_json) end
    if os.exists(test_asmb_json) then os.rm(test_asmb_json) end
    if os.host() == "windows" then
      os.execv(os.shell(), {"mill", "-i", "linknan.assembly"})
      os.execv(os.shell(), {"mill", "-i", "linknan.test.assembly"})
    else
      os.execv("mill", {"-i", "linknan.assembly"})
      os.execv("mill", {"-i", "linknan.test.assembly"})
    end
    os.mv(jar_path, path.join(build_dir, "linknan.jar"))
    os.mv(test_jar_path, path.join(build_dir, "linknan.test.jar"))
  end)
  set_menu {}
end)

task("clean", function()
  on_run(function()
    os.rm(path.join("out", "*.dep*"))
    os.rmdir(path.join("build", "*"))
    os.rmdir(path.join("sim", "*"))
  end)
  set_menu {}
end)
