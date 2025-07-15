set DESIGN_NAME "XiangShan-Nanhu"
set DESIGN_DIR .

debug $DESIGN_DIR
xeset designName $DESIGN_NAME
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
