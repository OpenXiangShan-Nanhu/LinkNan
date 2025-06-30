s/\$fatal/assert(1'b0)/g
s/assign ([a-zA-Z0-9_]+) = .* \? (.*) : [0-9]+'bx;/assign \1 = \2;/g
s/_LOG_MODULE_PATH_/%m/g
s/(^\s*)(reg\s+(\[[^]]+\]\s+)?(ridx_gray|widx_gray)\b\s*[;=])/\1(* dont_touch = "yes" *)\n\1\2/
/FILE "firrtl_black_box_resource_files\.f"/,$d
/^  .*DummyDPICWrapper/i\`ifndef SYNTHESIS
/^  .*DummyDPICWrapper/{:L0; N; /;/!b L0; s/;/;\n`endif/ };
/^  .*DelayReg(_[0-9]*)? difftest/i\`ifndef SYNTHESIS
/^  .*DelayReg(_[0-9]*)? difftest/{:L1; N; /;/!b L1; s/;/;\n`endif/ };
/^  .*TrafficBoard[a-zA-Z0-9_]+ cosim_.*/i\`ifndef SYNTHESIS
/^  .*TrafficBoard[a-zA-Z0-9_]+ cosim_.*/{:L1; N; /;/!b L1; s/;/;\n`endif/ };
/^ *[a-zA-Z0-9_]*:$/i\`ifndef SYNTHESIS
/^ *[a-zA-Z0-9_]*:$/{:L1; N; /;/!b L1; s/;/;\n`endif/};