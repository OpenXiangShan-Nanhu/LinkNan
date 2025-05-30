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

def get_release_folder(relative_path):
    entries = os.listdir(relative_path)
    release_folders = [entry for entry in entries if entry.startswith('Release') and os.path.isdir(os.path.join(relative_path, entry))]
    if len(release_folders) == 1:
        return release_folders[0]
    else:
        raise ValueError("Expected exactly one release folder, but found: {}".format(release_folders))

release_rtl = get_release_folder("../../")

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

with open(f"filelist.f", "w", encoding="utf-8") as file:
    if(lib_filelist != ""):
        file.write(f"-f ../../{release_rtl}/{lib_filelist}")
    file.write(flielist_content)


eastCSVName = "bosc_LNTop.MbistIoWrapperEast.csv"
northCSVName = "bosc_LNTop.MbistIoWrapperNorth.csv"
southCSVName = "bosc_LNTop.MbistIoWrapperSouth.csv"
westCSVName = "bosc_LNTop.MbistIoWrapperWest.csv"
nocHomeCSVName = "bosc_LNTop.MbistNocHome.csv"


filename = f"../../{release_rtl}/mbist/{eastCSVName}"
east_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        east_data.append(row)

east_data = east_data[3:]
east_sram_name = []
east_sram_type = []
east_sram_array = []
east_pipeline_depth = []
east_bitwrite = []
east_bank_addr = []
east_select0H_width = []
east_foundry = []
east_sram_inst = []

for row in east_data:
    east_sram_name.append(row[0])
    east_sram_type.append(row[1].rstrip('.sv'))
    east_sram_array.append(row[2])
    east_pipeline_depth.append(row[3])
    east_bitwrite.append(row[4])
    east_bank_addr.append(row[5])
    east_select0H_width.append(row[6])
    east_foundry.append(row[7])
    east_sram_inst.append(row[8])

east_sram_array     = [int(x) for x in east_sram_array]
east_pipeline_depth = [int(x) for x in east_pipeline_depth]
east_select0H_width = [int(x) for x in east_select0H_width]
east_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in east_bitwrite]

east_params = []

for sram_type_str in east_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        east_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        east_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        east_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        east_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        east_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        east_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        east_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        east_params.append((east_p_num, east_x_nums, east_ms_num, east_s_num, east_h_num, east_l_num, east_has_b))

east_data_width = []
east_data_width = [0] * len(east_select0H_width)
for i in range(len(east_select0H_width)):
    east_data_width[i] = int((east_params[i][1][1])/east_select0H_width[i])

east_addr_width = []
east_addr_width = [0] * len(east_select0H_width)
for i in range(len(east_select0H_width)):
    east_addr_width[i] = int(np.log2(east_params[i][1][0]))

east_array_width = []
east_array_width = [0] * len(east_select0H_width)
for i in range(len(east_select0H_width)):
    east_array_width[i] = int(np.log2(len(east_sram_array)))

east_mask_width = []
east_mask_width = [0] * len(east_select0H_width)
for i in range(len(east_select0H_width)):
    east_mask_width[i] = int(east_params[i][1][1]/east_params[i][2])

filename = f"../../{release_rtl}/mbist/{northCSVName}"
north_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        north_data.append(row)

north_data = north_data[3:]
north_sram_name = []
north_sram_type = []
north_sram_array = []
north_pipeline_depth = []
north_bitwrite = []
north_bank_addr = []
north_select0H_width = []
north_foundry = []
north_sram_inst = []

for row in north_data:
    north_sram_name.append(row[0])
    north_sram_type.append(row[1].rstrip('.sv'))
    north_sram_array.append(row[2])
    north_pipeline_depth.append(row[3])
    north_bitwrite.append(row[4])
    north_bank_addr.append(row[5])
    north_select0H_width.append(row[6])
    north_foundry.append(row[7])
    north_sram_inst.append(row[8])

north_sram_array     = [int(x) for x in north_sram_array]
north_pipeline_depth = [int(x) for x in north_pipeline_depth]
north_select0H_width = [int(x) for x in north_select0H_width]
north_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in north_bitwrite]

north_params = []

for sram_type_str in north_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        north_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        north_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        north_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        north_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        north_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        north_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        north_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        north_params.append((north_p_num, north_x_nums, north_ms_num, north_s_num, north_h_num, north_l_num, north_has_b))

north_data_width = []
north_data_width = [0] * len(north_select0H_width)
for i in range(len(north_select0H_width)):
    north_data_width[i] = int((north_params[i][1][1])/north_select0H_width[i])

north_addr_width = []
north_addr_width = [0] * len(north_select0H_width)
for i in range(len(north_select0H_width)):
    north_addr_width[i] = int(np.log2(north_params[i][1][0]))

north_array_width = []
north_array_width = [0] * len(north_select0H_width)
for i in range(len(north_select0H_width)):
    north_array_width[i] = int(np.log2(len(north_sram_array)))

north_mask_width = []
north_mask_width = [0] * len(north_select0H_width)
for i in range(len(north_select0H_width)):
    north_mask_width[i] = int(north_params[i][1][1]/north_params[i][2])

filename = f"../../{release_rtl}/mbist/{southCSVName}"
south_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        south_data.append(row)

south_data = south_data[3:]
south_sram_name = []
south_sram_type = []
south_sram_array = []
south_pipeline_depth = []
south_bitwrite = []
south_bank_addr = []
south_select0H_width = []
south_foundry = []
south_sram_inst = []

for row in south_data:
    south_sram_name.append(row[0])
    south_sram_type.append(row[1].rstrip('.sv'))
    south_sram_array.append(row[2])
    south_pipeline_depth.append(row[3])
    south_bitwrite.append(row[4])
    south_bank_addr.append(row[5])
    south_select0H_width.append(row[6])
    south_foundry.append(row[7])
    south_sram_inst.append(row[8])

south_sram_array     = [int(x) for x in south_sram_array]
south_pipeline_depth = [int(x) for x in south_pipeline_depth]
south_select0H_width = [int(x) for x in south_select0H_width]
south_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in south_bitwrite]

south_params = []

for sram_type_str in south_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        south_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        south_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        south_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        south_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        south_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        south_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        south_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        south_params.append((south_p_num, south_x_nums, south_ms_num, south_s_num, south_h_num, south_l_num, south_has_b))

south_data_width = []
south_data_width = [0] * len(south_select0H_width)
for i in range(len(south_select0H_width)):
    south_data_width[i] = int((south_params[i][1][1])/south_select0H_width[i])

south_addr_width = []
south_addr_width = [0] * len(south_select0H_width)
for i in range(len(south_select0H_width)):
    south_addr_width[i] = int(np.log2(south_params[i][1][0]))

south_array_width = []
south_array_width = [0] * len(south_select0H_width)
for i in range(len(south_select0H_width)):
    south_array_width[i] = int(np.log2(len(south_sram_array)))

south_mask_width = []
south_mask_width = [0] * len(south_select0H_width)
for i in range(len(south_select0H_width)):
    south_mask_width[i] = int(south_params[i][1][1]/south_params[i][2])

filename = f"../../{release_rtl}/mbist/{westCSVName}"
west_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        west_data.append(row)

