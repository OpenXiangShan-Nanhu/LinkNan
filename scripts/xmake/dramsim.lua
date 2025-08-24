---@diagnostic disable
local new_cmakefile = [[
cmake_minimum_required(VERSION 3.0.0)
project(dramsim3)

set(default_build_type "Release")
if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
  message(STATUS "Setting build type to '${default_build_type}' as none was specified.")
  set(CMAKE_BUILD_TYPE "${default_build_type}" CACHE
      STRING "Choose the type of build." FORCE)
  # Set the possible values of build type for cmake-gui
  set_property(CACHE CMAKE_BUILD_TYPE PROPERTY STRINGS
    "Debug" "Release" "MinSizeRel" "RelWithDebInfo")
endif()

add_library(inih INTERFACE)
target_include_directories(inih INTERFACE ext/headers)

add_library(format INTERFACE)
target_include_directories(format INTERFACE ext/fmt/include)
target_compile_definitions(format INTERFACE FMT_HEADER_ONLY=1)

# argparsing library, only used in main program not the library
add_library(args INTERFACE)
target_include_directories(args INTERFACE ext/headers)

add_library(json INTERFACE)
target_include_directories(json INTERFACE ext/headers)

option(BUILD_SHARED_LIBS "Build shared libraries (ON) or static libraries (OFF)" OFF)

if(BUILD_SHARED_LIBS)
    set(LIB_TYPE SHARED)
else()
    set(LIB_TYPE STATIC)
endif()

# Main DRAMSim Lib
add_library(dramsim3 ${LIB_TYPE}
    src/bankstate.cc
    src/channel_state.cc
    src/command_queue.cc
    src/common.cc
    src/configuration.cc
    src/controller.cc
    src/dram_system.cc
    src/hmc.cc
    src/refresh.cc
    src/simple_stats.cc
    src/timing.cc
    src/memory_system.cc
)

if (COSIM)
    target_sources(dramsim3
        PRIVATE src/cosimulation.cc
    )
endif(COSIM)

if (THERMAL)
    # dependency check
    # sudo apt-get install libatlas-base-dev on ubuntu
    find_package(BLAS REQUIRED)
    find_package(OpenMP REQUIRED)
    # YOU need to build superlu on your own. Do the following:
    # git submodule update --init
    # cd ext/SuperLU_MT_3.1 && make lib
    find_library(SUPERLU
        NAME superlu_mt_OPENMP libsuperlu_mt_OPENMP
        HINTS ${PROJECT_SOURCE_DIR}/ext/SuperLU_MT_3.1/lib/
    )

    target_link_libraries(dramsim3
        PRIVATE ${SUPERLU} f77blas atlas m ${OpenMP_C_FLAGS}
    )
    target_sources(dramsim3
        PRIVATE src/thermal.cc src/sp_ienv.c src/thermal_solver.c
    )
    target_compile_options(dramsim3 PRIVATE -DTHERMAL -D_LONGINT -DAdd_ ${OpenMP_C_FLAGS})

    add_executable(thermalreplay src/thermal_replay.cc)
    target_link_libraries(thermalreplay dramsim3 inih)
    target_compile_options(thermalreplay PRIVATE -DTHERMAL -D_LONGINT -DAdd_ ${OpenMP_C_FLAGS})
endif (THERMAL)

if (CMD_TRACE)
    target_compile_options(dramsim3 PRIVATE -DCMD_TRACE)
endif (CMD_TRACE)

if (ADDR_TRACE)
    target_compile_options(dramsim3 PRIVATE -DADDR_TRACE)
endif (ADDR_TRACE)


target_include_directories(dramsim3 INTERFACE src)
target_compile_options(dramsim3 PRIVATE -Wall)
target_link_libraries(dramsim3 PRIVATE inih format)
set_target_properties(dramsim3 PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS NO
)

# trace CPU, .etc
add_executable(dramsim3main src/main.cc src/cpu.cc)
target_link_libraries(dramsim3main PRIVATE dramsim3 args)
target_compile_options(dramsim3main PRIVATE)
set_target_properties(dramsim3main PROPERTIES
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS NO
)

# Unit testing
add_library(Catch INTERFACE)
target_include_directories(Catch INTERFACE ext/headers)

add_executable(dramsim3test EXCLUDE_FROM_ALL
    tests/test_config.cc
    tests/test_dramsys.cc
    tests/test_hmcsys.cc # IDK somehow this can literally crush your computer
)
target_link_libraries(dramsim3test Catch dramsim3)
target_include_directories(dramsim3test PRIVATE src/)

# We have to use this custome command because there's a bug in cmake
# that if you do `make test` it doesn't build your updated test files
# so we're stucking with `make dramsim3test` for now
add_custom_command(
    TARGET dramsim3test POST_BUILD
    COMMAND dramsim3test
    WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
    DEPENDS dramsim3test dramsim3
)
]]

function dramsim(home, build)
  import("core.base.option")
  local sim_dir = path.join(os.curdir(), "sim")
  local new_sim_dir = option.get("sim_dir") or os.getenv("SIM_DIR")
  if new_sim_dir then sim_dir = path.absolute(new_sim_dir) end

  local new_home = path.join(sim_dir, "dramsim")
  os.tryrm(new_home)
  os.cp(home, new_home)

  -- Replace the cmake file
  -- The new cmake file add an option to build the shared library
  -- Vcs only support compile dramsim3 using shared library
  io.writefile(path.join(new_home, "CMakeLists.txt"), new_cmakefile)

  local dramsim_build = path.join(new_home, "build")
  if not os.exists(dramsim_build) then os.mkdir(dramsim_build) end
  os.cd(dramsim_build)
  os.execv("cmake", {new_home, "-D", "COSIM=1", "-DCMAKE_C_COMPILER=clang",
    "-DCMAKE_CXX_COMPILER=clang++", "-DCMAKE_LINKER=clang++",
    "-DBUILD_SHARED_LIBS=ON",
    "-DCMAKE_POLICY_VERSION_MINIMUM=3.5"
  })
  os.execv("make", {"dramsim3", "-j", "8"})
  os.cd("-")
  os.cp(path.join(new_home, "libdramsim3.so"), dramsim_build)

  local src_cfg = path.join(new_home, "configs", "XiangShan-nanhu.ini")
  local tgt_cfg = path.join(dramsim_build, "XiangShan.ini")
  local cfg_lines = io.readfile(src_cfg)
  cfg_lines = cfg_lines .. "cpu_freq = 2000\n"
  cfg_lines = cfg_lines .. "dram_freq = 1600\n"
  io.writefile(tgt_cfg, cfg_lines)

  local cxx_flags = " -DWITH_DRAMSIM3"
  cxx_flags = cxx_flags .. " -I" .. path.join(new_home, "src")
  cxx_flags = cxx_flags .. format([[ -DDRAMSIM3_CONFIG=%s]], tgt_cfg)
  cxx_flags = cxx_flags .. format([[ -DDRAMSIM3_OUTDIR=%s]], build)
  local libfile = path.join(dramsim_build, "libdramsim3.so")
  return cxx_flags, libfile
end