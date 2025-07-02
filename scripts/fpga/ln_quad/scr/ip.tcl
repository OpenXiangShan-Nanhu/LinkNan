set project_name "nh5_ip_prj"

set abs_pwd [pwd]
set abs_prj_path [file join [pwd] $project_name]
if {[file exists $abs_prj_path] && [file isdirectory $abs_prj_path]} {
  file delete -force $abs_prj_path
}

create_project $project_name $project_name -part xcvp1902-vsva6865-2MP-e-S
set_property simulator_language Verilog [current_project]

set ln_fl [open "linknan/CpuCluster.f" r]
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

add_files -norecurse -scan_for_includes $cleaned_list
set_property file_type Verilog [get_files CpuCluster.sv]

set ip_repo $abs_pwd/nhv5-ip
if {[file exists $ip_repo] && [file isdirectory $ip_repo]} {
  file delete -force $ip_repo
}
ipx::package_project -root_dir $ip_repo -vendor xilinx.com -library user -taxonomy /UserIP -import_files -set_current false
ipx::unload_core $ip_repo/component.xml
ipx::edit_ip_in_project -upgrade true -name tmp_edit_project -directory $ip_repo $ip_repo/component.xml
set_property vendor bosc [ipx::current_core]
set_property display_name nanhu_v5 [ipx::current_core]
set_property description nanhu_v5 [ipx::current_core]
set_property version 5.0 [ipx::current_core]
set_property name CpuCluster [ipx::current_core]
update_compile_order -fileset sources_1
ipx::create_xgui_files [ipx::current_core]
ipx::update_checksums [ipx::current_core]
ipx::check_integrity [ipx::current_core]
ipx::save_core [ipx::current_core]
ipx::move_temp_component_back -component [ipx::current_core]
close_project -delete

file delete -force $abs_prj_path