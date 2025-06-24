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

#get rtl path
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
//-f ./memory_verilog.f
//-f ./sim_filelist.f
-f ../../{release_rtl}/{input_filelist}
+define+SYNTHESIS
+define+NTC
+define+MEM_CHECK_OFF
+define+no_warning
+define+UNIT_DELAY
"""

#gen filelist.f
with open(f"filelist.f", "w", encoding="utf-8") as file:
    if(lib_filelist != ""):
        file.write(f"-f ../../{release_rtl}/{lib_filelist}")
    file.write(flielist_content)

input_file_path = "search.log"
with open(input_file_path, 'r') as file:
    file_content = file.read()
pattern = re.compile(r'search_module_tb\.(.*?)}', re.DOTALL)
module_matches = pattern.findall(file_content)
#for match in module_matches:
#    print("module path:", match)


split_module_dir = {}

for item in module_matches:
    mbist_part = item.split('mbistIntf')[-1]
    if mbist_part not in split_module_dir:
        split_module_dir[mbist_part] = []
    split_module_dir[mbist_part].append(item)

#for key, value in split_module_dir.items():
#    print(f"Array {key}: {value}")


with open('module_list.txt', 'r') as file:
    lines = file.readlines()

top_module_name = None
unit_top_array = []
interface_array = []
csv_files_array = []

for line in lines:
    line = line.strip()
    if line.startswith('topModule:'):
        top_module_name = line.split(':')[1].strip()
    else:
        words = line.split()
        if len(words) > 1:
            unit_top_array.append(words[0])
        if len(words) > 1:
            interface_array.append(words[1])
        if len(words) > 2:
            csv_files_array.append(words[2])

#print("Top Module Name:", top_module_name)
#print("unit_top Array:", unit_top_array)
#print("interface Array:", interface_array)
#print("CSV Files Array:", csv_files_array)
module_path_array = []

for i in range(len(interface_array)):
    for key , value in split_module_dir.items():
        if key in interface_array[i]:
            module_path_array.append(value)
    #module_path_array.append(sub_module_path_array)

for row in module_path_array:
    for i in range(len(row)):
        last_dot_index = row[i].rfind('.')
        if last_dot_index != -1:
            row[i] = row[i][:last_dot_index]

#print("module_path_array:", module_path_array)



#find top's input signal
def find_file(target_dir, file_name):
    for root, dirs, files in os.walk(target_dir):
        if file_name in files:
            return os.path.join(root, file_name)
    return None

def extract_signals(file_path):
    input_pattern = re.compile(r'\binput\s+[^[]+\s+(\w+)')
    output_pattern = re.compile(r'\boutput\s+[^[]+\s+(\w+)')
    input_signals = []
    output_signals = []

    with open(file_path, 'r') as file:
        for line in file:
            input_match = input_pattern.search(line)
            output_match = output_pattern.search(line)
            if input_match:
                input_signals.append(input_match.group(1))
            elif output_match:
                output_signals.append(output_match.group(1))

    return input_signals, output_signals

target_dir = f'../../{release_rtl}/rtl'
file_name = f'{top_module_name}.sv'

file_path = find_file(target_dir, file_name)
if file_path:
    #print(f"find file: {file_path}")
    input_signals, output_signals = extract_signals(file_path)
    #print("get signal:", input_signals)
else:
    print(f"not find: {file_name}")

#find unit top module input
unit_top_input_array = []
sub_input_array = []
sub_output_array = []
for i in range(len(unit_top_array)):
    file_name = f'{unit_top_array[i]}.sv'
    file_path = find_file(target_dir, file_name)
    if file_path:
        sub_input_array, sub_output_array = extract_signals(file_path)
    else:
        print(f"not find: {file_name}")
    unit_top_input_array.append(sub_input_array)


#find path include mbistinterface

mbist_interface_array = []
with open('search.log', 'r') as file:
    file_content = file.read()
pattern = re.compile(r'dut_inst\.(.*?)}', re.DOTALL)
matches = pattern.findall(file_content)

for key in split_module_dir.keys():
    sub_mbist_interface_array = []
    for i in range(len(matches)):
        if key in matches[i]:
            sub_mbist_interface_array.append(matches[i])
        #print("sub_mbist_interface_array:", sub_mbist_interface_array)
    mbist_interface_array.append(sub_mbist_interface_array)

#print("mbist_interface_array:", mbist_interface_array)

data_array = []
sram_name_array = []
sram_type_array = []
sram_array_array = []
pipeline_depth_array = []
bitwrite_array = []
bank_addr_array = []
select0H_width_array = []
foundry_array = []
sram_inst_array = []
params_array = []
data_width_array = []
addr_width_array = []
array_width_array = []
mask_width_array = []

for i in range(len(csv_files_array)):
    csv_name = csv_files_array[i]
    filename = f"../../{release_rtl}/mbist/{csv_name}"
    data = []
    with open(filename, newline='', encoding='utf-8') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            data.append(row)
    data = data[3:]
    sram_name = []
    sram_type = []
    sram_array = []
    pipeline_depth = []
    bitwrite = []
    bank_addr = []
    select0H_width = []
    foundry = []
    sram_inst = []
    for row in data:
        sram_name.append(row[0])
        sram_type.append(row[1].rstrip('.sv'))
        sram_array.append(row[2])
        pipeline_depth.append(row[3])
        bitwrite.append(row[4])
        bank_addr.append(row[5])
        select0H_width.append(row[6])
        foundry.append(row[7])
        sram_inst.append(row[8])
    sram_array     = [int(x) for x in sram_array]
    pipeline_depth = [int(x) for x in pipeline_depth]
    select0H_width = [int(x) for x in select0H_width]
    bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in bitwrite]
    params = []
    for sram_type_str in sram_type:
        match = re.search(r'sram_array_(.*)', sram_type_str)
        if match:
            array_str = match.group(1)
            # Find the number before the first 'p'
            p_num_match = re.search(r'(\d+)p', array_str)
            p_num = int(p_num_match.group(1)) if p_num_match else None
            # Find the numbers on both sides of the first 'x'
            x_match = re.search(r'(\d+)x(\d+)', array_str)
            x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
            # Find the number between the first 'm' and 's'
            ms_match = re.search(r'm(\d+)s', array_str)
            ms_num = int(ms_match.group(1)) if ms_match else None
            # Find the number after the first 's'
            s_match = re.search(r's(\d+)', array_str)
            s_num = int(s_match.group(1)) if s_match else None
            # Find the number after the first 'h'
            h_match = re.search(r'h(\d+)', array_str)
            h_num = int(h_match.group(1)) if h_match else None
            # Find the number after the first 'l'
            l_match = re.search(r'l(\d+)', array_str)
            l_num = int(l_match.group(1)) if l_match else None
            # Check if there is a letter 'b' before the first underscore
            has_b = 'b' in sram_type_str.split('_')[0]
            # Add the results to the array
            params.append((p_num, x_nums, ms_num, s_num, h_num, l_num, has_b))
    data_width = []
    data_width = [0] * len(select0H_width)
    for i in range(len(select0H_width)):
        data_width[i] = int((params[i][1][1])/select0H_width[i])
    
    addr_width = []
    addr_width = [0] * len(select0H_width)
    for i in range(len(select0H_width)):
        addr_width[i] = int(np.log2(params[i][1][0]))
    
    array_width = []
    array_width = [0] * len(select0H_width)
    for i in range(len(select0H_width)):
        array_width[i] = int(np.log2(len(sram_array)))
    
    mask_width = []
    mask_width = [0] * len(select0H_width)
    for i in range(len(select0H_width)):
        mask_width[i] = int(params[i][1][1]/params[i][2])
    data_array.append(data)
    sram_name_array.append(sram_name)
    sram_type_array.append(sram_type)
    sram_array_array.append(sram_array)
    pipeline_depth_array.append(pipeline_depth)
    bitwrite_array.append(bitwrite)
    bank_addr_array.append(bank_addr)
    select0H_width_array.append(select0H_width)
    foundry_array.append(foundry)
    sram_inst_array.append(sram_inst)
    params_array.append(params)
    data_width_array.append(data_width)
    addr_width_array.append(addr_width)
    array_width_array.append(array_width)
    mask_width_array.append(mask_width)


write_to_search_tcl = []

for i in range(len(csv_files_array)):
    for j in range(len(sram_inst_array[i])):
        if(sram_inst_array[i][j] == "STANDARD"):
            write_to_search_tcl.append(f"search -module {sram_type_array[i][j]}\n")
        else:
            write_to_search_tcl.append(f"search -module {sram_type_array[i][j]}\n")

unique_write_to_search_tcl = list(set(write_to_search_tcl))



with open(f"search.tcl", "w", encoding="utf-8") as file:
    file.write("scope sram_test_tb\n")
    for tcl in unique_write_to_search_tcl:
        file.write(tcl)
    file.write("q\n")


with open(f"sram_test_tb.sv", "w", encoding="utf-8") as file:
    file.write(f"module sram_test_tb();\n")
    file.write(f"\n")
    file.write(f"bit clk;\n")
    file.write(f"bit reset;\n")
    file.write(f"\n")
    file.write(f"initial\n")
    file.write(f"begin\n")
    file.write(f"    clk = 1'b0;\n")
    file.write(f"    reset = 1'b1;\n")
    file.write(f"    #100;\n")
    file.write(f"    reset = 1'b0;\n")
    file.write(f"end\n")
    file.write(f"always #5 clk = ~clk;\n")
    for i in range(len(csv_files_array)):
        file.write(f"bit read_all_mode_{i};\n")
        file.write(f"bit [{max(array_width_array[i])}:0]   mbist_array_{i};\n")
        file.write(f"bit         mbist_req_{i};\n")
        file.write(f"bit         mbist_writeen_{i};\n")
        file.write(f"bit [{max(mask_width_array[i])-1}:0]   mbist_be_{i};\n")
        file.write(f"bit [{max(addr_width_array[i])}:0]  mbist_addr_{i};\n")
        file.write(f"bit [{max(data_width_array[i])-1}:0] mbist_indata_{i};\n")
        file.write(f"bit         mbist_readen_{i};\n")
        file.write(f"bit [{max(addr_width_array[i])}:0]  mbist_addr_rd_{i};\n")
        file.write(f"bit [{max(addr_width_array[i])}:0]  now_write_addr_{i};\n")
        file.write(f"bit [{max(addr_width_array[i])}:0]  read_all_check_addr_{i};\n")
        for k in range(len(module_path_array[i])):
            file.write(f"bit [{max(data_width_array[i])-1}:0] mbist_outdata_{i}_{k};\n")
        file.write(f"int         sram_lat_array_{i}[0:{max(sram_array_array[i])}];\n")
        file.write(f"string sram_name_array_{i}[0:{len(sram_array_array[i])-1}];\n")
    file.write(f"\n")

    for i in range(len(csv_files_array)):
        file.write(f"initial\n")
        file.write(f"begin\n")
        for j in range(len(sram_array_array[i])):
            file.write(f"    sram_lat_array_{i}[{j}] = {pipeline_depth_array[i][j]};\n")
        file.write("end\n")
    for i in range(len(csv_files_array)):
        file.write("initial\n")
        file.write("begin\n")
        for j in range(len(sram_array_array[i])):
            file.write(f"    sram_name_array_{i}[{j}] = \"{sram_type_array[i][j]}\";\n")
        file.write("end\n")

    for i in range(len(csv_files_array)):
        file.write(f"//for sram test driver\n")
        file.write(f"\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(reset == 1'b0);\n")
        file.write(f"    #1000;\n")
        file.write(f"    mbist_req_{i} = 1'b1;\n")
        file.write(f"    repeat('h810)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        file.write(f"\n")
        file.write(f"\n")
        #first init
        for j in range(len(sram_array_array[i])):
            file.write(f"    mbist_array_{i} = 'd{j};\n")
            file.write(f"    for(int i = 0; i < ($clog2({params_array[i][j][1][0]})+1); i++)begin\n")
            file.write(f"\n")
            file.write(f"            mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'h0}}}};\n")
            file.write(f"            if(i == 0)begin\n")
            file.write(f"                mbist_addr_rd_{i} = 'd0;\n")
            file.write(f"                mbist_addr_{i} = 'd0;\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                mbist_addr_rd_{i} = 1 << (i-1);\n")
            file.write(f"                mbist_addr_{i} = 1 << (i-1);\n")
            file.write(f"            end\n")
            file.write(f"            mbist_writeen_{i} = 1'b1;\n")
            temp_write_data = int(params_array[i][j][1][1]/params_array[i][j][2])
            file.write(f"            mbist_be_{i} = {{{temp_write_data}{{1'b1}}}};\n")
            file.write(f"            mbist_readen_{i} = 1'b0;\n")
            file.write(f"            @(posedge clk);\n")
            file.write(f"            mbist_writeen_{i} = 1'b0;\n")
            file.write(f"            @(posedge clk);\n")
            file.write(f"\n")
            file.write("    end\n")
            file.write("    @(posedge clk)\n")
            file.write("    @(posedge clk)\n")
            file.write("    @(posedge clk)\n")
        for j in range(len(sram_array_array[i])):
            file.write(f"    mbist_array_{i} = 'd{j};\n")
            file.write(f"    for(int i = 0; i < ($clog2({params_array[i][j][1][0]})+1); i++)begin\n")
            file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
            file.write(f"            if(j == 0)begin\n")
            file.write(f"                read_all_mode_{i} = 1'b0;\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                read_all_mode_{i} = 1'b1;\n")
            file.write(f"                now_write_addr_{i} = mbist_addr_rd_{i};\n")
            file.write(f"                @(posedge clk);\n")
            file.write(f"                for(int l = 0 ; l < ($clog2({params_array[i][j][1][0]})+1); l++)begin\n")
            file.write(f"                    if(l == 0)begin\n")
            file.write(f"                        mbist_addr_rd_{i} = 'd0;\n")
            file.write(f"                        mbist_addr_{i} = 'd0;\n")
            file.write(f"                    end\n")
            file.write(f"                    else begin\n")
            file.write(f"                        mbist_addr_rd_{i} = 1 << (l-1);\n")
            file.write(f"                        mbist_addr_{i} = 1 << (l-1);\n")
            file.write(f"                    end\n")
            file.write(f"                    @(posedge clk);\n")
            if(params_array[i][j][5] == 1):
                file.write(f"                    mbist_readen_{i} = 1'b1;\n")
                file.write(f"                    @(posedge clk);\n")
                file.write(f"                    mbist_readen_{i} = 1'b0;\n")
            elif(params_array[i][j][5] == 2):
                file.write(f"                    mbist_readen_{i} = 1'b1;\n")
                file.write(f"                    @(posedge clk);\n")
                file.write(f"                    mbist_readen_{i} = 1'b0;\n")
                file.write(f"                    @(posedge clk);//it has 2 lat.\n")
                file.write(f"\n")
            file.write(f"                end\n")
            file.write(f"                @(posedge clk);\n")
            file.write(f"                @(posedge clk);\n")
            file.write(f"                read_all_mode_{i} = 1'b0;\n")
            file.write(f"                @(posedge clk);\n")
            file.write(f"            end\n")
            file.write(f"\n")
            if(bitwrite_array[i][j] == 1):
                file.write(f"            for(int k = 0; k < 2; k++)begin//for mask\n")
                file.write(f"                if(j == 0)begin\n")
                file.write(f"                    mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'h5}}}};\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'ha}}}};\n")
                file.write(f"                end\n")
                file.write(f"                if(i == 0)begin\n")
                file.write(f"                    mbist_addr_rd_{i} = 'd0;\n")
                file.write(f"                    mbist_addr_{i} = 'd0;\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    mbist_addr_rd_{i} = 1 << (i-1);\n")
                file.write(f"                    mbist_addr_{i} = 1 << (i-1);\n")
                file.write(f"                end\n")
                temp_write_data = int(params_array[i][j][1][1]/params_array[i][j][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    mbist_be_{i} = 'd0;\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    mbist_be_{i} = {{{temp_write_data}{{1'b1}}}};\n")
                file.write(f"                end\n")
                file.write(f"                mbist_writeen_{i} = 1'b1;\n")
                file.write(f"                mbist_readen_{i} = 1'b0;\n")
                file.write(f"                @(posedge clk);\n")
                if(params_array[i][j][5] == 1):
                    file.write(f"                mbist_writeen_{i} = 1'b0;\n")
                    file.write(f"                mbist_readen_{i} = 1'b1;\n")
                    file.write(f"                @(posedge clk);\n")
                    file.write(f"                mbist_readen_{i} = 1'b0;\n")
                    file.write(f"            end\n")
                elif(params_array[i][j][5] == 2):
                    file.write(f"                mbist_writeen_{i} = 1'b0;//it has 2 lat.\n")
                    file.write(f"                @(posedge clk);\n")
                    file.write(f"                mbist_writeen_{i} = 1'b0;\n")
                    file.write(f"                mbist_readen_{i} = 1'b1;\n")
                    file.write(f"                @(posedge clk);\n")
                    file.write(f"                mbist_readen_{i} = 1'b0;\n")
                    file.write(f"                @(posedge clk);//it has 2 lat.\n")
                    file.write(f"            end\n")
                    file.write(f"\n")
            elif(bitwrite_array[i][j] == 0) and (params_array[i][j][5] == 1):
                file.write(f"            if(j == 0)begin\n")
                file.write(f"                mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'h5}}}};\n")
                file.write(f"            end\n")
                file.write(f"            else begin\n")
                file.write(f"                mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'ha}}}};\n")
                file.write(f"            end\n")
                file.write(f"            if(i == 0)begin\n")
                file.write(f"                mbist_addr_rd_{i} = 'd0;\n")
                file.write(f"                mbist_addr_{i} = 'd0;\n")
                file.write(f"            end\n")
                file.write(f"            else begin\n")
                file.write(f"                mbist_addr_rd_{i} = 1 << (i-1);\n")
                file.write(f"                mbist_addr_{i} = 1 << (i-1);\n")
                file.write(f"            end\n")
                file.write(f"            mbist_writeen_{i} = 1'b1;\n")
                file.write(f"            mbist_be_{i} = 'd0;//no mask\n")
                file.write(f"            mbist_readen_{i} = 1'b0;\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            mbist_writeen_{i} = 1'b0;\n")
                file.write(f"            mbist_readen_{i} = 1'b1;\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            mbist_readen_{i} = 1'b0;\n")
                file.write(f"\n")
            elif(bitwrite_array[i][j] == 0) and (params_array[i][j][5] == 2):
                file.write(f"            if(j == 0)begin\n")
                file.write(f"                mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'h5}}}};\n")
                file.write(f"            end\n")
                file.write(f"            else begin\n")
                file.write(f"                mbist_indata_{i} = {{{int(max(data_width_array[i])/4+1)}{{4'ha}}}};\n")
                file.write(f"            end\n")
                file.write(f"            if(i == 0)begin\n")
                file.write(f"                mbist_addr_rd_{i} = 'd0;\n")
                file.write(f"                mbist_addr_{i} = 'd0;\n")
                file.write(f"            end\n")
                file.write(f"            else begin\n")
                file.write(f"                mbist_addr_rd_{i} = 1 << (i-1);\n")
                file.write(f"                mbist_addr_{i} = 1 << (i-1);\n")
                file.write(f"            end\n")
                file.write(f"            mbist_be_{i} = 'd0;//no write mask\n")
                file.write(f"            mbist_writeen_{i} = 1'b1;\n")
                file.write(f"            mbist_readen_{i} = 1'b0;\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            mbist_writeen_{i} = 1'b0;//it has 2 lat.\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            mbist_writeen_{i} = 1'b0;\n")
                file.write(f"            mbist_readen_{i} = 1'b1;\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            mbist_readen_{i} = 1'b0;\n")
                file.write(f"            @(posedge clk);//it has 2 lat.\n")
            file.write("        end\n")
            file.write("    end\n")
            file.write("    @(posedge clk)\n")
            file.write(f"    for(int i = 0; i < sram_lat_array_{i}[{j}]; i++)begin\n")
            file.write("        @(posedge clk);\n")
            file.write("    end\n")
        file.write("end\n")
        file.write("\n")
    file.write(f"//ver initial\n")
    file.write(f"\n")
    file.write(f"int     error_count;\n")
    for i in range(len(csv_files_array)):
        file.write(f"bit     compare_finish_{i}[0:{len(sram_array_array[i])-1}];\n")
    file.write(f"initial\n")
    file.write(f"begin\n")
    file.write(f"    error_count = 0;\n")
    file.write(f"end\n")
    file.write(f"\n")
    for i in range(len(csv_files_array)):
        for j in range(len(sram_array_array[i])):
            for k in range(len(module_path_array[i])):
                file.write(f"bit    [{max(array_width_array[i])}:0] mbist_array_{i}_{j}_{k};\n")
                file.write(f"initial\n")
                file.write(f"begin\n")
                file.write(f"    $display(\"at time = %t ,  waiting for {unit_top_array[i]}'s SRAM at read_all_mode == 0\" , $time);\n")
                file.write(f"    wait(mbist_readen_{i} == 1'b1 && mbist_array_{i} == 'd{j} && read_all_mode_{i} == 1'b0);\n")
                file.write(f"    mbist_array_{i}_{j}_{k} = mbist_array_{i};\n")
                file.write(f"    for(int i = 0; i < sram_lat_array_{i}[{j}]; i++)begin\n")
                file.write(f"        @(posedge clk);\n")
                file.write(f"    end\n")
                if(params_array[i][j][5] == 2):
                    file.write(f"    @(posedge clk);\n")
                    file.write(f"    @(posedge clk);\n")
                file.write(f"    #1;\n")
                file.write(f"    for(int i = 0 ; i < ($clog2({params_array[i][j][1][0]})+1) ; i++)begin\n")
                file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
                if(bitwrite_array[i][j] == 0):
                    file.write(f"            if(j == 0)begin\n")
                    file.write(f"                if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                    $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                end\n")
                    file.write(f"                else begin\n")
                    file.write(f"                    error_count +=1;\n")
                    file.write(f"                    $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                    $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                    $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                end\n")
                    file.write(f"            end\n")
                    file.write(f"            else begin\n")
                    file.write(f"                $display(\"at time = %t ,  after read_all mode , waiting for {unit_top_array[i]}'s SRAM at read_all_mode == 0\" , $time);\n")
                    file.write(f"                wait(mbist_readen_{i} == 1'b1 && mbist_array_{i} == 'd{j} && read_all_mode_{i} == 1'b0);\n")
                    file.write(f"                for(int i = 0; i < sram_lat_array_{i}[{j}]; i++)begin\n")
                    file.write(f"                    @(posedge clk);\n")
                    file.write(f"                end\n")
                    #lat == 2
                    if(params_array[i][j][5] == 2):
                        file.write(f"                @(posedge clk);\n")
                        file.write(f"                @(posedge clk);\n")
                    file.write(f"                #1;\n")
                    file.write(f"                if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                    $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                end\n")
                    file.write(f"                else begin\n")
                    file.write(f"                    error_count +=1;\n")
                    file.write(f"                    $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                    $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                    $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                end\n")
                    file.write(f"            end\n")
                    file.write(f"            @(posedge clk);\n")
                    file.write(f"            @(posedge clk);\n")
                    if(params_array[i][j][5] == 2):
                        file.write(f"        @(posedge clk);\n")
                        file.write(f"        @(posedge clk);\n")
                    file.write(f"        #1;\n")
                    file.write(f"        end\n")
                    file.write(f"    end\n")
                    file.write(f"    compare_finish_{i}[{j}] = 1'b1;\n")
                    file.write(f"end\n")
                else:
                    file.write(f"            for(int k = 0 ; k < 2; k++)begin\n")
                    file.write(f"                if(j == 0)begin\n")
                    file.write(f"                    if(k == 0)begin\n")
                    file.write(f"                        if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h0}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                            $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                        else begin\n")
                    file.write(f"                            error_count += 1;\n")
                    file.write(f"                            $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                            $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                            $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h0}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                    end\n")
                    file.write(f"                    else begin\n")
                    file.write(f"                        if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                            $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                        else begin\n")
                    file.write(f"                            error_count += 1;\n")
                    file.write(f"                            $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                            $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                            $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                    end\n")
                    file.write(f"                end\n")
                    file.write(f"                else begin\n")
                    file.write(f"                    if(k == 0)begin\n")
                    file.write(f"                        $display(\"at time = %t ,  after read_all mode , waiting for {unit_top_array[i]}'s SRAM at read_all_mode == 0\" , $time);\n")
                    file.write(f"                        wait(mbist_readen_{i} == 1'b1 && mbist_array_{i} == 'd{j} && read_all_mode_{i} == 1'b0);\n")
                    file.write(f"                        for(int i = 0; i < sram_lat_array_{i}[{j}]; i++)begin\n")
                    file.write(f"                            @(posedge clk);\n")
                    file.write(f"                        end\n")
                    if(params_array[i][j][5] == 2):
                        file.write(f"                        @(posedge clk);\n")
                        file.write(f"                        @(posedge clk);\n")
                    file.write(f"                        #1;\n")
                    file.write(f"                        if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                            $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                        else begin\n")
                    file.write(f"                            error_count += 1;\n")
                    file.write(f"                            $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                            $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                            $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                    end\n")
                    file.write(f"                    else begin\n")
                    file.write(f"                        if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0])begin\n")
                    file.write(f"                            $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , simulating %s ,ver pass\" , $time , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                        else begin\n")
                    file.write(f"                            error_count += 1;\n")
                    file.write(f"                            $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , mbist_array_{i}_{j}_{k} , sram_name_array_{i}[{j}]);\n")
                    file.write(f"                            $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                    file.write(f"                            $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0]);\n")
                    file.write(f"                        end\n")
                    file.write(f"                    end\n")
                    file.write(f"                end\n")
                    file.write(f"                @(posedge clk);\n")
                    file.write(f"                @(posedge clk);\n")
                    if(params_array[i][j][5] == 2):
                        file.write(f"                @(posedge clk);\n")
                        file.write(f"                @(posedge clk);\n")
                    file.write(f"                #1;\n")
                    file.write(f"            end\n")
                    file.write(f"        end\n")
                    file.write(f"    end\n")
                    file.write(f"    compare_finish_{i}[{j}] = 1'b1;\n")
                    file.write(f"end\n")

    for i in range(len(csv_files_array)):
        for j in range(len(sram_array_array[i])):
            for k in range(len(module_path_array[i])):
                file.write(f"initial\n")
                file.write(f"begin\n")
                file.write(f"    for(int j = 0; j < ($clog2({params_array[i][j][1][0]})+1); j++)begin\n")
                file.write(f"        $display(\"at time = %t ,  waiting for {unit_top_array[i]}'s SRAM and addr is %d at read_all_mode == 1\" , $time , now_write_addr_{i});\n")
                file.write(f"        wait(mbist_readen_{i} == 1'b1 && mbist_array_{i} == 'd{j} && now_write_addr_{i} == j && read_all_mode_{i} == 1'b1);\n")
                file.write(f"        read_all_check_addr_{i} = 'd0;\n")
                file.write(f"        for(int i = 0; i < sram_lat_array_{i}[{j}]; i++)begin\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"        end\n")
                if(params_array[i][j][5] == 2):
                    file.write(f"        @(posedge clk);\n")
                #if setup multicylce == 2
                if(params_array[i][j][3] == 2):
                    file.write(f"        @(posedge clk);\n")
                file.write(f"        #1;\n")
                file.write(f"        for(int i = 0 ; i < ($clog2({params_array[i][j][1][0]})+1) ; i++)begin\n")
                file.write(f"            read_all_check_addr_{i} = i;\n")
                file.write(f"            if(read_all_check_addr_{i} == now_write_addr_{i})begin\n")
                file.write(f"                if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0])begin\n")
                file.write(f"                    $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM ,  when sram_addr is %d , simulating %s ,ver pass at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    error_count +=1;\n")
                file.write(f"                    $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when sram_addr is %d , simulating %s, ver has error at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                    $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                file.write(f"                    $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h5}}}}[{data_width_array[i][j]-1}:0]);\n")
                file.write(f"                end\n")
                file.write(f"            end\n")
                file.write(f"            else if(read_all_check_addr_{i} < now_write_addr_{i})begin\n")
                file.write(f"                if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0])begin\n")
                file.write(f"                    $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM ,  when sram_addr is %d , simulating %s ,ver pass at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    error_count +=1;\n")
                file.write(f"                    $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when sram_addr is %d , simulating %s, ver has error at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                    $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                file.write(f"                    $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'ha}}}}[{data_width_array[i][j]-1}:0]);\n")
                file.write(f"                end\n")
                file.write(f"            end\n")
                file.write(f"            else begin\n")
                file.write(f"                if(mbist_outdata_{i}_{k}[{data_width_array[i][j]-1}:0] == {{{int(data_width_array[i][j]/4+1)}{{4'h0}}}}[{data_width_array[i][j]-1}:0])begin\n")
                file.write(f"                    $display(\"at time = %t ,  for {unit_top_array[i]}'s SRAM ,  when sram_addr is %d , simulating %s ,ver pass at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                end\n")
                file.write(f"                else begin\n")
                file.write(f"                    error_count +=1;\n")
                file.write(f"                    $display(\"at time = %t , for {unit_top_array[i]}'s SRAM , when sram_addr is %d , simulating %s, ver has error at read_all_mode\" , $time , read_all_check_addr_{i} , sram_name_array_{i}[{j}]);\n")
                file.write(f"                    $display(\"for mbist_outdata is %h has error\" , mbist_outdata_{i}_{k});\n")
                file.write(f"                    $display(\"the right data is %h\" , {{{int(data_width_array[i][j]/4+1)}{{4'h0}}}}[{data_width_array[i][j]-1}:0]);\n")
                file.write(f"                end\n")
                file.write(f"            end\n")
                file.write(f"            @(posedge clk);\n")
                file.write(f"            @(posedge clk);\n")
                #if lat == 2
                if(params_array[i][j][5] == 2):
                    file.write(f"            @(posedge clk);\n")
                file.write(f"            #1;\n")
                file.write(f"        end\n")
                file.write(f"    end\n")
                file.write(f"end\n")

    file.write(f"initial\n")
    file.write(f"begin\n")
    for i in range(len(csv_files_array)):
        file.write(f"    wait(\n")
        for j in range(len(sram_array_array[i])):
            if(j == len(sram_array_array[i])-1):
                file.write(f"        compare_finish_{i}[{j}] == 1'b1);\n")
            else:
                file.write(f"        compare_finish_{i}[{j}] == 1'b1 &&\n")
    file.write(f"    if(error_count == 'd0)begin\n")
    file.write(f"        $display(\"TEST CASE PASS\");\n")
    file.write(f"    end\n")
    file.write(f"    else begin\n")
    file.write(f"        $display(\"TEST CASE FAIL\");\n")
    file.write(f"    end\n")
    file.write(f"    #100;\n")
    file.write(f"    $finish();\n")
    file.write(f"end\n")
    file.write(f"initial begin\n")
    file.write(f"    $fsdbDumpfile(\"sram_test.fsdb\");\n")
    file.write(f"    $fsdbDumpvars(0, sram_test_tb);\n")
    file.write(f"end\n")
    file.write(f"\n")
    file.write(f"{top_module_name} dut_inst(\n")
    for i in range(len(output_signals)):
        file.write(f"    .{output_signals[i]}(),\n")
    for i in range(len(input_signals)):
        if i == len(input_signals)-1:
            if "reset" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(reset)\n")
            elif "clk_on" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(1'b1)\n")
            elif "clock" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(clk)\n")
            else:
                file.write(f"    .{input_signals[i]}('d0)\n")
        else:
            if "reset" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(reset),\n")
            elif "clk_on" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(1'b1),\n")
            elif "clock" in input_signals[i]:
                file.write(f"    .{input_signals[i]}(clk),\n")
            else:
                file.write(f"    .{input_signals[i]}('d0),\n")
                
    file.write(f");\n")
    for i in range(len(csv_files_array)):
        for j in range(len(mbist_interface_array[i])):
            file.write(f"initial\n")
            file.write(f"begin\n")
            for k in range(len(unit_top_input_array[i])):
                if "clock" == unit_top_input_array[i][k]:
                    file.write(f"    force {module_path_array[i][j]}.{unit_top_input_array[i][k]} = clk;\n")
                if "reset" == unit_top_input_array[i][k]:
                    file.write(f"    force {module_path_array[i][j]}.{unit_top_input_array[i][k]} = reset;\n")
                if "clk_on" in unit_top_input_array[i][k]:
                    file.write(f"    force {module_path_array[i][j]}.{unit_top_input_array[i][k]} = 1'b1;\n")
                elif "dft" in unit_top_input_array[i][k] or "dfx" in unit_top_input_array[i][k]:
                    file.write(f"    force {module_path_array[i][j]}.{unit_top_input_array[i][k]} = 'd0;\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_array = mbist_array_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_req = mbist_req_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_writeen = mbist_writeen_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_be = mbist_be_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_addr = mbist_addr_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_indata = mbist_indata_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_readen = mbist_readen_{i};\n")
            file.write(f"    force dut_inst.{mbist_interface_array[i][j]}.mbist_addr_rd = mbist_addr_rd_{i};\n")
            file.write(f"    force mbist_outdata_{i}_{j} = dut_inst.{mbist_interface_array[i][j]}.mbist_outdata;\n")
            file.write(f"end\n")
    file.write(f"\n")
    file.write(f"\n")
    file.write(f"\n")
    file.write(f"\n")
    file.write(f"endmodule\n")