west_data = west_data[3:]
west_sram_name = []
west_sram_type = []
west_sram_array = []
west_pipeline_depth = []
west_bitwrite = []
west_bank_addr = []
west_select0H_width = []
west_foundry = []
west_sram_inst = []

for row in west_data:
    west_sram_name.append(row[0])
    west_sram_type.append(row[1].rstrip('.sv'))
    west_sram_array.append(row[2])
    west_pipeline_depth.append(row[3])
    west_bitwrite.append(row[4])
    west_bank_addr.append(row[5])
    west_select0H_width.append(row[6])
    west_foundry.append(row[7])
    west_sram_inst.append(row[8])

west_sram_array     = [int(x) for x in west_sram_array]
west_pipeline_depth = [int(x) for x in west_pipeline_depth]
west_select0H_width = [int(x) for x in west_select0H_width]
west_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in west_bitwrite]

west_params = []

for sram_type_str in west_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        west_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        west_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        west_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        west_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        west_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        west_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        west_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        west_params.append((west_p_num, west_x_nums, west_ms_num, west_s_num, west_h_num, west_l_num, west_has_b))

west_data_width = []
west_data_width = [0] * len(west_select0H_width)
for i in range(len(west_select0H_width)):
    west_data_width[i] = int((west_params[i][1][1])/west_select0H_width[i])

west_addr_width = []
west_addr_width = [0] * len(west_select0H_width)
for i in range(len(west_select0H_width)):
    west_addr_width[i] = int(np.log2(west_params[i][1][0]))

west_array_width = []
west_array_width = [0] * len(west_select0H_width)
for i in range(len(west_select0H_width)):
    west_array_width[i] = int(np.log2(len(west_sram_array)))

west_mask_width = []
west_mask_width = [0] * len(west_select0H_width)
for i in range(len(west_select0H_width)):
    west_mask_width[i] = int(west_params[i][1][1]/west_params[i][2])

filename = f"../../{release_rtl}/mbist/{nocHomeCSVName}"
nocHome_data = []

with open(filename, newline='', encoding='utf-8') as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
        nocHome_data.append(row)

nocHome_data = nocHome_data[3:]
nocHome_sram_name = []
nocHome_sram_type = []
nocHome_sram_array = []
nocHome_pipeline_depth = []
nocHome_bitwrite = []
nocHome_bank_addr = []
nocHome_select0H_width = []
nocHome_foundry = []
nocHome_sram_inst = []

for row in nocHome_data:
    nocHome_sram_name.append(row[0])
    nocHome_sram_type.append(row[1].rstrip('.sv'))
    nocHome_sram_array.append(row[2])
    nocHome_pipeline_depth.append(row[3])
    nocHome_bitwrite.append(row[4])
    nocHome_bank_addr.append(row[5])
    nocHome_select0H_width.append(row[6])
    nocHome_foundry.append(row[7])
    nocHome_sram_inst.append(row[8])

nocHome_sram_array     = [int(x) for x in nocHome_sram_array]
nocHome_pipeline_depth = [int(x) for x in nocHome_pipeline_depth]
nocHome_select0H_width = [int(x) for x in nocHome_select0H_width]
nocHome_bitwrite = [1 if x.lower() == 'true' else 0 if x.lower() == 'false' else None for x in nocHome_bitwrite]

nocHome_params = []

for sram_type_str in nocHome_sram_type:
    match = re.search(r'sram_array_(.*)', sram_type_str)
    if match:
        array_str = match.group(1)
        # Find the number before the first 'p'
        p_num_match = re.search(r'(\d+)p', array_str)
        nocHome_p_num = int(p_num_match.group(1)) if p_num_match else None
        # Find the numbers on both sides of the first 'x'
        x_match = re.search(r'(\d+)x(\d+)', array_str)
        nocHome_x_nums = (int(x_match.group(1)), int(x_match.group(2))) if x_match else (None, None)
        # Find the number between the first 'm' and 's'
        ms_match = re.search(r'm(\d+)s', array_str)
        nocHome_ms_num = int(ms_match.group(1)) if ms_match else None
        # Find the number after the first 's'
        s_match = re.search(r's(\d+)', array_str)
        nocHome_s_num = int(s_match.group(1)) if s_match else None
        # Find the number after the first 'h'
        h_match = re.search(r'h(\d+)', array_str)
        nocHome_h_num = int(h_match.group(1)) if h_match else None
        # Find the number after the first 'l'
        l_match = re.search(r'l(\d+)', array_str)
        nocHome_l_num = int(l_match.group(1)) if l_match else None
        # Check if there is a letter 'b' before the first underscore
        nocHome_has_b = 'b' in sram_type_str.split('_')[0]
        # Add the results to the array
        nocHome_params.append((nocHome_p_num, nocHome_x_nums, nocHome_ms_num, nocHome_s_num, nocHome_h_num, nocHome_l_num, nocHome_has_b))

nocHome_data_width = []
nocHome_data_width = [0] * len(nocHome_select0H_width)
for i in range(len(nocHome_select0H_width)):
    nocHome_data_width[i] = int((nocHome_params[i][1][1])/nocHome_select0H_width[i])

nocHome_addr_width = []
nocHome_addr_width = [0] * len(nocHome_select0H_width)
for i in range(len(nocHome_select0H_width)):
    nocHome_addr_width[i] = int(np.log2(nocHome_params[i][1][0]))

nocHome_array_width = []
nocHome_array_width = [0] * len(nocHome_select0H_width)
for i in range(len(nocHome_select0H_width)):
    nocHome_array_width[i] = int(np.log2(len(nocHome_sram_array)))

nocHome_mask_width = []
nocHome_mask_width = [0] * len(nocHome_select0H_width)
for i in range(len(nocHome_select0H_width)):
    nocHome_mask_width[i] = int(nocHome_params[i][1][1]/nocHome_params[i][2])











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
#print("SRAM Inst:", nocHome_sram_inst)

#l2_unique_sram_type = list(set(l2_sram_type))
#core_unique_sram_type = list(set(core_sram_type))
#l2_unique_sram_inst = list(set(l2_sram_inst))
#core_unique_sram_inst = list(set(core_sram_inst))


write_to_search_tcl = []
write_to_search_tcl = [0] * (len(east_sram_inst) + len(north_sram_inst) + len(south_sram_inst) + len(west_sram_inst) + len(nocHome_sram_inst))
for i in range(len(east_sram_inst)):
    if(east_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i] = f"search -module {east_sram_type[i]}\n"
    else:
        write_to_search_tcl[i] = f"search -module {east_sram_inst[i]}\n"
for i in range(len(north_sram_inst)):
    if(north_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i  + len(east_sram_inst)] = f"search -module {north_sram_type[i]}\n"
    else:
        write_to_search_tcl[i + len(east_sram_inst)] = f"search -module {north_sram_inst[i]}\n"
for i in range(len(south_sram_inst)):
    if(south_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i  + len(east_sram_inst) + len(north_sram_inst)] = f"search -module {south_sram_type[i]}\n"
    else:
        write_to_search_tcl[i + len(east_sram_inst) + len(north_sram_inst)] = f"search -module {south_sram_inst[i]}\n"
