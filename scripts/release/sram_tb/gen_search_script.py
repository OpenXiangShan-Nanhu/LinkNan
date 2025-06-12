import re
import sys
import numpy as np
import os
import glob

input_filelist = sys.argv[1]
input_top_name = sys.argv[2]
lib_filelist = ""
if(len(sys.argv) == 4):
    lib_filelist = sys.argv[3]

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
./search_module_tb.sv
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
with open(f"search_filelist.f", "w", encoding="utf-8") as file:
    if(lib_filelist != ""):
        file.write(f"-f ../../{release_rtl}/{lib_filelist}")
    file.write(flielist_content)

#gen module_list.txt
csv_files = glob.glob(os.path.join(f"../../{release_rtl}/mbist", '*.csv'))
csv_filenames = [os.path.basename(file) for file in csv_files]
#print(csv_filenames)

pattern = r"Mbist(.*?)\.csv"

interface_module_name = []
for element in csv_filenames:
    match = re.search(pattern, element)
    if match:
        interface_module_name.append(match.group(1).strip())
interface_module_name = ["bosc_MbistIntf" + element for element in interface_module_name]
#print(interface_module_name)


unit_top_module_name = []
search_directory = f"../../{release_rtl}/rtl"


for search_string in interface_module_name:
    for root, dirs, files in os.walk(search_directory):
        for file in files:
            file_path = os.path.join(root, file)
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                if search_string in content:
                    match = re.search(r"module\s*(.*?)\s*\(", content, re.DOTALL)
                    if match:
                        extracted_content = match.group(1).strip()
                        if extracted_content != search_string:
                            unit_top_module_name.append(extracted_content)
            except Exception as e:
                print(f"Error processing file {file_path}: {e}")
#print(unit_top_module_name)

with open(f"module_list.txt", "w", encoding="utf-8") as file:
    file.write(f"topModule:{input_top_name}\n")
    for i in range(len(unit_top_module_name)):
        file.write(f"{unit_top_module_name[i]} {interface_module_name[i]} {csv_filenames[i]}\n")
    file.write(f"\n")

with open('module_list.txt', 'r') as file:
    lines = file.readlines()

top_module_name = None
interface_array = []
csv_files_array = []

for line in lines:
    line = line.strip()
    if line.startswith('topModule:'):
        top_module_name = line.split(':')[1].strip()
    else:
        words = line.split()
        if len(words) > 1:
            interface_array.append(words[1])
        if len(words) > 2:
            csv_files_array.append(words[2])

#print("Top Module Name:", top_module_name)
#print("Words Array:", interface_array)
#print("CSV Files Array:", csv_files_array)


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


gen_tb = f"""
module search_module_tb();

    {top_module_name} dut_inst(
    );

endmodule
"""

with open(f"search_module_tb.sv", "w", encoding="utf-8") as file:
    file.write(gen_tb)
    file.write(f"\n")

write_to_search_tcl = []
write_to_search_tcl = [0] * len(interface_array)
for i in range(len(interface_array)):
    write_to_search_tcl[i] = f"search -module {interface_array[i]}\n"

with open(f"search.tcl", "w", encoding="utf-8") as file:
    file.write("scope search_module_tb\n")
    for tcl in write_to_search_tcl:
        file.write(tcl)
    file.write("q\n")



