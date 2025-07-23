ABS_WORK_DIR = $(shell pwd)
RELEASE_DIR = $(shell find $(ABS_WORK_DIR) -name "Release-LinkNan*")
SIM_DIR = $(ABS_WORK_DIR)/sim
NEMU_DIR = $(ABS_WORK_DIR)/NEMU
CASES_DIR = $(ABS_WORK_DIR)/cases

CFG_CIR           = $(ABS_WORK_DIR)/env/difftest/config
SIM_CSRC_DIR 	  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/common
PLUGIN_CHEAD_DIR  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/plugin/include
PLUGIN_CSRC_DIR   = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/plugin/spikedasm
DIFFTEST_CSRC_DIR = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/difftest
VCS_CSRC_DIR 	  = $(ABS_WORK_DIR)/env/difftest/src/test/csrc/vcs
GEN_CSRC_DIR      = $(ABS_WORK_DIR)/env/generated-src

SIM_COMMON_DIR    = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/common
SIM_ST_DIR        = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/st
VCS_TOP_DIR       = $(ABS_WORK_DIR)/env/difftest/src/test/vsrc/vcs
SIMTOP_DIR        = $(ABS_WORK_DIR)/env/sim

# VCS compile
ifndef VERDI_HOME
$(error VERDI_HOME is not set. Try whereis verdi, abandon /bin/verdi and set VERID_HOME manually)
else
NOVAS_HOME = $(VERDI_HOME)
NOVAS = $(NOVAS_HOME)/share/PLI/VCS/LINUX64
EXTRA += +define+CONSIDER_FSDB -P $(NOVAS)/novas.tab $(NOVAS)/pli.a
endif

VCS_FLAGS += -full64 +v2k -timescale=1ns/10ps -sverilog -debug_access+all +lint=TFIPC-L
VCS_FLAGS += -l vcs.log -top tb_top -fgp -lca -kdb +nospecify +notimingcheck -no_save -xprop
VCS_FLAGS += +define+DIFFTEST +define+ASSERT_VERBOSE_COND_=1 +define+PRINTF_COND_=1
VCS_FLAGS += +define+STOP_COND_=1 +define+VCS +incdir+$(GEN_CSRC_DIR)
VCS_FLAGS += -CFLAGS "$(VCS_CXXFLAGS)" -LDFLAGS "$(VCS_LDFLAGS)" -j200
VCS_FLAGS += $(EXTRA)

VCS_CXXFLAGS += -std=c++17 -static -Wall -DREF_PROXY=NemuProxy -DNUM_CORES=4
VCS_CXXFLAGS += -I$(CFG_CIR) -I$(GEN_CSRC_DIR) -I$(VCS_CSRC_DIR) -I$(SIM_CSRC_DIR)
VCS_CXXFLAGS += -I$(PLUGIN_CHEAD_DIR) -I$(PLUGIN_CSRC_DIR) -I$(DIFFTEST_CSRC_DIR)
VCS_LDFLAGS += -Wl,--no-as-needed -lpthread -lSDL2 -ldl -lz -lzstd


DUT_FILELIST = $(SIM_DIR)/dut.f
ENV_FILELIST = $(SIM_DIR)/env.f

FORCE:

$(DUT_FILELIST): FORCE
	@mkdir -p $(SIM_DIR)
	@cat $(RELEASE_DIR)/bosc_FullSys.f \
		 | sort -u > $(DUT_FILELIST)
	sed -i 's|$$release_path|$(RELEASE_DIR)|g' $(DUT_FILELIST)

$(ENV_FILELIST): FORCE
	@mkdir -p $(SIM_DIR)
	@find $(SIM_CSRC_DIR) -name "*.cpp" > $(ENV_FILELIST)
	@find $(PLUGIN_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(DIFFTEST_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(GEN_CSRC_DIR) -name "*.cpp" >> $(ENV_FILELIST)
	@find $(VCS_CSRC_DIR) -name "*.cpp" -or -name "*.c" >> $(ENV_FILELIST)
	@find $(SIM_COMMON_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(SIM_ST_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(SIMTOP_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)
	@find $(VCS_TOP_DIR) -name "*.v" -or -name "*.sv" >> $(ENV_FILELIST)


simv: $(DUT_FILELIST) $(ENV_FILELIST)
	@mkdir -p $(SIM_DIR)/comp
	cd $(SIM_DIR)/comp && vcs $(VCS_FLAGS) -f $(DUT_FILELIST) -f $(ENV_FILELIST)
	rm $(ENV_FILELIST)

# VCS simulation
libnemu := $(NEMU_DIR)/build/riscv64-nemu-interpreter-so

$(libnemu):
	NEMU_HOME=$(NEMU_DIR) make -C $(NEMU_DIR) riscv64-nhv5-multi-ref_defconfig
	NEMU_HOME=$(NEMU_DIR) make -C $(NEMU_DIR) -j

RUN_BIN_DIR = $(CASES_DIR)
RUN_BIN  ?= dhrystone.bin
RUN_OPTS += -fgp=num_threads:4,num_fsdb_threads:4
RUN_OPTS += -assert finish_maxfail=30 -assert global_finish_maxfail=10000
RUN_OPTS += +dump-wave=fsdb +workload=$(RUN_BIN_DIR)/$(RUN_BIN)
RUN_OPTS += $(if $(filter 0,$(DIFF)),+no-diff,+diff=$(libnemu))

run: $(libnemu)
	@mkdir -p $(SIM_DIR)/$(RUN_BIN)
	@touch $(SIM_DIR)/$(RUN_BIN)/sim.log
	@rm -f $(SIM_DIR)/$(RUN_BIN)/simv
	@rm -rf $(SIM_DIR)/$(RUN_BIN)/simv.daidir
	@ln -s $(SIM_DIR)/comp/simv $(SIM_DIR)/$(RUN_BIN)/simv
	@ln -s $(SIM_DIR)/comp/simv.daidir $(SIM_DIR)/$(RUN_BIN)/simv.daidir
	cd $(SIM_DIR)/$(RUN_BIN) && (./simv $(RUN_OPTS) 2> assert.log | tee sim.log)

clean:
	rm -rf $(SIM_DIR)
	make -C NEMU clean
	make -C NEMU clean-softfloat