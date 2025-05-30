import csv
import re
import sys
import numpy as np
import os

#if len(sys.argv) != 2:
#    print("need indicate filelist")
#    sys.exit(1)

input_filelist = sys.argv[1]
lib_filelist = ""
if(len(sys.argv) == 3):
    lib_filelist = sys.argv[2]

#if "Core" in input_csvname:
#    csvname = "Core"
#elif "L2Cache" in input_csvname:
#    csvname = "L2Cache"
#else:
#    print("find undefined csv name")
#    sys.exit(1)


def get_Release_folder(relative_path):
    entries = os.listdir(relative_path)
    release_folders = [entry for entry in entries if entry.startswith('Release') and os.path.isdir(os.path.join(relative_path, entry))]
    if len(release_folders) == 1:
        return release_folders[0]
    else:
        raise ValueError("Expected exactly one release folder, but found: {}".format(release_folders))

release_rtl = get_Release_folder("../../")

flielist_content = f"""
./sram_test_tb.sv
-f ../../{release_rtl}/{input_filelist}
+define+SYNTHESIS
+define+NTC
+define+MEM_CHECK_OFF
+define+no_warning
+define+UNIT_DELAY

"""

with open(f"filelist.f", "w", encoding="utf-8") as file:
    if(lib_filelist != ""):
        file.write(f"-f ../../{release_rtl}/{lib_filelist}")
    file.write(flielist_content)


l2CSVName = "bosc_LNTop.MbistL2Cache.csv"
coreCSVName = "bosc_LNTop.MbistCore.csv"


filename = f"../../{release_rtl}/mbist/{l2CSVName}"
#filename = f"./real_mbist/{l2CSVName}"
l2_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        l2_data.append(row)

l2_data = l2_data[3:]
l2_sram_name = []
l2_sram_type = []
l2_sram_array = []
l2_pipeline_depth = []
l2_bitwrite = []
l2_bank_addr = []
l2_select0H_width = []
l2_foundry = []
l2_sram_inst = []

for row in l2_data:
    l2_sram_name.append(row[0])
    l2_sram_type.append(row[1].rstrip('.sv'))
    l2_sram_array.append(row[2])
    l2_pipeline_depth.append(row[3])
    l2_bitwrite.append(row[4])
    l2_bank_addr.append(row[5])
    l2_select0H_width.append(row[6])
    l2_foundry.append(row[7])
    l2_sram_inst.append(row[8])

l2_sram_array     = [int(x) for x in l2_sram_array]
l2_pipeline_depth = [int(x) for x in l2_pipeline_depth]
l2_select0H_width = [int(x) for x in l2_select0H_width]
l2_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in l2_bitwrite]

l2_params = []

for sram_type_str in l2_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        l2_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        l2_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        l2_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        l2_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        l2_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        l2_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        l2_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        l2_params.append((l2_p_num, l2_x_nums, l2_ms_num, l2_s_num, l2_h_num, l2_l_num, l2_has_b))

l2_data_width = []
l2_data_width = [0] * len(l2_select0H_width)
for i in range(len(l2_select0H_width)):
    l2_data_width[i] = int((l2_params[i][1][1])/l2_select0H_width[i])

l2_addr_width = []
l2_addr_width = [0] * len(l2_select0H_width)
for i in range(len(l2_select0H_width)):
    l2_addr_width[i] = int(np.log2(l2_params[i][1][0]))

l2_array_width = []
l2_array_width = [0] * len(l2_select0H_width)
for i in range(len(l2_select0H_width)):
    l2_array_width[i] = int(np.log2(len(l2_sram_array)))

l2_mask_width = []
l2_mask_width = [0] * len(l2_select0H_width)
for i in range(len(l2_select0H_width)):
    l2_mask_width[i] = int(l2_params[i][1][1]/l2_params[i][2])


filename = f"../../{release_rtl}/mbist/{coreCSVName}"
#filename = f"./real_mbist/{coreCSVName}"
core_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        core_data.append(row)