for i in range(len(west_sram_inst)):
    if(west_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i  + len(east_sram_inst) + len(north_sram_inst) + len(south_sram_inst)] = f"search -module {west_sram_type[i]}\n"
    else:
        write_to_search_tcl[i + len(east_sram_inst) + len(north_sram_inst) + len(south_sram_inst)] = f"search -module {west_sram_inst[i]}\n"
for i in range(len(nocHome_sram_inst)):
    if(nocHome_sram_inst[i] == "STANDARD"):
        write_to_search_tcl[i  + len(east_sram_inst) + len(north_sram_inst) + len(south_sram_inst) + len(west_sram_inst)] = f"search -module {nocHome_sram_type[i]}\n"
    else:
        write_to_search_tcl[i + len(east_sram_inst) + len(north_sram_inst) + len(south_sram_inst)  + len(west_sram_inst)] = f"search -module {nocHome_sram_inst[i]}\n"
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

bit [{max(east_array_width)}:0]   east_mbist_array;
bit         east_mbist_req;
bit         east_mbist_writeen;
bit [{max(east_mask_width)-1}:0]   east_mbist_be;
bit [{max(east_addr_width)}:0]  east_mbist_addr;
bit [{max(east_data_width)-1}:0] east_mbist_indata;
bit         east_mbist_readen;
bit [{max(east_addr_width)}:0]  east_mbist_addr_rd;
bit [{max(east_data_width)-1}:0] east_mbist_outdata;
int         east_sram_lat_array[0:{max(east_sram_array)}];
string east_sram_name_array[0:{len(east_sram_array)-1}];

bit [{max(north_array_width)}:0]   north_mbist_array;
bit         north_mbist_req;
bit         north_mbist_writeen;
bit [{max(north_mask_width)-1}:0]   north_mbist_be;
bit [{max(north_addr_width)}:0]  north_mbist_addr;
bit [{max(north_data_width)-1}:0] north_mbist_indata;
bit         north_mbist_readen;
bit [{max(north_addr_width)}:0]  north_mbist_addr_rd;
bit [{max(north_data_width)-1}:0] north_mbist_outdata;
int         north_sram_lat_array[0:{max(north_sram_array)}];
string north_sram_name_array[0:{len(north_sram_array)-1}];

bit [{max(south_array_width)}:0]   south_mbist_array;
bit         south_mbist_req;
bit         south_mbist_writeen;
bit [{max(south_mask_width)-1}:0]   south_mbist_be;
bit [{max(south_addr_width)}:0]  south_mbist_addr;
bit [{max(south_data_width)-1}:0] south_mbist_indata;
bit         south_mbist_readen;
bit [{max(south_addr_width)}:0]  south_mbist_addr_rd;
bit [{max(south_data_width)-1}:0] south_mbist_outdata;
int         south_sram_lat_array[0:{max(south_sram_array)}];
string south_sram_name_array[0:{len(south_sram_array)-1}];

bit [{max(west_array_width)}:0]   west_mbist_array;
bit         west_mbist_req;
bit         west_mbist_writeen;
bit [{max(west_mask_width)-1}:0]   west_mbist_be;
bit [{max(west_addr_width)}:0]  west_mbist_addr;
bit [{max(west_data_width)-1}:0] west_mbist_indata;
bit         west_mbist_readen;
bit [{max(west_addr_width)}:0]  west_mbist_addr_rd;
bit [{max(west_data_width)-1}:0] west_mbist_outdata;
int         west_sram_lat_array[0:{max(west_sram_array)}];
string west_sram_name_array[0:{len(west_sram_array)-1}];

bit [{max(nocHome_array_width)}:0]   nocHome_mbist_array;
bit         nocHome_mbist_req;
bit         nocHome_mbist_writeen;
bit [{max(nocHome_mask_width)-1}:0]   nocHome_mbist_be;
bit [{max(nocHome_addr_width)}:0]  nocHome_mbist_addr;
bit [{max(nocHome_data_width)-1}:0] nocHome_mbist_indata;
bit         nocHome_mbist_readen;
bit [{max(nocHome_addr_width)}:0]  nocHome_mbist_addr_rd;
bit [{max(nocHome_data_width)-1}:0] nocHome_mbist_outdata_0;
bit [{max(nocHome_data_width)-1}:0] nocHome_mbist_outdata_1;
bit [{max(nocHome_data_width)-1}:0] nocHome_mbist_outdata_2;
bit [{max(nocHome_data_width)-1}:0] nocHome_mbist_outdata_3;
int         nocHome_sram_lat_array[0:{max(nocHome_sram_array)}];
string nocHome_sram_name_array[0:{len(nocHome_sram_array)-1}];



