set DESIGN_NAME "XiangShan-Nanhu"

set DESIGN_DIR $env(PLDM_COMP_DIR)

debug $DESIGN_DIR
xeset designName $DESIGN_NAME
host $env(PLDM_HOST)

xc wait
xc xt0 zt0 on -tbrun -initok

database -open pldm_db
probe -create -all -depth all tb_top -database pldm_db
xeset traceMemSize 20000
xeset triggerPos 2

run -swap

if {[info exists env(PLDM_RUN_MEMINIT_SCR)]} {
  source $env(PLDM_RUN_MEMINIT_SCR)
}

run

database -upload

xc off
exit
