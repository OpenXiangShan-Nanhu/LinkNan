---@diagnostic disable

function dramsim(home, build)
  local dramsim_build = path.join(os.curdir(), "sim", "dramsim")
  if not os.exists(dramsim_build) then os.mkdir(dramsim_build) end
  os.cd(dramsim_build)
  os.execv("cmake", {home, "-D", "COSIM=1", "-DCMAKE_C_COMPILER=clang",
    "-DCMAKE_CXX_COMPILER=clang++", "-DCMAKE_LINKER=clang++"
  })
  os.execv("make", {"dramsim3", "-j", "8"})
  os.cd("-")

  local src_cfg = path.join(home, "configs", "XiangShan-nanhu.ini")
  local tgt_cfg = path.join(dramsim_build, "XiangShan.ini")
  local cfg_lines = io.readfile(src_cfg)
  cfg_lines = cfg_lines .. "cpu_freq = 2000\n"
  cfg_lines = cfg_lines .. "dram_freq = 1600\n"
  io.writefile(tgt_cfg, cfg_lines)

  local cxx_flags = " -DWITH_DRAMSIM3"
  cxx_flags = cxx_flags .. " -I" .. path.join(home, "src")
  cxx_flags = cxx_flags .. " -DDRAMSIM3_CONFIG=\\\\\\\"" .. tgt_cfg .. "\\\\\\\""
  cxx_flags = cxx_flags .. " -DDRAMSIM3_OUTDIR=\\\\\\\"" .. build .. "\\\\\\\""
  local ld_flags = " " .. path.join(dramsim_build, "libdramsim3.a")
  return cxx_flags, ld_flags
end