initial
begin
"""

dri_ini = f"""
//for sram test driver
initial
begin
    wait(reset == 1'b0);
    #1000;
    east_mbist_req = 1'b1;
    north_mbist_req = 1'b1;
    south_mbist_req = 1'b1;
    west_mbist_req = 1'b1;
    nocHome_mbist_req = 1'b1;
    repeat('h805)begin
        @(posedge clk);
    end
    @(posedge clk);
    @(posedge clk);
    @(posedge clk);
    @(posedge clk);
"""

east_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    east_mbist_addr_rd = 'd0;
                    east_mbist_addr = 'd0;
                end
                else begin
                    east_mbist_addr_rd = 1 << (i-1);
                    east_mbist_addr = 1 << (i-1);
                end
"""
east_mask_driver_last = f"""
                else begin
                    east_mbist_be = 'd0;
                end
                east_mbist_writeen = 1'b1;
                east_mbist_readen = 1'b0;
                @(posedge clk);
                east_mbist_writeen = 1'b0;
                east_mbist_readen = 1'b1;
                @(posedge clk);
                east_mbist_readen = 1'b0;
            end
"""

east_nomask_nolat_driver = f"""
            if(j == 0)begin
                east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                east_mbist_addr_rd = 'd0;
                east_mbist_addr = 'd0;
            end
            else begin
                east_mbist_addr_rd = 1 << (i-1);
                east_mbist_addr = 1 << (i-1);
            end
            east_mbist_writeen = 1'b1;
            east_mbist_be = 'd0;//no mask
            east_mbist_readen = 1'b0;
            @(posedge clk);
            east_mbist_writeen = 1'b0;
            east_mbist_readen = 1'b1;
            @(posedge clk);
            east_mbist_readen = 1'b0;
"""

east_nomask_lat2_driver = f"""
            if(j == 0)begin
                east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                east_mbist_addr_rd = 'd0;
                east_mbist_addr = 'd0;
            end
            else begin
                east_mbist_addr_rd = 1 << (i-1);
                east_mbist_addr = 1 << (i-1);
            end
            east_mbist_be = 'd0;//no write mask
            east_mbist_writeen = 1'b1;
            east_mbist_readen = 1'b0;
            @(posedge clk);
            east_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            east_mbist_writeen = 1'b0;
            east_mbist_readen = 1'b1;
            @(posedge clk);
            east_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

east_mask_lat2_driver_first = f"""
            for(int k = 0 ; k < 2; k++)begin
                if(j == 0)begin
                    east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    east_mbist_indata = {{{int(max(east_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    east_mbist_addr_rd = 'd0;
                    east_mbist_addr = 'd0;
                end
                else begin
                    east_mbist_addr_rd = 1 << (i-1);
                    east_mbist_addr = 1 << (i-1);
                end
"""

east_mask_lat2_driver_last = f"""
                else begin
                    east_mbist_be = 'd0;
                end
                east_mbist_writeen = 1'b1;
                east_mbist_readen = 1'b0;
                @(posedge clk);
                east_mbist_writeen = 1'b0;//it has 2 lat.
                @(posedge clk);
                east_mbist_writeen = 1'b0;
                east_mbist_readen = 1'b1;
                @(posedge clk);
                east_mbist_readen = 1'b0;
                @(posedge clk);//it has 2 lat.
            end
"""

north_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    north_mbist_addr_rd = 'd0;
                    north_mbist_addr = 'd0;
                end
                else begin
                    north_mbist_addr_rd = 1 << (i-1);
                    north_mbist_addr = 1 << (i-1);
                end
"""
north_mask_driver_last = f"""
                else begin
                    north_mbist_be = 'd0;
                end
                north_mbist_writeen = 1'b1;
                north_mbist_readen = 1'b0;
                @(posedge clk);
                north_mbist_writeen = 1'b0;
                north_mbist_readen = 1'b1;
                @(posedge clk);
                north_mbist_readen = 1'b0;
            end
"""

north_nomask_nolat_driver = f"""
            if(j == 0)begin
                north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                north_mbist_addr_rd = 'd0;
                north_mbist_addr = 'd0;
            end
            else begin
                north_mbist_addr_rd = 1 << (i-1);
                north_mbist_addr = 1 << (i-1);
            end
            north_mbist_writeen = 1'b1;
            north_mbist_be = 'd0;//no mask
            north_mbist_readen = 1'b0;
            @(posedge clk);
            north_mbist_writeen = 1'b0;
            north_mbist_readen = 1'b1;
            @(posedge clk);
            north_mbist_readen = 1'b0;
"""

north_nomask_lat2_driver = f"""
            if(j == 0)begin
                north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                north_mbist_addr_rd = 'd0;
                north_mbist_addr = 'd0;
            end
            else begin
                north_mbist_addr_rd = 1 << (i-1);
                north_mbist_addr = 1 << (i-1);
            end
            north_mbist_be = 'd0;//no write mask
            north_mbist_writeen = 1'b1;
            north_mbist_readen = 1'b0;
            @(posedge clk);
            north_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            north_mbist_writeen = 1'b0;
            north_mbist_readen = 1'b1;
            @(posedge clk);
            north_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

north_mask_lat2_driver_first = f"""
            for(int k = 0 ; k < 2; k++)begin
                if(j == 0)begin
                    north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    north_mbist_indata = {{{int(max(north_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    north_mbist_addr_rd = 'd0;
                    north_mbist_addr = 'd0;
                end
                else begin
                    north_mbist_addr_rd = 1 << (i-1);
                    north_mbist_addr = 1 << (i-1);
                end
"""

north_mask_lat2_driver_last = f"""
                else begin
                    north_mbist_be = 'd0;
                end
                north_mbist_writeen = 1'b1;
                north_mbist_readen = 1'b0;
                @(posedge clk);
                north_mbist_writeen = 1'b0;//it has 2 lat.
                @(posedge clk);
                north_mbist_writeen = 1'b0;
                north_mbist_readen = 1'b1;
                @(posedge clk);
                north_mbist_readen = 1'b0;
                @(posedge clk);//it has 2 lat.
            end
"""

south_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    south_mbist_addr_rd = 'd0;
                    south_mbist_addr = 'd0;
                end
                else begin
                    south_mbist_addr_rd = 1 << (i-1);
                    south_mbist_addr = 1 << (i-1);
                end
"""
south_mask_driver_last = f"""
                else begin
                    south_mbist_be = 'd0;
                end
                south_mbist_writeen = 1'b1;
                south_mbist_readen = 1'b0;
                @(posedge clk);
                south_mbist_writeen = 1'b0;
                south_mbist_readen = 1'b1;
                @(posedge clk);
                south_mbist_readen = 1'b0;
            end
"""

south_nomask_nolat_driver = f"""
            if(j == 0)begin
                south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                south_mbist_addr_rd = 'd0;
                south_mbist_addr = 'd0;
            end
            else begin
                south_mbist_addr_rd = 1 << (i-1);
                south_mbist_addr = 1 << (i-1);
            end
            south_mbist_writeen = 1'b1;
            south_mbist_be = 'd0;//no mask
            south_mbist_readen = 1'b0;
            @(posedge clk);
            south_mbist_writeen = 1'b0;
            south_mbist_readen = 1'b1;
            @(posedge clk);
            south_mbist_readen = 1'b0;
"""

south_nomask_lat2_driver = f"""
            if(j == 0)begin
                south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                south_mbist_addr_rd = 'd0;
                south_mbist_addr = 'd0;
            end
            else begin
                south_mbist_addr_rd = 1 << (i-1);
                south_mbist_addr = 1 << (i-1);
            end
            south_mbist_be = 'd0;//no write mask
            south_mbist_writeen = 1'b1;
            south_mbist_readen = 1'b0;
            @(posedge clk);
            south_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            south_mbist_writeen = 1'b0;
            south_mbist_readen = 1'b1;
            @(posedge clk);
            south_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

south_mask_lat2_driver_first = f"""
            for(int k = 0 ; k < 2; k++)begin
                if(j == 0)begin
                    south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    south_mbist_indata = {{{int(max(south_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    south_mbist_addr_rd = 'd0;
                    south_mbist_addr = 'd0;
                end
                else begin
                    south_mbist_addr_rd = 1 << (i-1);
                    south_mbist_addr = 1 << (i-1);
                end
"""

south_mask_lat2_driver_last = f"""
                else begin
                    south_mbist_be = 'd0;
                end
                south_mbist_writeen = 1'b1;
                south_mbist_readen = 1'b0;
                @(posedge clk);
                south_mbist_writeen = 1'b0;//it has 2 lat.
                @(posedge clk);
                south_mbist_writeen = 1'b0;
                south_mbist_readen = 1'b1;
                @(posedge clk);
                south_mbist_readen = 1'b0;
                @(posedge clk);//it has 2 lat.
            end
"""

west_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    west_mbist_addr_rd = 'd0;
                    west_mbist_addr = 'd0;
                end
                else begin
                    west_mbist_addr_rd = 1 << (i-1);
                    west_mbist_addr = 1 << (i-1);
                end
"""
west_mask_driver_last = f"""
                else begin
                    west_mbist_be = 'd0;
                end
                west_mbist_writeen = 1'b1;
                west_mbist_readen = 1'b0;
                @(posedge clk);
                west_mbist_writeen = 1'b0;
                west_mbist_readen = 1'b1;
                @(posedge clk);
                west_mbist_readen = 1'b0;
            end
"""

west_nomask_nolat_driver = f"""
            if(j == 0)begin
                west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                west_mbist_addr_rd = 'd0;
                west_mbist_addr = 'd0;
            end
            else begin
                west_mbist_addr_rd = 1 << (i-1);
                west_mbist_addr = 1 << (i-1);
            end
            west_mbist_writeen = 1'b1;
            west_mbist_be = 'd0;//no mask
            west_mbist_readen = 1'b0;
            @(posedge clk);
            west_mbist_writeen = 1'b0;
            west_mbist_readen = 1'b1;
            @(posedge clk);
            west_mbist_readen = 1'b0;
"""

west_nomask_lat2_driver = f"""
            if(j == 0)begin
                west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                west_mbist_addr_rd = 'd0;
                west_mbist_addr = 'd0;
            end
            else begin
                west_mbist_addr_rd = 1 << (i-1);
                west_mbist_addr = 1 << (i-1);
            end
            west_mbist_be = 'd0;//no write mask
            west_mbist_writeen = 1'b1;
            west_mbist_readen = 1'b0;
            @(posedge clk);
            west_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            west_mbist_writeen = 1'b0;
            west_mbist_readen = 1'b1;
            @(posedge clk);
            west_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

west_mask_lat2_driver_first = f"""
            for(int k = 0 ; k < 2; k++)begin
                if(j == 0)begin
                    west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    west_mbist_indata = {{{int(max(west_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    west_mbist_addr_rd = 'd0;
                    west_mbist_addr = 'd0;
                end
                else begin
                    west_mbist_addr_rd = 1 << (i-1);
                    west_mbist_addr = 1 << (i-1);
                end
"""

west_mask_lat2_driver_last = f"""
                else begin
                    west_mbist_be = 'd0;
                end
                west_mbist_writeen = 1'b1;
                west_mbist_readen = 1'b0;
                @(posedge clk);
                west_mbist_writeen = 1'b0;//it has 2 lat.
                @(posedge clk);
                west_mbist_writeen = 1'b0;
                west_mbist_readen = 1'b1;
                @(posedge clk);
                west_mbist_readen = 1'b0;
                @(posedge clk);//it has 2 lat.
            end
"""

nocHome_mask_driver_first = f"""
            for(int k = 0; k < 2; k++)begin//for mask
                if(j == 0)begin
                    nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    nocHome_mbist_addr_rd = 'd0;
                    nocHome_mbist_addr = 'd0;
                end
                else begin
                    nocHome_mbist_addr_rd = 1 << (i-1);
                    nocHome_mbist_addr = 1 << (i-1);
                end
"""
nocHome_mask_driver_last = f"""
                else begin
                    nocHome_mbist_be = 'd0;
                end
                nocHome_mbist_writeen = 1'b1;
                nocHome_mbist_readen = 1'b0;
                @(posedge clk);
                nocHome_mbist_writeen = 1'b0;
                nocHome_mbist_readen = 1'b1;
                @(posedge clk);
                nocHome_mbist_readen = 1'b0;
            end
"""

nocHome_nomask_nolat_driver = f"""
            if(j == 0)begin
                nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                nocHome_mbist_addr_rd = 'd0;
                nocHome_mbist_addr = 'd0;
            end
            else begin
                nocHome_mbist_addr_rd = 1 << (i-1);
                nocHome_mbist_addr = 1 << (i-1);
            end
            nocHome_mbist_writeen = 1'b1;
            nocHome_mbist_be = 'd0;//no mask
            nocHome_mbist_readen = 1'b0;
            @(posedge clk);
            nocHome_mbist_writeen = 1'b0;
            nocHome_mbist_readen = 1'b1;
            @(posedge clk);
            nocHome_mbist_readen = 1'b0;
"""

nocHome_nomask_lat2_driver = f"""
            if(j == 0)begin
                nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}};
            end
            else begin
                nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}};
            end
            if(i == 0)begin
                nocHome_mbist_addr_rd = 'd0;
                nocHome_mbist_addr = 'd0;
            end
            else begin
                nocHome_mbist_addr_rd = 1 << (i-1);
                nocHome_mbist_addr = 1 << (i-1);
            end
            nocHome_mbist_be = 'd0;//no write mask
            nocHome_mbist_writeen = 1'b1;
            nocHome_mbist_readen = 1'b0;
            @(posedge clk);
            nocHome_mbist_writeen = 1'b0;//it has 2 lat.
            @(posedge clk);
            nocHome_mbist_writeen = 1'b0;
            nocHome_mbist_readen = 1'b1;
            @(posedge clk);
            nocHome_mbist_readen = 1'b0;
            @(posedge clk);//it has 2 lat.
"""

nocHome_mask_lat2_driver_first = f"""
            for(int k = 0 ; k < 2; k++)begin
                if(j == 0)begin
                    nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}};
                end
                else begin
                    nocHome_mbist_indata = {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}};
                end
                if(i == 0)begin
                    nocHome_mbist_addr_rd = 'd0;
                    nocHome_mbist_addr = 'd0;
                end
                else begin
                    nocHome_mbist_addr_rd = 1 << (i-1);
                    nocHome_mbist_addr = 1 << (i-1);
                end
"""

nocHome_mask_lat2_driver_last = f"""
                else begin
                    nocHome_mbist_be = 'd0;
                end
                nocHome_mbist_writeen = 1'b1;
                nocHome_mbist_readen = 1'b0;
                @(posedge clk);
                nocHome_mbist_writeen = 1'b0;//it has 2 lat.
                @(posedge clk);
                nocHome_mbist_writeen = 1'b0;
                nocHome_mbist_readen = 1'b1;
                @(posedge clk);
                nocHome_mbist_readen = 1'b0;
                @(posedge clk);//it has 2 lat.
            end
"""









ver_init = f"""
//ver initial

int     error_count;
bit     east_compare_finish[0:{len(east_sram_array)-1}];
bit     north_compare_finish[0:{len(north_sram_array)-1}];
bit     south_compare_finish[0:{len(south_sram_array)-1}];
bit     west_compare_finish[0:{len(west_sram_array)-1}];
bit     nocHome_compare_finish[0:{len(nocHome_sram_array)-1}];
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
    force dut_inst.hnf_0.io_dfx_func_ram_hold = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_ram_bypass = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_ram_bp_clken = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_cgen = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_ram_aux_clk = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_ram_aux_ckbp = 1'b0;
    force dut_inst.hnf_0.io_dfx_func_ram_mcp_hold = 1'b0;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_array = nocHome_mbist_array;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_req = nocHome_mbist_req;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_writeen = nocHome_mbist_writeen;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_be = nocHome_mbist_be;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_addr = nocHome_mbist_addr;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_indata = nocHome_mbist_indata;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_readen = nocHome_mbist_readen;
    force dut_inst.hnf_0.mbistIntfNocHome.mbist_addr_rd = nocHome_mbist_addr_rd;
    force nocHome_mbist_outdata_0 = dut_inst.hnf_0.mbistIntfNocHome.mbist_outdata;
end

initial
begin
    force dut_inst.hnf_1.io_dfx_func_ram_hold = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_ram_bypass = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_ram_bp_clken = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_cgen = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_ram_aux_clk = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_ram_aux_ckbp = 1'b0;
    force dut_inst.hnf_1.io_dfx_func_ram_mcp_hold = 1'b0;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_array = nocHome_mbist_array;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_req = nocHome_mbist_req;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_writeen = nocHome_mbist_writeen;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_be = nocHome_mbist_be;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_addr = nocHome_mbist_addr;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_indata = nocHome_mbist_indata;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_readen = nocHome_mbist_readen;
    force dut_inst.hnf_1.mbistIntfNocHome.mbist_addr_rd = nocHome_mbist_addr_rd;
    force nocHome_mbist_outdata_1 = dut_inst.hnf_1.mbistIntfNocHome.mbist_outdata;
end

initial
begin
    force dut_inst.hnf_2.io_dfx_func_ram_hold = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_ram_bypass = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_ram_bp_clken = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_cgen = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_ram_aux_clk = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_ram_aux_ckbp = 1'b0;
    force dut_inst.hnf_2.io_dfx_func_ram_mcp_hold = 1'b0;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_array = nocHome_mbist_array;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_req = nocHome_mbist_req;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_writeen = nocHome_mbist_writeen;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_be = nocHome_mbist_be;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_addr = nocHome_mbist_addr;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_indata = nocHome_mbist_indata;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_readen = nocHome_mbist_readen;
    force dut_inst.hnf_2.mbistIntfNocHome.mbist_addr_rd = nocHome_mbist_addr_rd;
    force nocHome_mbist_outdata_2 = dut_inst.hnf_2.mbistIntfNocHome.mbist_outdata;
end

initial
begin
    force dut_inst.hnf_3.io_dfx_func_ram_hold = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_ram_bypass = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_ram_bp_clken = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_cgen = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_ram_aux_clk = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_ram_aux_ckbp = 1'b0;
    force dut_inst.hnf_3.io_dfx_func_ram_mcp_hold = 1'b0;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_array = nocHome_mbist_array;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_req = nocHome_mbist_req;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_writeen = nocHome_mbist_writeen;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_be = nocHome_mbist_be;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_addr = nocHome_mbist_addr;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_indata = nocHome_mbist_indata;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_readen = nocHome_mbist_readen;
    force dut_inst.hnf_3.mbistIntfNocHome.mbist_addr_rd = nocHome_mbist_addr_rd;
    force nocHome_mbist_outdata_3 = dut_inst.hnf_3.mbistIntfNocHome.mbist_outdata;
end

initial
begin
    //force dut_inst.iowrp_east.io_dft_reset_lgc_rst_n = 1'b0;
    force dut_inst.iowrp_east.io_dft_reset_mode = 1'b0;
    force dut_inst.iowrp_east.io_dft_reset_scan_mode = 1'b0;
    force dut_inst.iowrp_east.io_dft_func_ram_hold = 1'b0;
    force dut_inst.iowrp_east.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.iowrp_east.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.iowrp_east.io_dft_func_cgen = 1'b0;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_array = east_mbist_array;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_req = east_mbist_req;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_writeen = east_mbist_writeen;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_be = east_mbist_be;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_addr = east_mbist_addr;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_indata = east_mbist_indata;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_readen = east_mbist_readen;
    force dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_addr_rd = east_mbist_addr_rd;
    force east_mbist_outdata = dut_inst.iowrp_east.mbistIntfIoWrapperEast.mbist_outdata;
end

initial
begin
    //force dut_inst.iowrp_north.io_dft_reset_lgc_rst_n = 1'b0;
    force dut_inst.iowrp_north.io_dft_reset_mode = 1'b0;
    force dut_inst.iowrp_north.io_dft_reset_scan_mode = 1'b0;
    force dut_inst.iowrp_north.io_dft_func_ram_hold = 1'b0;
    force dut_inst.iowrp_north.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.iowrp_north.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.iowrp_north.io_dft_func_cgen = 1'b0;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_array = north_mbist_array;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_req = north_mbist_req;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_writeen = north_mbist_writeen;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_be = north_mbist_be;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_addr = north_mbist_addr;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_indata = north_mbist_indata;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_readen = north_mbist_readen;
    force dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_addr_rd = north_mbist_addr_rd;
    force north_mbist_outdata = dut_inst.iowrp_north.mbistIntfIoWrapperNorth.mbist_outdata;
end

initial
begin
    //force dut_inst.iowrp_south.io_dft_reset_lgc_rst_n = 1'b0;
    force dut_inst.iowrp_south.io_dft_reset_mode = 1'b0;
    force dut_inst.iowrp_south.io_dft_reset_scan_mode = 1'b0;
    force dut_inst.iowrp_south.io_dft_func_ram_hold = 1'b0;
    force dut_inst.iowrp_south.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.iowrp_south.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.iowrp_south.io_dft_func_cgen = 1'b0;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_array = south_mbist_array;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_req = south_mbist_req;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_writeen = south_mbist_writeen;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_be = south_mbist_be;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_addr = south_mbist_addr;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_indata = south_mbist_indata;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_readen = south_mbist_readen;
    force dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_addr_rd = south_mbist_addr_rd;
    force south_mbist_outdata = dut_inst.iowrp_south.mbistIntfIoWrapperSouth.mbist_outdata;
end

initial
begin
    //force dut_inst.iowrp_west.io_dft_reset_lgc_rst_n = 1'b0;
    force dut_inst.iowrp_west.io_dft_reset_mode = 1'b0;
    force dut_inst.iowrp_west.io_dft_reset_scan_mode = 1'b0;
    force dut_inst.iowrp_west.io_dft_func_ram_hold = 1'b0;
    force dut_inst.iowrp_west.io_dft_func_ram_bypass = 1'b0;
    force dut_inst.iowrp_west.io_dft_func_ram_bp_clken = 1'b0;
    force dut_inst.iowrp_west.io_dft_func_cgen = 1'b0;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_array = west_mbist_array;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_req = west_mbist_req;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_writeen = west_mbist_writeen;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_be = west_mbist_be;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_addr = west_mbist_addr;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_indata = west_mbist_indata;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_readen = west_mbist_readen;
    force dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_addr_rd = west_mbist_addr_rd;
    force west_mbist_outdata = dut_inst.iowrp_west.mbistIntfIoWrapperWest.mbist_outdata;
end



initial begin
    $fsdbDumpfile("sram_test_ln.fsdb");
    $fsdbDumpvars(0, sram_test_tb);
end

bosc_ZCI4X4C4P7D5M1G8 dut_inst(
    .clock(clk),
    .reset(reset)
);

endmodule
"""



with open(f"sram_test_tb.sv", "w", encoding="utf-8") as file:
    file.write(tb_init)
    for i in range(len(east_sram_array)):
        file.write(f"   east_sram_lat_array[{i}] = {east_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(north_sram_array)):
        file.write(f"   north_sram_lat_array[{i}] = {north_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(south_sram_array)):
        file.write(f"   south_sram_lat_array[{i}] = {south_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(west_sram_array)):
        file.write(f"   west_sram_lat_array[{i}] = {west_pipeline_depth[i]};\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(nocHome_sram_array)):
        file.write(f"   nocHome_sram_lat_array[{i}] = {nocHome_pipeline_depth[i]};\n")
    file.write("end\n")

    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(east_sram_array)):
        file.write(f"   east_sram_name_array[{i}] = \"{east_sram_type[i]}\";\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(north_sram_array)):
        file.write(f"   north_sram_name_array[{i}] = \"{north_sram_type[i]}\";\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(south_sram_array)):
        file.write(f"   south_sram_name_array[{i}] = \"{south_sram_type[i]}\";\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(west_sram_array)):
        file.write(f"   west_sram_name_array[{i}] = \"{west_sram_type[i]}\";\n")
    file.write("end\n")
    file.write("initial\n")
    file.write("begin\n")
    for i in range(len(nocHome_sram_array)):
        file.write(f"   nocHome_sram_name_array[{i}] = \"{nocHome_sram_type[i]}\";\n")
    file.write("end\n")

    file.write(dri_ini)
    for i in range(len(east_sram_array)):
        file.write(f"    east_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({east_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(east_bitwrite[i] == 1):
            if(east_params[i][5] == 1):
                file.write(east_mask_driver_first)
                temp_write_data = int(east_params[i][1][1]/east_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    east_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(east_mask_driver_last)
            elif(east_params[i][5] == 2):
                file.write(east_mask_lat2_driver_first)
                temp_write_data = int(east_params[i][1][1]/east_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    east_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(east_mask_lat2_driver_last)
        elif(east_bitwrite[i] == 0) and (east_params[i][5] == 1):
            file.write(east_nomask_nolat_driver)
        elif(east_bitwrite[i] == 0) and (east_params[i][5] == 2):
            file.write(east_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < east_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(dri_ini)
    for i in range(len(north_sram_array)):
        file.write(f"    north_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({north_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(north_bitwrite[i] == 1):
            if(north_params[i][5] == 1):
                file.write(north_mask_driver_first)
                temp_write_data = int(north_params[i][1][1]/north_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    north_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(north_mask_driver_last)
            elif(north_params[i][5] == 2):
                file.write(north_mask_lat2_driver_first)
                temp_write_data = int(north_params[i][1][1]/north_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    north_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(north_mask_lat2_driver_last)
        elif(north_bitwrite[i] == 0) and (north_params[i][5] == 1):
            file.write(north_nomask_nolat_driver)
        elif(north_bitwrite[i] == 0) and (north_params[i][5] == 2):
            file.write(north_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < north_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(dri_ini)
    for i in range(len(south_sram_array)):
        file.write(f"    south_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({south_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(south_bitwrite[i] == 1):
            if(south_params[i][5] == 1):
                file.write(south_mask_driver_first)
                temp_write_data = int(south_params[i][1][1]/south_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    south_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(south_mask_driver_last)
            elif(south_params[i][5] == 2):
                file.write(south_mask_lat2_driver_first)
                temp_write_data = int(south_params[i][1][1]/south_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    south_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(south_mask_lat2_driver_last)
        elif(south_bitwrite[i] == 0) and (south_params[i][5] == 1):
            file.write(south_nomask_nolat_driver)
        elif(south_bitwrite[i] == 0) and (south_params[i][5] == 2):
            file.write(south_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < south_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(dri_ini)
    for i in range(len(west_sram_array)):
        file.write(f"    west_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({west_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(west_bitwrite[i] == 1):
            if(west_params[i][5] == 1):
                file.write(west_mask_driver_first)
                temp_write_data = int(west_params[i][1][1]/west_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    west_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(west_mask_driver_last)
            elif(west_params[i][5] == 2):
                file.write(west_mask_lat2_driver_first)
                temp_write_data = int(west_params[i][1][1]/west_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    west_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(west_mask_lat2_driver_last)
        elif(west_bitwrite[i] == 0) and (west_params[i][5] == 1):
            file.write(west_nomask_nolat_driver)
        elif(west_bitwrite[i] == 0) and (west_params[i][5] == 2):
            file.write(west_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < west_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(dri_ini)
    for i in range(len(nocHome_sram_array)):
        file.write(f"    nocHome_mbist_array = 'd{i};\n")
        file.write(f"    for(int i = 0; i < ($clog2({nocHome_params[i][1][0]})+1); i++)begin\n")
        file.write("        for(int j = 0 ; j < 2; j++)begin\n")
        if(nocHome_bitwrite[i] == 1):
            if(nocHome_params[i][5] == 1):
                file.write(nocHome_mask_driver_first)
                temp_write_data = int(nocHome_params[i][1][1]/nocHome_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    nocHome_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(nocHome_mask_driver_last)
            elif(nocHome_params[i][5] == 2):
                file.write(nocHome_mask_lat2_driver_first)
                temp_write_data = int(nocHome_params[i][1][1]/nocHome_params[i][2])
                file.write(f"                if(k == 0)begin\n")
                file.write(f"                    nocHome_mbist_be = {{ {temp_write_data}{{1'b1}} }};\n")
                file.write(f"                end\n")
                file.write(nocHome_mask_lat2_driver_last)
        elif(nocHome_bitwrite[i] == 0) and (nocHome_params[i][5] == 1):
            file.write(nocHome_nomask_nolat_driver)
        elif(nocHome_bitwrite[i] == 0) and (nocHome_params[i][5] == 2):
            file.write(nocHome_nomask_lat2_driver)
        file.write("        end\n")
        file.write("    end\n")
        file.write("    @(posedge clk)\n")
        file.write(f"    for(int i = 0; i < nocHome_sram_lat_array[{i}]; i++)begin\n")
        file.write("        @(posedge clk);\n")
        file.write("    end\n")
    file.write("end\n")
    file.write("\n")

    file.write(ver_init)
    for i in range(len(east_sram_array)):
        file.write(f"bit    [{max(east_array_width)}:0] east_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(east_mbist_readen == 1'b1 && east_mbist_array == 'd{i});\n")
        file.write(f"    east_mbist_array_{i} = east_mbist_array;\n")
        file.write(f"    for(int i = 0; i < east_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(east_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({east_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(east_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t ,  for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t ,  for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(east_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    east_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t ,  for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t ,  for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'h5}}}}[{east_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t ,  for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(east_mbist_outdata[{east_data_width[i]-1}:0] == {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_east's SRAM , simulating %s ,ver pass\" , $time , east_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_east's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , east_mbist_array_{i} , east_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , east_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(east_data_width)/4+1)}{{4'ha}}}}[{east_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(east_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   east_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(north_sram_array)):
        file.write(f"bit    [{max(north_array_width)}:0] north_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(north_mbist_readen == 1'b1 && north_mbist_array == 'd{i});\n")
        file.write(f"    north_mbist_array_{i} = north_mbist_array;\n")
        file.write(f"    for(int i = 0; i < north_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(north_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({north_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(north_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(north_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    north_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'h5}}}}[{north_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(north_mbist_outdata[{north_data_width[i]-1}:0] == {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , simulating %s ,ver pass\" , $time , north_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_north's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , north_mbist_array_{i} , north_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , north_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(north_data_width)/4+1)}{{4'ha}}}}[{north_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(north_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   north_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(south_sram_array)):
        file.write(f"bit    [{max(south_array_width)}:0] south_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(south_mbist_readen == 1'b1 && south_mbist_array == 'd{i});\n")
        file.write(f"    south_mbist_array_{i} = south_mbist_array;\n")
        file.write(f"    for(int i = 0; i < south_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(south_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({south_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(south_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(south_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    south_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'h5}}}}[{south_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(south_mbist_outdata[{south_data_width[i]-1}:0] == {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , simulating %s ,ver pass\" , $time , south_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_south's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , south_mbist_array_{i} , south_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , south_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(south_data_width)/4+1)}{{4'ha}}}}[{south_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(south_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   south_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(west_sram_array)):
        file.write(f"bit    [{max(west_array_width)}:0] west_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(west_mbist_readen == 1'b1 && west_mbist_array == 'd{i});\n")
        file.write(f"    west_mbist_array_{i} = west_mbist_array;\n")
        file.write(f"    for(int i = 0; i < west_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(west_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({west_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(west_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(west_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    west_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'h5}}}}[{west_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(west_mbist_outdata[{west_data_width[i]-1}:0] == {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , simulating %s ,ver pass\" , $time , west_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for iowrp_west's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , west_mbist_array_{i} , west_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , west_mbist_outdata);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(west_data_width)/4+1)}{{4'ha}}}}[{west_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(west_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   west_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(nocHome_sram_array)):
        file.write(f"bit    [{max(nocHome_array_width)}:0] nocHome_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(nocHome_mbist_readen == 1'b1 && nocHome_mbist_array == 'd{i});\n")
        file.write(f"    nocHome_mbist_array_{i} = nocHome_mbist_array;\n")
        file.write(f"    for(int i = 0; i < nocHome_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(nocHome_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({nocHome_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(nocHome_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_0[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_0's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_0);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(nocHome_sram_array)):
        file.write(f"bit    [{max(nocHome_array_width)}:0] nocHome_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(nocHome_mbist_readen == 1'b1 && nocHome_mbist_array == 'd{i});\n")
        file.write(f"    nocHome_mbist_array_{i} = nocHome_mbist_array;\n")
        file.write(f"    for(int i = 0; i < nocHome_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(nocHome_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({nocHome_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(nocHome_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_1[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_1's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_1);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(nocHome_sram_array)):
        file.write(f"bit    [{max(nocHome_array_width)}:0] nocHome_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(nocHome_mbist_readen == 1'b1 && nocHome_mbist_array == 'd{i});\n")
        file.write(f"    nocHome_mbist_array_{i} = nocHome_mbist_array;\n")
        file.write(f"    for(int i = 0; i < nocHome_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(nocHome_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({nocHome_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(nocHome_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_2[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_2's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_2);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")

    for i in range(len(nocHome_sram_array)):
        file.write(f"bit    [{max(nocHome_array_width)}:0] nocHome_mbist_array_{i};\n")
        file.write(f"initial\n")
        file.write(f"begin\n")
        file.write(f"    wait(nocHome_mbist_readen == 1'b1 && nocHome_mbist_array == 'd{i});\n")
        file.write(f"    nocHome_mbist_array_{i} = nocHome_mbist_array;\n")
        file.write(f"    for(int i = 0; i < nocHome_sram_lat_array[{i}]; i++)begin\n")
        file.write(f"        @(posedge clk);\n")
        file.write(f"    end\n")
        if(nocHome_params[i][5] == 2):
            file.write(f"    @(posedge clk);\n")
            file.write(f"    @(posedge clk);\n")
        file.write(f"    #1;\n")
        file.write(f"    for(int i = 0 ; i < ($clog2({nocHome_params[i][1][0]})+1) ; i++)begin\n")
        file.write(f"        for(int j = 0 ; j < 2; j++)begin\n")
        if(nocHome_bitwrite[i] == 0):
            file.write(f"            if(j == 0)begin\n")
            file.write(f"            if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        else begin\n")
            file.write(f"            if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"            end\n")
            file.write(f"            else begin\n")
            file.write(f"                error_count +=1;\n")
            file.write(f"                $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"            end\n")
            file.write(f"        end\n")
            file.write(f"        @(posedge clk);\n")
            file.write(f"        @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"        @(posedge clk);\n")
                file.write(f"        @(posedge clk);\n")
            file.write(f"        #1;\n")
            file.write(f"        end\n")
            file.write(f"    end\n")
            file.write(f"    nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")
        else:
            file.write(f"        for(int k = 0 ; k < 2; k++)begin\n")
            file.write(f"           if(j == 0)begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'h5}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           else begin\n")
            file.write(f"               if(k == 0)begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"               else begin\n")
            file.write(f"                   if(nocHome_mbist_outdata_3[{nocHome_data_width[i]-1}:0] == {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_data_width[i]-1}:0])begin\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , simulating %s ,ver pass\" , $time , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                   end\n")
            file.write(f"                   else begin\n")
            file.write(f"                       error_count += 1;\n")
            file.write(f"                       $display(\"at time = %t , for hnf_3's SRAM , when mbist_array is %d , simulating %s, ver has error\" , $time , nocHome_mbist_array_{i} , nocHome_sram_name_array[{i}]);\n")
            file.write(f"                       $display(\"for mbist_outdata is %h has error\" , nocHome_mbist_outdata_3);\n")
            file.write(f"                       $display(\"the right data is %h\" , {{{int(max(nocHome_data_width)/4+1)}{{4'ha}}}}[{nocHome_params[i][1][1]-1}:0]);\n")
            file.write(f"                   end\n")
            file.write(f"               end\n")
            file.write(f"           end\n")
            file.write(f"           @(posedge clk);\n")
            file.write(f"           @(posedge clk);\n")
            if(nocHome_params[i][5] == 2):
                file.write(f"           @(posedge clk);\n")
                file.write(f"           @(posedge clk);\n")
            file.write(f"           #1;\n")
            file.write(f"       end\n")
            file.write(f"   end\n")
            file.write(f"   end\n")
            file.write(f"   nocHome_compare_finish[{i}] = 1'b1;\n")
            file.write(f"end\n")






    file.write(f"initial\n")
    file.write(f"begin\n")
    file.write(f"   wait(\n")
    for i in range(len(east_sram_array)):
        if(i == len(east_sram_array)-1):
            file.write(f"       east_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       east_compare_finish[{i}] == 1'b1 &&\n")

    file.write(f"   wait(\n")
    for i in range(len(north_sram_array)):
        if(i == len(north_sram_array)-1):
            file.write(f"       north_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       north_compare_finish[{i}] == 1'b1 &&\n")

    file.write(f"   wait(\n")
    for i in range(len(south_sram_array)):
        if(i == len(south_sram_array)-1):
            file.write(f"       south_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       south_compare_finish[{i}] == 1'b1 &&\n")

    file.write(f"   wait(\n")
    for i in range(len(west_sram_array)):
        if(i == len(west_sram_array)-1):
            file.write(f"       west_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       west_compare_finish[{i}] == 1'b1 &&\n")

    file.write(f"   wait(\n")
    for i in range(len(nocHome_sram_array)):
        if(i == len(nocHome_sram_array)-1):
            file.write(f"       nocHome_compare_finish[{i}] == 1'b1);\n")
        else:
            file.write(f"       nocHome_compare_finish[{i}] == 1'b1 &&\n")

    file.write(ver_finish)


