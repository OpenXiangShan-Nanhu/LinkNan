set DESIGN_NAME "XiangShan-Nanhu"

set DESIGN_DIR $env(PLDM_COMP_DIR)

set CUST_INIT_QEL $env(PLDM_RUN_MEMINIT_SCR)

debug $DESIGN_DIR
xeset designName $DESIGN_NAME
host $env(PLDM_HOST)
xc xt0 zt0 on -tbrun -initok

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
