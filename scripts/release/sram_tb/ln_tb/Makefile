LN_FILE ?= FPGA.f
LIB_FILELIST?=
COMPILE_OPTIONS := -l compile.log \
                   -CFLAGS -DVCS \
                   -lca -kdb \
                   -full64 +v2k -sverilog \
                   -timescale=1ns/1ns \
                   +plusarg_save -debug_access+pp+dmptf+thread \
                   -debug_region=cell+encrypt -notice \
                   -debug_all \
                   +delay_mode_unit \
                   +nospecify +notimingcheck +no_tchk_msg -add_seq_delay 20ps \
                   #-P /nfs/tools/synopsys/verdi/S-2021.09-SP1/share/PLI/VCS/linux64/novas.tab /nfs/tools/synopsys/verdi/S-2021.09-SP1/share/PLI/VCS/linux64/pli.a \


complie_tile:
	vcs ${COMPILE_OPTIONS} -f filelist.f

gen_tb:
	python scv_gen.py $(LN_FILE) $(LIB_FILELIST)

run:
	./simv > run_log_ln.txt

search_sram:
	./simv -l search.log -ucli -i search.tcl

get_sram_path:
	python get_sram_path.py

gen_run: gen_tb complie_tile run search_sram get_sram_path

