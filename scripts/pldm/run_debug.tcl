# TODO:


set DESIGN_DIR $env(PLDM_COMP_DIR)
set CUST_INIT_QEL $env(PLDM_SCR_DIR)/loadmem.qel

debug $DESIGN_DIR

host $env(PLDM_HOST)
xc xt0 zt0 on -tbrun
database -open pldm_db
probe -create -all -depth all tb_top -database pldm_db
xeset traceMemSize 20000
xeset triggerPos 2
run -swap
if { $CUST_INIT_QEL != "" } { source $CUST_INIT_QEL }
run
database -upload
xc off
exit
