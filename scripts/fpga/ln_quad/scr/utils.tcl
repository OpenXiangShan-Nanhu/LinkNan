proc import_filelist {mod} {
  set ln_fl [open "linknan/$mod.f" r]
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
  set_property file_type Verilog [get_files $mod.sv]
}

proc export_ip {mod ip} {
  set project_name $mod
  set abs_pwd [pwd]
  set abs_prj_path [file join [pwd] $project_name]
  if {[file exists $abs_prj_path] && [file isdirectory $abs_prj_path]} {
    file delete -force $abs_prj_path
  }

  create_project $project_name $project_name -part xcvp1902-vsva6865-2MP-e-S
  set_property simulator_language Verilog [current_project]
  import_filelist $mod

  set ip_repo $abs_pwd/ip/$ip
  if {[file exists $ip_repo] && [file isdirectory $ip_repo]} {
    file delete -force $ip_repo
  }
  ipx::package_project -root_dir $ip_repo -vendor bosc -library user -taxonomy /UserIP -import_files -set_current false
  ipx::unload_core $ip_repo/component.xml
  ipx::edit_ip_in_project -upgrade true -name tmp_edit_project -directory $ip_repo $ip_repo/component.xml
  set_property vendor bosc [ipx::current_core]
  set_property display_name $ip [ipx::current_core]
  set_property description $ip [ipx::current_core]
  set_property version 1.0 [ipx::current_core]
  set_property name $mod [ipx::current_core]
  update_compile_order -fileset sources_1
  ipx::create_xgui_files [ipx::current_core]
  ipx::update_checksums [ipx::current_core]
  ipx::check_integrity [ipx::current_core]
  ipx::save_core [ipx::current_core]
  ipx::move_temp_component_back -component [ipx::current_core]
  close_project -delete

  file delete -force $abs_prj_path
}

proc import_ip {ip} {
  set mod $ip
  append mod "_0"
  set xci [create_ip -name $ip -vendor bosc -library user -version 1.0 -module_name $mod]
  generate_target {instantiation_template} [get_files $xci]
  generate_target all [get_files $xci]
  catch { config_ip_cache -export [get_ips -all $mod] }
  export_ip_user_files -of_objects [get_files $xci] -no_script -sync -force -quiet
  create_ip_run [get_files -of_objects [get_fileset sources_1] $xci]
}