core_data = core_data[3:]
core_sram_name = []
core_sram_type = []
core_sram_array = []
core_pipeline_depth = []
core_bitwrite = []
core_bank_addr = []
core_select0H_width = []
core_foundry = []
core_sram_inst = []

for row in core_data:
    core_sram_name.append(row[0])
    core_sram_type.append(row[1].rstrip('.sv'))
    core_sram_array.append(row[2])
    core_pipeline_depth.append(row[3])
    core_bitwrite.append(row[4])
    core_bank_addr.append(row[5])
    core_select0H_width.append(row[6])
    core_foundry.append(row[7])
    core_sram_inst.append(row[8])

core_sram_array     = [int(x) for x in core_sram_array]
core_pipeline_depth = [int(x) for x in core_pipeline_depth]
core_select0H_width = [int(x) for x in core_select0H_width]
core_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in core_bitwrite]

core_params = []

for sram_type_str in core_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        core_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        core_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        core_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        core_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        core_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        core_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        core_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        core_params.append((core_p_num, core_x_nums, core_ms_num, core_s_num, core_h_num, core_l_num, core_has_b))

core_data_width = []
core_data_width = [0] * len(core_select0H_width)
for i in range(len(core_select0H_width)):
    core_data_width[i] = int((core_params[i][1][1])/core_select0H_width[i])

core_addr_width = []
core_addr_width = [0] * len(core_select0H_width)
for i in range(len(core_select0H_width)):
    core_addr_width[i] = int(np.log2(core_params[i][1][0]))

core_array_width = []
core_array_width = [0] * len(core_select0H_width)
for i in range(len(core_select0H_width)):
    core_array_width[i] = int(np.log2(len(core_sram_array)))

core_mask_width = []
core_mask_width = [0] * len(core_select0H_width)
for i in range(len(core_select0H_width)):
    core_mask_width[i] = int(core_params[i][1][1]/core_params[i][2])





# Print the results to verify
#print("Results:")
#for param in params:
#    print(param)
#
#
#print("SRAM Name:", l2_sram_name)
#print("SRAM Type:", l2_sram_type)
#print("SRAM Array:", l2_sram_array)
#print("Pipeline Depth:", l2_pipeline_depth)
#print("BitWrite:", l2_bitwrite)
#print("Bank Addr:", l2_bank_addr)
#print("Select0H Width:", l2_select0H_width)
#print("Foundry:", l2_foundry)
#print("SRAM Inst:", l2_sram_inst)

#l2_unique_sram_type = list(set(l2_sram_type))
#core_unique_sram_type = list(set(core_sram_type))
#l2_unique_sram_inst = list(set(l2_sram_inst))
#core_unique_sram_inst = list(set(core_sram_inst))


write_to_search_tcl = []
write_to_search_tcl = [0] * (len(l2_sram_inst) + len(core_sram_inst))
for i in range(len(l2_sram_inst)):
    if(l2_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i] = f"search -module {l2_sram_type[i]}\n"
    else:
        write_to_search_tcl[i] = f"search -module {l2_sram_inst[i]}\n"
for i in range(len(core_sram_inst)):
    if(core_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i  + len(l2_sram_inst)] = f"search -module {core_sram_type[i]}\n"
    else:
        write_to_search_tcl[i + len(l2_sram_inst)] = f"search -module {core_sram_inst[i]}\n"
unique_write_to_search_tcl = list(set(write_to_search_tcl))



with open(f"search.tcl", "w", encoding="utf-8") as file:
    for tcl in unique_write_to_search_tcl:
        file.write(tcl)
    file.write("q\n")


