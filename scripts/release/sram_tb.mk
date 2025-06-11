CORE_FILE?=bosc_NanhuCoreWrapper.f
LN_FILE?=bosc_FullSys.f
TOP_MODULE_NAME?=bosc_LNTop
#LIB_FILELIST?=
#LN_FILE ?= bosc_LNTop.f


RTL_RELEASE_ENTRIES := $(wildcard ./$(RELEASE_DIRS)/Release*)
RTL_RELEASE_DIRS := $(filter-out $(wildcard ./$(RELEASE_DIRS)/Release*.*), $(RTL_RELEASE_ENTRIES))

rtl_path = $(abspath ./$(RTL_RELEASE_DIRS)/)
export release_path=$(rtl_path)

ver_echo:
	@echo "Exported release_path: $$release_path"

dir_sram_tb:
	mkdir -p ./sim
	mkdir -p ./sim/sram_tb

copy_sram_tb:
	cp ./env/sram_tb/* ./sim/sram_tb/

run_sram_tb:
ifeq ($(LIB_FILELIST), )
	$(MAKE) -C ./sim/sram_tb gen_run LN_FILE=$(LN_FILE) TOP_MODULE_NAME=$(TOP_MODULE_NAME)
else
	$(MAKE) -C ./sim/sram_tb gen_run LN_FILE=$(LN_FILE) TOP_MODULE_NAME=$(TOP_MODULE_NAME) LIB_FILELIST=$(LIB_FILELIST)
endif

run: ver_echo dir_sram_tb copy_sram_tb run_sram_tb

clean:
	rm -rf ./sim/sram_tb


