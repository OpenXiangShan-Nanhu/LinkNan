# LinkNan
`LinkNan` (岭南) is the SoC system integrating the [Nanhu](https://github.com/Siudya/Nanhu) high performance RISC-V core and [ZhuJiang](https://github.com/Siudya/ZhuJiang) NoC.

# Compile source code
Notice: Make sure that you have installed `xmake`. If not, please refer to the [xmake](https://github.com/xmake-io/xmake) official website for installation.
```bash
xmake init
xmake comp
```

# Generate LinkNan
```bash
xmake soc --help # Display options of soc task

# Typical usage, use for physical design:
# -g: use vcs style
# -r: generate release package
# -m: enable MBIST
# -A: enable hardware assertion
# -x: add prefix for all modules
xmake soc -grmA -x bosc_

# Used in scripts/release.sh
# -s generate SimTop to wrapper simulation memory and LinkNan itself
xmake soc -sgrmA -x bosc_

# Config NoC, LLC and Core
# -N small: use small NoC config, full by default
# -L medium: use medium LLC config, full by default
# -C minimal: use minimal Core config, full by default
# All permitted config string can be seen in Config.scala 
xmake soc -N small -L medium -C minimal
```

# Simulation with Verilator
## Compile EMU
```bash
# Task emu is used for compiling env
# -d use dramsim3
# -p disable perf counter
# -n disable difftest
# -f disable wave dump
# -j compile jobs, 16 by default
# -l enable cache scoreboard co-sim
# -Y use xiangshan legacy memory map for CLINT and PLIC
# -t simulation threads, 16 by default
# -r choose reference model, Nemu by default
# -C Core config, full by default
# -N NoC config, small by default
# -L LLC config, small by default

# This line is used for run legacy xiangshan checkpoints or linux bin
xmake emu -j64 -dY -L medium

# This line is used for run program with lua scb enabled
xmake emu -j64 -l -L medium
```
## Run simulation
```bash
# Task emu-run is used for running bare-metal programs
# -d: disable fork, dump wave anyway
# -i: program bin
# -f: flash bin
# -z: program gz
# -w: workload bin/gz(fullpath)
# -c: max sim cycles, 0 means no limit
# -X: fork interval, 50 by default
# -b: wave dump begin time
# -e: wave dump end time
# -W: warmup intr num
# -I: max intr num
# -s: randomization seed

# This line is used for running linux.bin in ready-to-run, needs xmake emu -Y
xmake emu-run -i linux -X 15

# This line is used for running dhrystone.bin in ready-to-run with qual core
xmake emu-run -i dhrystone -f flash --ref=iscv64-nemu-interpreter-multicore-so
```

# Simluation with Synopsys VCS
## Compile simv
```bash
# Task simv is used for compiling env
# Option for task simv are mostly similar as task emu
# -x: disbale -xprop
# This line is used for compile simv
xmake simv
```
## Run simulation
```bash
# Task simv-run is used for running simulation
# -I: init reg, 0/1/random, needs xmake simv -x
# -i: program bin
# -f: flash bin
# -z: program gz
# -w: workload bin/gz(fullpath)
# -c: max cycles
# This line is used for running dhrystone.bin in ready-to-run
xmake simv-run -i dhrystone

# This line is used for running dhrystone.bin in ready-to-run with qual core
xmake simv-run -i dhrystone -f flash --ref=iscv64-nemu-interpreter-multicore-so
```