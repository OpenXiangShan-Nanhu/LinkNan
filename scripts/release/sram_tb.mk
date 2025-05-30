CORE_FILE?=bosc_NanhuCoreWrapper.f
LN_FILE?=bosc_FullSys.f
#LIB_FILELIST?=
#LN_FILE ?= bosc_LNTop.f


RTL_RELEASE_ENTRIES := $(wildcard ./$(RELEASE_DIRS)/Release*)
RTL_RELEASE_DIRS := $(filter-out $(wildcard ./$(RELEASE_DIRS)/Release*.*), $(RTL_RELEASE_ENTRIES))

rtl_path = $(abspath ./$(RTL_RELEASE_DIRS)/)
export release_path=$(rtl_path)

ver_echo:
	@echo "Exported release_path: $$release_path"

core_dir_sram_tb:
	mkdir -p ./sim
	mkdir -p ./sim/sram_tb_core

core_copy_core_tb:
	cp ./env/sram_tb/core_tb/* ./sim/sram_tb_core/

core_run_core_sram_tb:
ifeq ($(LIB_FILELIST), )
	$(MAKE) -C ./sim/sram_tb_core gen_run CORE_FILE=$(CORE_FILE)
else
	$(MAKE) -C ./sim/sram_tb_core gen_run CORE_FILE=$(CORE_FILE) LIB_FILELIST=$(LIB_FILELIST)
endif

core_run: core_dir_sram_tb core_copy_core_tb core_run_core_sram_tb

ln_dir_sram_tb:
	mkdir -p ./sim
	mkdir -p ./sim/sram_tb_ln

ln_copy_core_tb:
	cp ./env/sram_tb/ln_tb/* ./sim/sram_tb_ln/

ln_run_core_sram_tb:
ifeq ($(LIB_FILELIST), )
	$(MAKE) -C ./sim/sram_tb_ln gen_run LN_FILE=$(LN_FILE)
else 
	$(MAKE) -C ./sim/sram_tb_ln gen_run LN_FILE=$(LN_FILE) LIB_FILELIST=$(LIB_FILELIST)
endif

ln_run: ln_dir_sram_tb ln_copy_core_tb ln_run_core_sram_tb

run: ver_echo ln_run core_run

clean:
	rm -rf ./sim/sram_tb_ln ./sim/sram_tb_core


