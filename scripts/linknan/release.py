import re
import os
from concurrent.futures import ProcessPoolExecutor
import shutil
from datetime import date
import argparse

module_instance_re = r"(?!\bmodule\b)\b([a-zA-Z_]\w*)\b\s*(?:\#\s*\([^;]*?\))?\s*\w+\s*\("
macros_re_list = [r"\w*ClockGate$", r"\w*sram_array_[12]p\d+x\w*", r"\w*ClockManagerWrapper$"]

def is_macros(line:str):
    results = [re.match(pattern, line) for pattern in macros_re_list]
    return any(x is not None for x in results)

class ModuleParser:

    module_name:str
    submodule_set: set[str]
    ignore_module_list: list[str]
    rtl_str_dict:dict[str, str]
    export_file:str

    def __init__(self, module_name:str, rtl_pool:dict[str, str], ignore_list:list[str], export_dir:str):
        self.module_name = module_name
        self.rtl_str_dict = rtl_pool
        self.ignore_module_list = ignore_list
        self.submodule_set = set()
        self.submodule_set.add(module_name)
        self.export_dir = export_dir

    def run(self):
        self.parse(self.module_name)
        with open(f"{self.export_dir}/{self.module_name}.f", mode = "w", encoding='utf-8') as f:
            for s in self.submodule_set:
                if is_macros(s):
                    f.write(f"$release_path/macros/{s}.sv\n")
                else:
                    f.write(f"$release_path/rtl/{s}.sv\n")

    def parse(self, module:str):
        module_content = self.rtl_str_dict[module]
        matches = re.findall(module_instance_re, module_content)
        for m in matches:
            cond0 = m not in self.submodule_set
            cond1 = m in self.rtl_str_dict
            cond2 = m not in self.ignore_module_list
            if cond0 and cond1 and cond2:
                self.submodule_set.add(m)
                self.parse(m)

def parse_children(src_dir:str, dst_dir:str, harden_list:list[str]):
    rtl_pool = dict[str, str]()
    for filename in os.listdir(src_dir):
        if filename.endswith(".sv"):
            full_fn = os.path.join(src_dir, filename)
            with open(full_fn, mode = "r") as f:
                bn, _ = os.path.splitext(filename)
                rtl_pool[bn] = f.read()

    parser_list = list(map(lambda x: ModuleParser(x, rtl_pool, harden_list, dst_dir), harden_list))
    with ProcessPoolExecutor(max_workers = len(harden_list)) as executor:
        results = [executor.submit(p.run) for p in parser_list]

def release_pack(build_dir:str, release_dir, harden_list:list[str]):
    src_rtl_dir = os.path.join(build_dir, "rtl")
    dst_rtl_dir = os.path.join(release_dir, "rtl")
    dst_macro_dir = os.path.join(release_dir, "macros")

    if os.path.exists(dst_rtl_dir):
        shutil.rmtree(dst_rtl_dir)
    if os.path.exists(dst_macro_dir):
        shutil.rmtree(dst_macro_dir)
        
    os.makedirs(dst_rtl_dir)
    os.makedirs(dst_macro_dir)

    fpga_filelist = os.path.join(release_dir, "FPGA.f")
    fpga_file = open(fpga_filelist, mode = "w")

    for filename in os.listdir(build_dir):
        full_name = os.path.join(build_dir, filename)
        if os.path.isdir(full_name) and os.path.basename(filename) != "rtl" :
            shutil.copytree(full_name, os.path.join(release_dir, filename))

    for filename in os.listdir(src_rtl_dir):
        if filename.endswith(".v") or filename.endswith(".sv"):
            cp_src = os.path.join(src_rtl_dir, filename)
            cp_rtl_dst = os.path.join(dst_rtl_dir, filename)
            cp_macro_dst = os.path.join(dst_macro_dir, filename)
            bn = os.path.basename(cp_src)
            fn, _ = os.path.splitext(bn)
            if is_macros(fn):
                shutil.copy(cp_src, cp_macro_dst)
                fpga_file.write(f"$release_path/macros/{bn}\n")
            else:
                shutil.copy(cp_src, cp_rtl_dst)
                fpga_file.write(f"$release_path/rtl/{bn}\n")

    print("Doing release procedures!")
    parse_children(src_rtl_dir, release_dir, harden_list)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Post Compilation Script for XS')
    parser.add_argument('harden', type=str, nargs='*', help='harden module list for filelist generation')
    parser.add_argument('-b', '--build', type=str, default="build", help='build directory')
    parser.add_argument('-x', '--prefix', type=str, default="", help='module prefix')
    parser.add_argument('-s', '--sim', type=str, default="", help='simulation top')
    args = parser.parse_args()
    curdir = os.path.abspath(os.getcwd())

    rtl_dir = os.path.join(curdir, args.build)
    release_dir = os.path.join(curdir, f'Release-LinkNan-{date.today().strftime("%b-%d-%Y")}')

    harden_list = [args.prefix + elm for elm in args.harden]
    if args.sim != "":
        harden_list.append(args.sim)
    release_pack(rtl_dir, release_dir, harden_list)

    if args.sim != "":
        sim_fl = os.path.join(release_dir, args.sim + ".f")
        sim_dir = os.path.join(release_dir, "sim")
        if os.path.exists(sim_dir):
            shutil.rmtree(sim_dir)
        os.makedirs(sim_dir)
        with open(sim_fl, 'r') as file:
            for line in file:
                abs_src = line.replace('$release_path', release_dir).rstrip('\n')
                base_src = os.path.basename(abs_src)
                shutil.move(abs_src, os.path.join(sim_dir, base_src))
                
    mbist_dir = os.path.join(release_dir, "mbist")
    if os.path.exists(mbist_dir):
        shutil.copy(os.path.join(curdir, "scripts", "release", "sharedBusLvlibGen.tcl"), os.path.join(mbist_dir, "sharedBusLvlibGen.tcl"))


