set project_name "cpu_die"
create_project $project_name $project_name -part xcvu19p-fsva3824-2-e
set_property simulator_language Verilog [current_project]

set core_fl [open "linknan/NanhuCoreWrapper.f" r]
set cluster_fl [open "linknan/CpuCluster.f" r]
set ln_path [pwd]
append ln_path "/linknan"

set all_files_raw [read $core_fl]
append all_files_raw [read $cluster_fl]
regsub -all {\$release_path} $all_files_raw $ln_path all_files
set cleaned_list {}
foreach item [split $all_files "\n"] {
  if {$item ne ""} {
    lappend cleaned_list $item
  }
}

add_files -norecurse -scan_for_includes $cleaned_list
set_property top CpuCluster [current_fileset]

set cwd [pwd]
append cwd "/"
append cwd $project_name
cd $cwd

proc create_ooc {ooc_top} {
  set ooc_src_dir [join [ list "cpu_die.srcs" $ooc_top "new"] "/"]
  set ooc_xdc [join [ list $ooc_src_dir [format "%s.xdc" $ooc_top] ] "/"]
  create_fileset -blockset -define_from $ooc_top $ooc_top
  file mkdir $ooc_src_dir
  close [ open $ooc_xdc w ]
  add_files -fileset $ooc_top $ooc_xdc
  set_property USED_IN {out_of_context synthesis implementation}  [get_files $ooc_xdc]
  set syn_name [format "%s_synth_1" $ooc_top]
  set_property STEPS.SYNTH_DESIGN.ARGS.GATED_CLOCK_CONVERSION on [get_runs $syn_name]
  set_property STEPS.SYNTH_DESIGN.ARGS.KEEP_EQUIVALENT_REGISTERS true [get_runs $syn_name]
}

create_ooc NanhuCoreWrapper
create_ooc ClusterSharedUnit
create_ooc AlwaysOnDomain
update_compile_order -fileset sources_1

create_ip -name aurora_64b66b -vendor xilinx.com -library ip -version 12.0 -module_name Aurora6466
set_property -dict [list CONFIG.CHANNEL_ENABLE {X0Y4 X0Y5 X0Y6 X0Y7} CONFIG.C_AURORA_LANES {4} CONFIG.interface_mode {Streaming} CONFIG.C_GT_LOC_4 {4} CONFIG.C_GT_LOC_3 {3} CONFIG.C_GT_LOC_2 {2} CONFIG.drp_mode {Native} CONFIG.SupportLevel {1}] [get_ips Aurora6466]

cd ..