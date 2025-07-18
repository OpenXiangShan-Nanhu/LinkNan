---@diagnostic disable

import("core.base.option")
import("core.project.depend")
import("core.base.task")

-- TODO: dramsim3
-- TODO: Seperate difftest as a sub xmake.lua

local debug = false
local check_host_name = true
local pldm_z2_host_prefix = "node016"

if os.getenv("NO_CHECK_HOST") then
  check_host_name = false
end

local tb_top = "tb_top"
local dpi_so_name = "libdpi_emu.so"

local function log(name, ...)
  cprintf("${blue}[pldm.lua] " .. name .. "${clear} ")
  print(...)
end

local function if_debug(debug_stmt, normal_stmt)
  local stmt
  if debug then
    stmt = debug_stmt
  else
    stmt = normal_stmt
  end

  local t = type(stmt)
  if t == "function" then
    local ret = stmt()
    assert(ret, "[pldm.lua] [if_debug] function stmt returns nil")
    return ret
  else
    return stmt
  end
end

local function remove_trailing_newline(s)
  return s:gsub("[\r\n]", "")
end

local function io_run(cmd)
  return remove_trailing_newline(os.iorun(cmd))
end

local function load_pldm_z2env()
  -- TODO: load_pldm_z1env

  local host_name = os.getenv("HOSTNAME") or "<Undefined>"
  if check_host_name and not host_name:startswith(pldm_z2_host_prefix) then
    raise(format(
      "[pldm.lua] [load_pldm_z2env] you are not in the correct host, expect host name prefix: `%s`, but got host name: `%s`", 
      pldm_z2_host_prefix,
      host_name
    ))
  end

  local envs = {}
  local function set_env(key, value)
    os.setenv(key, value)
    envs[key] = value
  end
  local function add_env(key, value)
    local new_value = value .. ":" .. assert(os.getenv(key), format("[pldm.lua] [load_pldm_z2env.add_env] key: %s is nil", key))
    os.setenv(key, new_value)
    envs[key] = new_value
  end

  set_env("BITMODE", "64")
  set_env("CDS_INST_DIR", "ALL")

  -- xcelium setup
  set_env("XLM_HOME", "/nfs/tools/Cadence/XCELIUM24.03.005")
  set_env("CDS_INST_DIR", envs.XLM_HOME)
  add_env("PATH", envs.XLM_HOME .. "/bin")
  add_env("PATH", envs.XLM_HOME .. "/tools.lnx86/bin")
  add_env("PATH", envs.XLM_HOME .. "/tools/cdsgcc/gcc/bin")
  add_env("LD_LIBRARY_PATH", envs.XLM_HOME .. "/tools.lnx86/lib/64bit")
  add_env("LD_LIBRARY_PATH", envs.XLM_HOME .. "/tools.lnx86/inca/lib/64bit")
  
  -- HDLICE setup
  set_env("HDLICE_HOME", "/nfs/tools/Cadence/IXCOM24.05.s001")
  add_env("PATH", envs.HDLICE_HOME .. "/bin")

  -- IXCOM setup
  set_env("VXE_HOME", "/nfs/tools/Cadence/WXE24.05.s001")
  set_env("WXE_HOME", "/nfs/tools/Cadence/WXE24.05.s001")
  add_env("PATH", envs.VXE_HOME .. "/bin")
  add_env("LD_LIBRARY_PATH", envs.VXE_HOME .. "/tools.lnx86/lib/64bit")

  -- license setup
  set_env("LM_LICENSE_FILE", "5280@node016")

  set_env("VERDI_HOME", "/nfs/tools/synopsys/verdi/R-2020.12-SP1")
  add_env("PATH", envs.VERDI_HOME .. "/bin")
  add_env("LD_LIBRARY_PATH", envs.VERDI_HOME .. "/share/PLI/lib/linux64")
  add_env("LD_LIBRARY_PATH", envs.VERDI_HOME .. "/share/VCS/lib/linux64")
  add_env("LD_LIBRARY_PATH", envs.VERDI_HOME .. "/share/IUS/lib/linux64")
  
  set_env("VCS_HOME", "/nfs/tools/synopsys/vcs/Q-2020.03-SP2")
  add_env("PATH", envs.VCS_HOME .. "/bin")

  set_env("PLDM_HOST", "bjos_emu2")

  log("[load_pldm_z2env]", "finish")
