set DESIGN_NAME "XiangShan-Nanhu"

# environment variable `PLDM_COMP_DIR` is set by pldm.lua(pldm_run)
set DESIGN_DIR $env(PLDM_COMP_DIR)

debug $DESIGN_DIR
xeset designName $DESIGN_NAME

# environment variable `PLDM_HOST` is set by pldm.lua(load_z2_env)
host $env(PLDM_HOST)

# wait until resource available
xc wait
xc zt0 xt0 on -tbrun -initok
date

# swap into emulator
run -swap

# run
run

xc off
exit
