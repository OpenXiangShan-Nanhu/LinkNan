#!/bin/bash
set -ex

if [ $# -gt 1 ] || { [ $# -eq 1 ] && [ "$1" != "single" ]; }; then
    echo "Error: Invalid arguments. Usage: $0 [single]" >&2
    exit 1
fi

data=`date +"%Y%m%d"`
commit=`git rev-parse --short HEAD`

linknan_dir=$(pwd)
package_dir="$linknan_dir"/Release-LinkNan-$(LC_TIME=en_US.UTF-8 date +"%b-%d-%Y")

suffix=$([ "$1" = "single" ] && echo "_single" || echo "")
release_dir="$linknan_dir"/release_${data}_${commit}${suffix}
nemu_dir="$release_dir"/NEMU
am_dir="$release_dir"/nexus-am
case_dir="$release_dir"/cases
env_dir="$release_dir"/env

# 1. backup and create release dir
[ -d "$linknan_dir/build" ] && mv $linknan_dir/build "$linknan_dir"/build.bak
[ -d "$release_dir" ] && rm -rf "$release_dir"
mkdir -p "$release_dir"
mkdir -p "$env_dir"

# 2. get nexus-am and NEMU
git clone --depth=1 -b nact https://github.com/OpenXiangShan-Nanhu/nexus-am.git $am_dir
git clone --depth=1 -b master https://github.com/OpenXiangShan-Nanhu/NEMU.git  $nemu_dir
git clone --depth=1 -b master https://github.com/ucb-bar/berkeley-softfloat-3.git $nemu_dir/resource/softfloat/repo
git clone --depth=1 -b master https://github.com/nanopb/nanopb.git $nemu_dir/resource/nanopb
git clone --depth=1 -b master https://github.com/OpenXiangShan/LibCheckpoint.git $nemu_dir/resource/LibCheckpoint
git clone --depth=1 -b main https://github.com/OpenXiangShan/LibcheckpointAlpha.git $nemu_dir/resource/gcpt_restore

# 3. compile cases
mkdir -p "$case_dir"
export AM_HOME="$am_dir"
find "$am_dir/cases" -name Makefile -execdir make ARCH=riscv64-ln -j \;
find "$am_dir/cases" -name '*.bin'  -exec cp {} "$case_dir" \;

# 4. generate soc package and env
if [ "$1" = "single" ]; then
  xmake soc -sgrmzAY -x bosc_ -N fpga_1 -L medium
else
  xmake soc -sgrmzA -x bosc_
fi
cd "$package_dir"/software && dtc -I dts -O dtb -o LNSim.dtb LNSim.dts
cd "$linknan_dir"
mv "$package_dir"/generated-src "$package_dir"/sim "$env_dir"
mv "$package_dir" "$release_dir"

cp -r "$linknan_dir"/scripts/release/Makefile "$release_dir"
cp -r "$linknan_dir"/scripts/release/sram_tb.mk "$release_dir"
cp -r "$linknan_dir"/dependencies/difftest "$env_dir"
cp -r "$linknan_dir"/scripts/release/sram_tb "$env_dir"
if [ "$1" = "single" ]; then
  sed -i 's/riscv64-nhv5-multi-ref_defconfig/riscv64-nhv5-ref_defconfig/' "$release_dir"/Makefile
  sed -i 's/-DNUM_CORES=4//' "$release_dir"/Makefile
fi

# 5. tar and archive
tar -zcvf "release_${data}_${commit}${suffix}.tar.gz" "release_${data}_${commit}${suffix}"
[ -d "/nfs/share/home/nact-release" ] && cp "$release_dir.tar.gz" /nfs/share/home/nact-release
