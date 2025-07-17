launch_runs synth_1 -job 8
reset_runs ln_quad_ln_0_synth_1
reset_runs synth_1
wait_on_runs CpuCluster_0_synth_1

launch_runs ln_quad_ln_0_synth_1
wait_on_runs ln_quad_ln_0_synth_1

launch_runs synth_1
wait_on_runs synth_1

launch_runs impl_1 -job 8
wait_on_runs impl_1