LN_FILE?=bosc_FullSys.f
LIB_FILELIST?=
TOP_MODULE_NAME?=bosc_LNTop
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

gen_search_module_sv: 
	python gen_search_script.py $(LN_FILE) $(TOP_MODULE_NAME) $(LIB_FILELIST)

compile_search_tb:
	vcs ${COMPILE_OPTIONS} -f search_filelist.f

search_module:
	./simv -l search.log -ucli -i search.tcl

total_search_module: gen_search_module_sv compile_search_tb search_module

complie_tb:
	vcs ${COMPILE_OPTIONS} -f filelist.f

gen_tb:
	python scv_gen.py $(LN_FILE) $(LIB_FILELIST)

run:
	./simv > run_log.txt

search_sram:
	./simv -l search.log -ucli -i search.tcl

get_sram_path:
	python get_sram_path.py

temp_compile: gen_tb complie_tb run

gen_run: total_search_module gen_tb complie_tb run search_sram get_sram_path