tb_init = f"""
module sram_test_tb();

bit clk;
bit reset;


initial
begin
    clk = 1'b0;
    reset = 1'b1;
    #100; 
    reset = 1'b0;
end 
always #5 clk = ~clk;

bit [{max(l2_array_width)}:0]   l2_mbist_array;
bit         l2_mbist_req;
bit         l2_mbist_writeen;
bit [{max(l2_mask_width)-1}:0]   l2_mbist_be;
bit [{max(l2_addr_width)}:0]  l2_mbist_addr;
bit [{max(l2_data_width)-1}:0] l2_mbist_indata;
bit         l2_mbist_readen;
bit [{max(l2_addr_width)}:0]  l2_mbist_addr_rd;
bit [{max(l2_data_width)-1}:0] l2_mbist_outdata;
int         l2_sram_lat_array[0:{max(l2_sram_array)}];
string l2_sram_name_array[0:{len(l2_sram_array)-1}];

bit [{max(core_array_width)}:0]   core_mbist_array;
bit         core_mbist_req;
bit         core_mbist_writeen;
bit [{max(core_mask_width)-1}:0]   core_mbist_be;
bit [{max(core_addr_width)}:0]  core_mbist_addr;
bit [{max(core_data_width)-1}:0] core_mbist_indata;
bit         core_mbist_readen;
bit [{max(core_addr_width)}:0]  core_mbist_addr_rd;
bit [{max(core_data_width)-1}:0] core_mbist_outdata;
int         core_sram_lat_array[0:{max(core_sram_array)}];
string core_sram_name_array[0:{len(core_sram_array)-1}];

initial
begin
"""

dri_ini = f"""
//for sram test driver
initial
begin
    wait(reset == 1'b0);
    @(posedge clk);
    l2_mbist_req = 1'b1;
    core_mbist_req = 1'b1;
    @(posedge clk);
"""

l2_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    l2_mbist_addr_rd = 'd0;
                    l2_mbist_addr = 'd0;
                end
                else begin
                    l2_mbist_addr_rd = 1 << (i-1);
                    l2_mbist_addr = 1 << (i-1);
                end
"""
l2_mask_driver_last = f"""
                else begin
                    l2_mbist_be = 'd0;
                end
                l2_mbist_writeen = 1'b1;
                l2_mbist_readen = 1'b0;
                @(posedge clk);
                l2_mbist_writeen = 1'b0;
                l2_mbist_readen = 1'b1;
                @(posedge clk);
                l2_mbist_readen = 1'b0;
            end
"""

core_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    core_mbist_addr_rd = 'd0;
                    core_mbist_addr = 'd0;
                end
                else begin
                    core_mbist_addr_rd = 1 << (i-1);
                    core_mbist_addr = 1 << (i-1);
                end
"""
core_mask_driver_last = f"""
                else begin
                    core_mbist_be = 'd0;
                end
                core_mbist_writeen = 1'b1;
                core_mbist_readen = 1'b0;
                @(posedge clk);
                core_mbist_writeen = 1'b0;
                core_mbist_readen = 1'b1;
                @(posedge clk);
                core_mbist_readen = 1'b0;
            end
"""

l2_nomask_nolat_driver = f"""
            if(j == 0)begin
                l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                l2_mbist_addr_rd = 'd0;
                l2_mbist_addr = 'd0;
            end
            else begin
                l2_mbist_addr_rd = 1 << (i-1);
                l2_mbist_addr = 1 << (i-1);
            end
            l2_mbist_writeen = 1'b1;
            l2_mbist_be = 'd0;//no mask
            l2_mbist_readen = 1'b0;
            @(posedge clk);
            l2_mbist_writeen = 1'b0;
            l2_mbist_readen = 1'b1;
            @(posedge clk);
            l2_mbist_readen = 1'b0;
"""

core_nomask_nolat_driver = f"""
            if(j == 0)begin
                core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                core_mbist_addr_rd = 'd0;
                core_mbist_addr = 'd0;
            end
            else begin
                core_mbist_addr_rd = 1 << (i-1);
                core_mbist_addr = 1 << (i-1);
            end
            core_mbist_writeen = 1'b1;
            core_mbist_be = 'd0;//no mask
            core_mbist_readen = 1'b0;
            @(posedge clk);
            core_mbist_writeen = 1'b0;
            core_mbist_readen = 1'b1;
            @(posedge clk);
            core_mbist_readen = 1'b0;
"""