end 

function pldm_comp(num_cores)
  assert(num_cores, "[pldm.lua] [pldm_comp] num_cores is required")
  load_pldm_z2env()

  local abs_dir = os.curdir()
  local ixcom_dir = path.join(io_run("cds_root ixcom"), "share", "uxe", "etc", "ixcom")
  local simtool_dir = path.join(io_run("cds_root xrun"), "tools", "include")
  local pldm_scripts_dir = path.join(abs_dir, "scripts", "pldm")
  local chisel_dep_srcs = os.filedirs(path.join(abs_dir, "src", "**.scala"))
  table.join2(chisel_dep_srcs, os.filedirs(path.join(abs_dir, "dependencies", "**.scala")))
  table.join2(chisel_dep_srcs, {path.join(abs_dir, "build.sc")})
  table.join2(chisel_dep_srcs, {path.join(abs_dir, "xmake.lua")})
  if option.get("jar") ~= "" then chisel_dep_srcs = option.get("jar") end

  local build_dir = path.join(abs_dir, "build")
  local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
  if new_build_dir then build_dir = path.absolute(new_build_dir) end

  local sim_dir = path.join(abs_dir, "sim")
  local new_sim_dir = option.get("sim_dir") or os.getenv("SIM_DIR")
  if new_sim_dir then sim_dir = path.absolute(new_sim_dir) end
  
  local comp_dir = path.join(sim_dir, "pldm", "comp")
  if not os.exists(comp_dir) then os.mkdir(comp_dir) end
  local design_gen_dir = path.join(build_dir, "generated-src")
  local dpi_export_dir = path.join(comp_dir, "dpi_export")
  local difftest_vsrc = path.join(abs_dir, "dependencies", "difftest", "src", "test", "vsrc")
  local difftest_csrc = path.join(abs_dir, "dependencies", "difftest", "src", "test", "csrc")

  local vsrc = {}
  local vsrc_dirs = if_debug({
    path.join(abs_dir, "scripts", "pldm", "debug_rtl")
  }, {
    path.join(build_dir, "rtl"),
    path.join(difftest_vsrc, "common"),
    path.join(difftest_vsrc, "vcs")
  })
  for _, p in ipairs(vsrc_dirs) do
    table.join2(vsrc, os.files(path.join(p, "*v")))
  end

  local csrc = {}
  local cinc_dirs = if_debug({}, {
    design_gen_dir,
    path.join(difftest_csrc, "common"),
    path.join(difftest_csrc, "difftest"),
    path.join(difftest_csrc, "plugin", "spikedasm"),
    path.join(abs_dir, "dependencies", "difftest", "config")
  })
  local csrc_dirs = if_debug({}, {
    design_gen_dir,
    path.join(difftest_csrc, "vcs"),
    path.join(difftest_csrc, "common"),
    path.join(difftest_csrc, "plugin", "spikedasm"),
  })
  for _, p in ipairs(csrc_dirs) do
    table.join2(csrc, os.files(path.join(p, "*.cpp")))
    table.join2(csrc, os.files(path.join(p, "*.c")))
  end

  if not option.get("no_diff") and not debug then
    local difftest_csrc_difftest = path.join(difftest_csrc, "difftest")
    table.join2(csrc, os.files(path.join(difftest_csrc_difftest, "*.cpp")))
  end

  depend.on_changed(function ()
    log("[pldm_comp]", "change detected! start compiling `soc`...")
    if os.exists(build_dir) then os.rmdir(build_dir) end
    task.run("soc", {
      vcs = true, sim = true, config = option.get("config"),
      pldm_verilog = true,
      socket = option.get("socket"), lua_scoreboard = option.get("lua_scoreboard"),
      core = option.get("core"), l3 = option.get("l3"), noc = option.get("noc"),
      legacy = option.get("legacy"), jar = option.get("jar"),
      build_dir = build_dir
    })

    vsrc = {}
    csrc = {}
    for _, p in ipairs(vsrc_dirs) do
      table.join2(vsrc, os.files(path.join(p, "*v")))
    end
    for _, p in ipairs(csrc_dirs) do
      table.join2(csrc, os.files(path.join(p, "*.cpp")))
      table.join2(csrc, os.files(path.join(p, "*.c")))
    end

    if option.get("lua_scoreboard") then
      local dpi_cfg_lua = path.join(abs_dir, "scripts", "verilua", "dpi_cfg.lua")
      if os.exists(dpi_export_dir) then os.rmdir(dpi_export_dir) end
      os.mkdir(dpi_export_dir)
      local dpi_exp_opts =  {"dpi_exporter"}
      table.join2(dpi_exp_opts, {"--config", dpi_cfg_lua})
      table.join2(dpi_exp_opts, {"--out-dir", dpi_export_dir})
      table.join2(dpi_exp_opts, {"--work-dir", dpi_export_dir})
      table.join2(dpi_exp_opts, {"-I", design_gen_dir})
      table.join2(dpi_exp_opts, {"--quiet", "--pldm-gfifo-dpi"})
      table.join2(dpi_exp_opts, {"--top", tb_top})
      table.join2(dpi_exp_opts, vsrc)
      local cmd_file = path.join(comp_dir, "dpi_exp_cmd.sh")
      io.writefile(cmd_file, table.concat(dpi_exp_opts, " "))
      os.execv(os.shell(), { cmd_file })
    end
  end,{
    files = chisel_dep_srcs,
    dependfile = path.join("out", "chisel.pldm.dep." .. (build_dir):gsub("/", "_"):gsub(" ", "_")),
    dryrun = option.get("rebuild"),
    values = table.join2({build_dir}, xmake.argv())
  })

  if option.get("lua_scoreboard") then
    vsrc = os.files(path.join(dpi_export_dir, "*v"))
  end
  assert(#vsrc > 0, "[pldm.lua] [pldm_comp] vsrc is empty")

  -- Generate filelist
  local vsrc_f = path.join(comp_dir, "vsrc.f")
  local csrc_f = path.join(comp_dir, "csrc.f")
  os.tryrm(vsrc_f)
  os.tryrm(csrc_f)
  io.writefile(vsrc_f, table.concat(vsrc, "\n"))
  io.writefile(csrc_f, table.concat(csrc, "\n"))

  local v_incdir_flags = ""
  for _, p in ipairs(table.join2(vsrc_dirs, { design_gen_dir })) do
    v_incdir_flags = v_incdir_flags .. " -incdir " .. p
  end

  local headers = {}
  local c_incdir_flags = ""
  for _, p in ipairs(cinc_dirs) do
    c_incdir_flags = c_incdir_flags .. " -I" .. p
    table.join2(headers, os.files(path.join(p, "*.h")))
  end

  local vlan_flags = {
    "-64", "-sv", "-vtimescale 1ns/1ns", v_incdir_flags,
    "-F " .. vsrc_f, "-l " .. path.join(comp_dir, "vlan.log")
  }
  local ixcom_flags = {
    "-clean", "-64", "-ua", "+sv", "+ignoreSimVerCheck", "+xe_alt_xlm",
    "-xeCompile compilerOptions=" .. path.join(pldm_scripts_dir, "compilerOptions.qel"),
    "+gfifoDisp+" .. tb_top,
    v_incdir_flags,
    "+dut+" .. tb_top,
    "-v " .. path.join(ixcom_dir, "IXCclkgen.sv"),
    "+tfconfig+" .. path.join(pldm_scripts_dir, "argConfigs.qel"),
    "-F " .. vsrc_f, "-l " .. path.join(comp_dir, "ixcom.log")
  }
  local macro_flags = {
    "+define+PALLADIUM", "+define+RANDOMIZE_MEM_INIT", "+define+RANDOMIZE_REG_INIT", "+define+RANDOMIZE_DELAY=0",
    "+define+SIM_TOP_MODULE_NAME=" .. tb_top .. ".sim"
  }

  if option.get("bypass_clockgate") then
    table.join2(macro_flags, {"+define+BYPASS_CLOCKGATE"})
  end

  if option.get("lua_scoreboard") then
    table.join2(macro_flags, {"+define+DUT_CLEAN"})
  end

  if option.get("synthesis") then
    table.join2(ixcom_flags, {"+1xua"})

    local pldm_clock = "clock_gen"
    local pldm_clock_def = path.join(pldm_scripts_dir, pldm_clock .. ".def")
    local pldm_clock_src = path.join(comp_dir, pldm_clock .. ".sv")

    -- Generate pldm clock source file
    os.execv("ixclkgen", {"-input " .. pldm_clock_def, "-output " .. pldm_clock_src, "-module " .. pldm_clock, "-hierarchy \"" .. tb_top .. ".\""})

    table.join2(ixcom_flags, {"+dut+" .. pldm_clock, pldm_clock_src})

    table.join2(macro_flags, {"+define+SYNTHESIS", "+define+TB_NO_DPIC"})
  else
    -- `+iscDelay+<module>[+<module>]`: enables blocking time delay transformations for DUT modules
    table.join2(ixcom_flags, {"+iscDelay+" .. tb_top, "-enableLargeSizeMem"})
    table.join2(ixcom_flags, {"+rtlCommentPragma", "+tran_relax", "-relativeIXCDIR"})

    -- TODO: Optional?
    table.join2(macro_flags, {"+define+DIFFTEST", "+define+DISABLE_SIMJTAG_DPIC"})
    table.join2(macro_flags, {"+define+DISABLE_DIFFTEST_RAM_DPIC", "+define+DISABLE_DIFFTEST_FLASH_DPIC"})
    
    -- Build dpi lib
    do
      log("[pldm_comp]", "Build dpi lib")
      local cc = os.getenv("CC") or "gcc"

      local cc_build_dir = path.join(comp_dir, "dpi_build")
      if not os.exists(cc_build_dir) then os.mkdir(cc_build_dir) end
      os.cd(cc_build_dir)

      -- Generate objects
      do
        local pldm_cflags = {
          "-O3", "-m64", "-c", "-fPIC", "-g", "-std=c++17",
          "-I" .. ixcom_dir, "-I" .. simtool_dir,
          "-DNUM_CORES=" .. num_cores
        }

        if option.get("lua_scoreboard") then
          local verilua_home = assert(os.getenv("VERILUA_HOME"), "[pldm.lua] [pldm_comp] error: VERILUA_HOME is not set!")
          table.insert(pldm_cflags, "-DDPI_EXP_CALL_VERILUA_ENV_STEP")
          table.join2(csrc, os.files(path.join(dpi_export_dir, "*cpp")))
          table.insert(csrc, path.join(verilua_home, "src", "dummy_vpi", "dummy_vpi.cpp"))
        end

        for _, inc in ipairs(cinc_dirs) do
          table.insert(pldm_cflags, "-I" .. inc) 
        end

        if option.get("ref") == "Spike" then
          table.insert(pldm_cflags, "-DREF_PROXY=SpikeProxy")
        else
          table.insert(pldm_cflags, "-DREF_PROXY=NemuProxy")
        end

        table.join2(pldm_cflags, csrc)
        if #csrc > 0 then
          log("[pldm_comp]", "\tGenerate dpi objects(total csrc: %d)...", #csrc)
          os.exec(cc .. " " .. table.concat(pldm_cflags, " "))
        end
      end
      
      -- Generate shared library
      local has_so = false
      do
        local pldm_ldflags = {
          "-o " .. path.join(cc_build_dir, dpi_so_name), "-m64", "-shared"
        }
        local objs = os.files(path.join(cc_build_dir, "*.o"))
        
        if option.get("lua_scoreboard") then
          local verilua_home = assert(os.getenv("VERILUA_HOME"), "[pldm.lua] [pldm_comp] error: VERILUA_HOME is not set!")
          local verilua_shared = path.join(verilua_home, "shared")
          local luajit_home = path.join(verilua_home, "luajit-pro", "luajit2.1")
          local luajit_lib_home = path.join(luajit_home, "lib")

          table.join2(pldm_ldflags, {
            "-lverilua_vcs_dpi",
            "-L" .. verilua_shared,
            "-Wl,-rpath," .. verilua_shared,
            "-lluajit-5.1",
            "-L" .. luajit_lib_home,
            "-Wl,-rpath," .. luajit_lib_home
          })
        end

        if #objs > 0 then
          table.join2(pldm_ldflags, objs)
          log("[pldm_comp]", "\tGenerate dpi shared library(total obj: %d)...", #objs)
          os.exec(cc .. " " .. table.concat(pldm_ldflags, " "))
          has_so = true
        end
      end

      if has_so then
        os.cp(path.join(cc_build_dir, dpi_so_name), path.join(comp_dir, dpi_so_name))
      end
      os.cd(os.curdir())
    end
  end

  table.join2(ixcom_flags, macro_flags)

  local depend_srcs = vsrc
  table.join2(depend_srcs, csrc)
  table.join2(depend_srcs, headers)
  table.join2(depend_srcs, { path.join(abs_dir, "scripts", "xmake", "pldm.lua") })

  os.cd(comp_dir)
  depend.on_changed(function()
    os.exec("vlan " .. table.concat(vlan_flags, " "))
    os.exec("ixcom " .. table.concat(ixcom_flags, " "))
    log("[pldm_comp]", "pldm_comp success!")
  end, {
    files = depend_srcs,
    dependfile = path.join(comp_dir, "pldm.ln.dep." .. (sim_dir):gsub("/", "_"):gsub(" ", "_")),
    dryrun = option.get("rebuild"),
    values = table.join2({sim_dir}, xmake.argv())
  })
end

function pldm_run()
  if not debug then
    assert(
      option.get("image") or option.get("imagez") or option.get("workload"),
      "[pldm.lua] [pldm_run] must set one of `image(-i)`, `imagez(-z)` or `workload(-w)`"
    )
  end
  load_pldm_z2env()

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
    raise("[pldm.lua] [pldm_run] `image(-i)`, `imagez(-z)` and `workload(-w)` cannot be both set")
  end

  local case_name = path.basename(image_file)
  if option.get("case_name") ~= nil then case_name = option.get("case_name") end

  log("[pldm_run]", "image_file: " .. image_file)
  log("[pldm_run]", "flash_file: " .. flash_file)
  log("[pldm_run]", "case_name: " .. case_name)

  local sim_dir = path.join(abs_dir, "sim")
  local new_sim_dir = option.get("sim_dir") or os.getenv("SIM_DIR")
  if new_sim_dir then sim_dir = path.absolute(new_sim_dir) end
  
  local pldm_sim_dir = path.join(sim_dir, "pldm")
  local pldm_case_dir = path.join(pldm_sim_dir, case_name)
  local pldm_comp_dir = path.join(pldm_sim_dir, "comp")
  local pldm_scripts_dir = path.join(abs_dir, "scripts", "pldm")

  if not os.exists(pldm_comp_dir) then 
    raise(format(
      "[pldm.lua] [pldm_run] comp_dir(`%s`) does not exist, maybe you should run `xmake pldm <flags>` first", 
      pldm_comp_dir
    ))
  end
  if not os.exists(pldm_case_dir) then os.mkdir(pldm_case_dir) end

  os.cd(pldm_case_dir)
  os.setenv("PLDM_COMP_DIR", pldm_comp_dir)
  local xsim_pre_flags = {
    "--xmsim", "-64", "+xcprof", "-profile", "-PROFTHREAD", 
    if_debug("", "-sv_lib " .. path.join(pldm_comp_dir, dpi_so_name)),
    "-licqueue", -- waitting for license release
    "+diff=" .. path.join(abs_ref_base_dir, option.get("ref")),
    "+workload=" .. image_file,
    "+max-cycles=" .. option.get("cycles"),
  }
  if flash_file ~= "" then table.insert(xsim_pre_flags, "+flash=" .. flash_file) end

  local xsim_post_flags = {
    "--",
    "-input " .. path.join(pldm_scripts_dir, "run.tcl"),
    "-l " .. path.join(pldm_case_dir, format("run-%s-%s.log", case_name, io_run("date +%Y%m%d-%H%M%S")))
  }

  local xsim_flags = {}
  table.join2(xsim_flags, xsim_pre_flags)
  table.join2(xsim_flags, xsim_post_flags)

  os.exec("xeDebug " .. table.concat(xsim_flags, " "))
end
