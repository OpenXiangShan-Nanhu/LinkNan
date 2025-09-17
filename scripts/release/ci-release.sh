#!/bin/bash
set -ex

# usage: ./scripts/release/ci-release.sh FPGA_Single_Core FPGA_Quad_Core ST_Single_Core ST_Quad_Core
release_args=("FPGA_Single_Core" "FPGA_Quad_Core" "ST_Single_Core" "ST_Quad_Core")

if ! { [ "$#" -ge 1 ] && [ "$#" -le 4 ] \
    && for arg in "$@"; do [[ " ${release_args[*]} " =~ " ${arg} " ]] || exit 1; done; }; then
    echo "Error: Invalid arguments. Usage: $0 [${release_args[*]}]"
    exit 1
fi

commit=$(git rev-parse --short HEAD)
linknan_dir=$(pwd)

# only ST verification needs NEMU
for arg in "$@"; do
  if [[ "$arg" == "ST_Single_Core" || "$arg" == "ST_Quad_Core" ]]; then
    temp_nemu_dir=$(pwd)/tmp_NEMU
    git clone --depth=1 -b master https://github.com/OpenXiangShan-Nanhu/NEMU.git  $temp_nemu_dir
    git clone --depth=1 -b master https://github.com/ucb-bar/berkeley-softfloat-3.git $temp_nemu_dir/resource/softfloat/repo
    git clone --depth=1 -b master https://github.com/nanopb/nanopb.git $temp_nemu_dir/resource/nanopb
    git clone --depth=1 -b master https://github.com/OpenXiangShan/LibCheckpoint.git $temp_nemu_dir/resource/LibCheckpoint
    git clone --depth=1 -b main https://github.com/OpenXiangShan/LibcheckpointAlpha.git $temp_nemu_dir/resource/gcpt_restore
    break
  fi
done

# generate releases by arguments
for arg in "$@"; do
  suffix="$arg"
  release_name="Release_$(LC_TIME=en_US.UTF-8 date +"%b_%d_%Y")_${commit}_${suffix}"
  release_dir="$linknan_dir/$release_name"
  env_dir="$release_dir/env"
  package_dir="$linknan_dir/Release-LinkNan-$(LC_TIME=en_US.UTF-8 date +"%b-%d-%Y")"
  nemu_dir="$release_dir/NEMU"
  # clean up before generate
  [ -d "$linknan_dir/build" ] && rm -rf "$linknan_dir/build"
  [ -d "$package_dir" ] && rm -rf "$package_dir"
  [ -d "$release_dir"  ] && rm -rf "$release_dir"

  # FPGA single core only need xmake soc target.
  if [ "$arg" = "FPGA_Single_Core" ]; then
    xmake soc -fgrcY -N fpga_bosc_1 -L single
    mv $package_dir $release_dir

  # FPGA quad core only need xmake soc target.
  elif [ "$arg" = "FPGA_Quad_Core" ]; then
    xmake soc -fgrcY -N fpga_bosc_4 -L quad
    mv $package_dir $release_dir
    
  # ST single core release for EDA IT verification.
  elif [ "$arg" = "ST_Single_Core" ]; then
    mkdir -p "$release_dir"
    mkdir -p "$env_dir"
    xmake soc -sgr -x bosc_ -N fpga_inno_1 -L medium
    mv "$package_dir" "${package_dir}_NOCmedium"
    mv "${package_dir}_NOCmedium" "$release_dir"

    xmake soc -sgr -x bosc_ -N fpga_inno_1 -L extreme
    mv "$package_dir" "${package_dir}_NOCextreme"
    mv "${package_dir}_NOCextreme" "$release_dir"

    xmake soc -sgr -x bosc_ -N fpga_inno_1 -C minimal -L extreme
    mv "$package_dir" "${package_dir}_NOCextreme_COREminimal"
    mv "${package_dir}_NOCextreme_COREminimal" "$release_dir"

    xmake soc -sgr -x bosc_ -N fpga_inno_1 -L extreme --fake_dram_latency
    mv "$package_dir" "${package_dir}_NOCextreme_FakeDramLatency"
    mv "${package_dir}_NOCextreme_FakeDramLatency" "$release_dir"

    cp -r "$linknan_dir"/dependencies/difftest "$env_dir"
    cp -r "$temp_nemu_dir" "$nemu_dir"

  # ST quad core release for EDA ST verification, should be replaced into multi-core version difftest and nemu.
  elif [ "$arg" = "ST_Quad_Core" ]; then
    mkdir -p "$release_dir"
    mkdir -p "$env_dir"
    xmake soc -sgr -x bosc_ -N full -L full
    cd "$package_dir"/software && dtc -I dts -O dtb -o LNSim.dtb LNSim.dts
    cd "$linknan_dir"
    mv "$package_dir"/generated-src "$package_dir"/sim "$env_dir"
    mv "$package_dir" "$release_dir"

    cp -r "$linknan_dir"/scripts/release/Makefile "$release_dir"
    cp -r "$linknan_dir"/scripts/release/sram_tb.mk "$release_dir"
    cp -r "$linknan_dir"/dependencies/difftest "$env_dir"
    cp -r "$linknan_dir"/scripts/release/sram_tb "$env_dir"
    cp -r "$temp_nemu_dir" "$nemu_dir"

    sed -i 's/riscv64-nhv5-multi-ref_defconfig/riscv64-nhv5-ref_defconfig/' "$release_dir"/Makefile
    sed -i 's/-DNUM_CORES=4//' "$release_dir"/Makefile
  fi

  # package
  cd "$linknan_dir"
  tar -zcvf $release_name.tar.gz $release_name
  # clean up generate.
  rm -rf "$release_dir" "$package_dir"
done

# clean up temporary NEMU directory.
rm -rf "$temp_nemu_dir"