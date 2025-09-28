set DESIGN_NAME "XiangShan-Nanhu"

# environment variable `PLDM_COMP_DIR` is set by pldm.lua(pldm_run)
set DESIGN_DIR $env(PLDM_COMP_DIR)

set CUST_INIT_QEL $env(PLDM_SCR_DIR)/loadmem.qel
debug $DESIGN_DIR
xeset designName $DESIGN_NAME

# environment variable `PLDM_HOST` is set by pldm.lua(load_z2/z1_env)
host $env(PLDM_HOST)

# wait until resource available
xc wait
xc zt0 xt0 on -tbrun -initok
date

# swap into emulator
run -swap

if { $CUST_INIT_QEL != "" } { source $CUST_INIT_QEL }

# run
run

xc off
exit
