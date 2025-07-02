# Create CpuCluster IP
# source scr/ip.tcl

set project_name "ln_quad"

set abs_pwd [pwd]
set abs_prj_path [file join [pwd] $project_name]
if {[file exists $abs_prj_path] && [file isdirectory $abs_prj_path]} {
  file delete -force $abs_prj_path
}

create_project $project_name $project_name -part xcvp1902-vsva6865-2MP-e-S
set_property simulator_language Verilog [current_project]

set io_xdc [file join [pwd] "constr" "ln_ioplan.xdc"]
set fp_xdc [file join [pwd] "constr" "ln_floorplan.xdc"]
set ln_xdc [file join [pwd] "constr" "ln_ooc.xdc"]
set cc_xdc [file join [pwd] "constr" "cc_ooc.xdc"]
set te_tcl [file join [pwd] "constr" "ln_timing.tcl"]
create_fileset -constrset ln_ooc_constr
create_fileset -constrset cc_ooc_constr
add_files -fileset constrs_1 -norecurse [list $io_xdc $fp_xdc]
add_files -fileset ln_ooc_constr -norecurse $ln_xdc
add_files -fileset cc_ooc_constr -norecurse $cc_xdc
set_property USED_IN_SYNTHESIS 0 [get_files $io_xdc]
set_property USED_IN_SYNTHESIS 0 [get_files $fp_xdc]
set_property USED_IN_IMPLEMENTATION 0 [get_files $ln_xdc]
set_property USED_IN_IMPLEMENTATION 0 [get_files $cc_xdc]
add_files -fileset utils_1 -norecurse [list $te_tcl]

set ln_fl [open "linknan/XlnFpgaTop.f" r]
set ln_path [pwd]
append ln_path "/linknan"

set all_files_raw [read $ln_fl]
regsub -all {\$release_path} $all_files_raw $ln_path all_files
set cleaned_list {}
foreach item [split $all_files "\n"] {
  if {$item ne ""} {
    lappend cleaned_list $item
  }
}

# Use cpu cluster IP here
set cpu_mod CpuCluster_0
set file_path "$ln_path/rtl/LNTop.sv"
set fp [open $file_path r]
set content [read $fp]
close $fp
set new_content [string map {"CpuCluster cc" "CpuCluster_0 cc"} $content]
set fp [open $file_path w]
puts $fp $new_content
close $fp

add_files -norecurse -scan_for_includes $cleaned_list
set_property file_type Verilog [get_files XlnFpgaTop.sv]
set_property ip_repo_paths $abs_pwd/nhv5-ip [current_project]
set cc_xci [create_ip -name CpuCluster -vendor bosc -library user -version 5.0 -module_name $cpu_mod]
generate_target {instantiation_template} [get_files $cc_xci]
generate_target all [get_files $cc_xci]
catch { config_ip_cache -export [get_ips -all $cpu_mod] }
export_ip_user_files -of_objects [get_files $cc_xci] -no_script -sync -force -quiet
create_ip_run [get_files -of_objects [get_fileset sources_1] $cc_xci]

create_bd_design $project_name
create_bd_cell -type module -reference XlnFpgaTop ln
save_bd_design

source scr/ddr.tcl
save_bd_design
source scr/peri.tcl
save_bd_design
source scr/top.tcl
save_bd_design

validate_bd_design
save_bd_design
close_bd_design [get_bd_designs $project_name]

if {$tcl_platform(platform) eq "windows"} {
  set_param general.maxThreads 4
  puts "Running on Windows OS, set maxThreads to 4"
} else {
  set_param general.maxThreads 8
  puts "Running on Unix-Like OS, set maxThreads to 8"
}

set wrp_name $project_name
append wrp_name "_wrapper"
set bd_file [file join $abs_prj_path "$project_name.srcs" "sources_1" "bd" $project_name "$project_name.bd"]
set bd_wrp_file [file join $abs_prj_path "$project_name.gen" "sources_1" "bd" $project_name "hdl" "$wrp_name.v"]
make_wrapper -files [get_files $bd_file] -top
add_files -norecurse $bd_wrp_file
set_property top $wrp_name [current_fileset]
update_compile_order -fileset sources_1
generate_target all [get_files $bd_file]
export_ip_user_files -of_objects [get_files $bd_file] -no_script -sync -force -quiet
create_ip_run [get_files -of_objects [get_fileset sources_1] $bd_file]

set_property strategy Flow_PerfOptimized_high [get_runs CpuCluster_0_synth_1]
set_property strategy Flow_PerfOptimized_high [get_runs ln_quad_ln_0_synth_1]
set_property STEPS.SYNTH_DESIGN.ARGS.FLATTEN_HIERARCHY none [get_runs CpuCluster_0_synth_1]
set_property STEPS.SYNTH_DESIGN.ARGS.FLATTEN_HIERARCHY none [get_runs ln_quad_ln_0_synth_1]
set_property strategy Performance_ExplorePostRoutePhysOpt [get_runs impl_1]

set_property CONSTRSET cc_ooc_constr [get_runs CpuCluster_0_synth_1]
set_property CONSTRSET ln_ooc_constr [get_runs ln_quad_ln_0_synth_1]
set_property STEPS.INIT_DESIGN.TCL.POST [get_files $te_tcl -of [get_fileset utils_1]] [get_runs impl_*]