l2_nomask_lat2_driver = f"""
            if(j == 0)begin
                l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                l2_mbist_indata = {{{int(max(l2_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                l2_mbist_addr_rd = 'd0;
                l2_mbist_addr = 'd0;
            end
            else begin
                l2_mbist_addr_rd = 1 << (i-1);
                l2_mbist_addr = 1 << (i-1);
            end
            l2_mbist_be = 'd0;//no write mask
            l2_mbist_writeen = 1'b1;
            l2_mbist_readen = 1'b0;
            @(posedge clk);
            l2_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            l2_mbist_writeen = 1'b0;
            l2_mbist_readen = 1'b1;
            @(posedge clk);
            l2_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

core_nomask_lat2_driver = f"""
            if(j == 0)begin
                core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                core_mbist_indata = {{{int(max(core_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                core_mbist_addr_rd = 'd0;
                core_mbist_addr = 'd0;
            end
            else begin
                core_mbist_addr_rd = 1 << (i-1);
                core_mbist_addr = 1 << (i-1);
            end
            core_mbist_be = 'd0;//no write mask
            core_mbist_writeen = 1'b1;
            core_mbist_readen = 1'b0;
            @(posedge clk);
            core_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            core_mbist_writeen = 1'b0;
            core_mbist_readen = 1'b1;
            @(posedge clk);
            core_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

ver_init = f"""
//ver initial

int     error_count;
bit     l2_compare_finish[0:{len(l2_sram_array)-1}];
bit     core_compare_finish[0:{len(core_sram_array)-1}];
initial
begin
    error_count = 0;
end

"""
ver_finish = f"""
    if(error_count == 'd0)begin
        $display("TEST CASE PASS");
    end
    else begin
        $display("TEST CASE FAIL");
    end
    #100;
    $finish();
end
initial
begin
    force dut_inst.l2cache.io_dft_func_ram_hold = 1'b0;
    force dut_inst.l2cache.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.l2cache.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.l2cache.io_dft_func_cgen = 1'b0;
    force dut_inst.l2cache.io_dft_func_ram_aux_clk = 1'b0;
    force dut_inst.l2cache.io_dft_func_ram_aux_ckbp = 1'b0;
    force dut_inst.l2cache.io_dft_func_ram_mcp_hold = 1'b0;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_array = l2_mbist_array;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_req = l2_mbist_req;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_writeen = l2_mbist_writeen;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_be = l2_mbist_be;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_addr = l2_mbist_addr;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_indata = l2_mbist_indata;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_readen = l2_mbist_readen;
    force dut_inst.l2cache.mbistIntfL2Cache.mbist_addr_rd = l2_mbist_addr_rd;
    force l2_mbist_outdata = dut_inst.l2cache.mbistIntfL2Cache.mbist_outdata;
end

initial
begin
    force dut_inst.core.io_dft_func_ram_hold = 1'b0;
    force dut_inst.core.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.core.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.core.io_dft_func_cgen = 1'b0;
    force dut_inst.core.mbistIntfCore.mbist_array = core_mbist_array;
    force dut_inst.core.mbistIntfCore.mbist_req = core_mbist_req;
    force dut_inst.core.mbistIntfCore.mbist_writeen = core_mbist_writeen;
    force dut_inst.core.mbistIntfCore.mbist_be = core_mbist_be;
    force dut_inst.core.mbistIntfCore.mbist_addr = core_mbist_addr;
    force dut_inst.core.mbistIntfCore.mbist_indata = core_mbist_indata;
    force dut_inst.core.mbistIntfCore.mbist_readen = core_mbist_readen;
    force dut_inst.core.mbistIntfCore.mbist_addr_rd = core_mbist_addr_rd;
    force core_mbist_outdata = dut_inst.core.mbistIntfCore.mbist_outdata;
end

initial begin
    $fsdbDumpfile("sram_test_core.fsdb");
    $fsdbDumpvars(0, sram_test_tb);
end

bosc_NanhuCoreWrapper dut_inst(
    .io_clock(clk),
    .io_reset(reset)
);

endmodule
"""



with open(f"sram_test_tb.sv", "w", encoding="utf-8") as file:
    file.write(tb_init)
    for i in range(len(l2_sram_array)):
        file.write(f"   l2_sram_lat_array[{i}] = {l2_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(core_sram_array)):
        file.write(f"   core_sram_lat_array[{i}] = {core_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(l2_sram_array)):
        file.write(f"   l2_sram_name_array[{i}] = \"{l2_sram_type[i]}\";\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(core_sram_array)):
        file.write(f"   core_sram_name_array[{i}] = \"{core_sram_type[i]}\";\n")
    file.write("end\n")


    file.write(dri_ini)
    for i in range(len(l2_sram_array)):
        file.write(f"    l2_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({l2_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(l2_bitwrite[i] == 1):
            file.write(l2_mask_driver_first)
            temp_write_data = int(l2_params[i][1][1]/l2_params[i][2])
            file.write(f"                if(k == 0)begin\n")
            file.write(f"                    l2_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
            file.write(f"                end\n")
            file.write(l2_mask_driver_last)
        elif(l2_bitwrite[i] == 0) and (l2_params[i][5] == 1):
            file.write(l2_nomask_nolat_driver)
        elif(l2_bitwrite[i] == 0) and (l2_params[i][5] == 2):
            file.write(l2_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < l2_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(dri_ini)
    for i in range(len(core_sram_array)):
        file.write(f"    core_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({core_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(core_bitwrite[i] == 1):
            file.write(core_mask_driver_first)
            temp_write_data = int(core_params[i][1][1]/core_params[i][2])
            file.write(f"                if(k == 0)begin\n")
            file.write(f"                    core_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
            file.write(f"                end\n")
            file.write(core_mask_driver_last)
        elif(core_bitwrite[i] == 0) and (core_params[i][5] == 1):
            file.write(core_nomask_nolat_driver)
        elif(core_bitwrite[i] == 0) and (core_params[i][5] == 2):
            file.write(core_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < core_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(ver_init)

    for i in range(len(l2_sram_array)):
        file.write(f"bit    [{max(l2_array_width)}:0] l2_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(l2_mbist_readen == 1'b1 && l2_mbist_array == 'd{i});\n")
        file.write(f"    l2_mbist_array_{i} = l2_mbist_array;\n")
        file.write(f"    for(int i = 0; i < l2_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(l2_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({l2_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(l2_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(l2_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    l2_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'h5}}}}[{l2_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(l2_mbist_outdata[{l2_data_width[i]-1}:0] == {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , simulating %s ,ver pass\" , $time , l2_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for L2's SRAM , when mbist_array is %d simulating %s, ver has error\" , $time , l2_mbist_array_{i} , l2_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , l2_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(l2_data_width)/4+1)}{{4'ha}}}}[{l2_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   l2_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")


    for i in range(len(core_sram_array)):
        file.write(f"bit    [{max(core_array_width)}:0] core_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(core_mbist_readen == 1'b1 && core_mbist_array == 'd{i});\n")
        file.write(f"    core_mbist_array_{i} = core_mbist_array;\n")
        file.write(f"    for(int i = 0; i < core_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(core_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({core_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(core_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(core_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    core_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'h5}}}}[{core_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(core_mbist_outdata[{core_data_width[i]-1}:0] == {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , simulating %s ,ver pass\" , $time , core_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for Core's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , core_mbist_array_{i} , core_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , core_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(core_data_width)/4+1)}{{4'ha}}}}[{core_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   core_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    file.write(f"initial\n")
    file.write(f"begin\n")
    file.write(f"   wait(\n")
    for i in range(len(l2_sram_array)):
        if(i == len(l2_sram_array)-1):
            file.write(f"       l2_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       l2_compare_finish[{i}] == 1'b1 &&\n")
    file.write(f"   wait(\n")
    for i in range(len(core_sram_array)):
        if(i == len(core_sram_array)-1):
            file.write(f"       core_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       core_compare_finish[{i}] == 1'b1 &&\n")

    #file.write(f"    $display(\"{len(l2_sram_array) + len(core_sram_array)} sram has verified\");\n")
    file.write(ver_finish)


