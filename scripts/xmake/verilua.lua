---@diagnostic disable

function setup_env(sim)
  import("core.base.option")
  assert(sim == "vcs" or sim == "verilator" or sim == "pldm", "[verilua.lua] [setup_env] invalid sim type: " .. sim)

  local verilua_dir = path.join(os.projectdir(), "scripts", "verilua")
  os.setenv("VERILUA_CFG", path.join(verilua_dir, "cfg.lua"))
  os.setenv("LUA_SCRIPT", path.join(verilua_dir, "main.lua"))

  if sim == "pldm" then
    -- Reuse vcs configurations in verilua
    sim = "vcs"
  end
  os.setenv("SIM", sim)
  os.setenv("PRJ_TOP", os.projectdir())
  os.setenv("SOC_CFG_FILE", path.join(os.projectdir(), "build", "generated-src", "soc.lua"))

  local new_build_dir = option.get("build_dir") or os.getenv("BUILD_DIR")
  if new_build_dir then
    os.setenv("SOC_CFG_FILE", path.absolute(path.join(new_build_dir, "generated-src", "soc.lua")))
  end

  if option.get("no_scb") then
    os.setenv("LUA_SCB_DISABLE", 1)
  end
end
