#!/bin/bash
set -ex

data=`date +"%Y%m%d"`
commit=`git rev-parse --short HEAD`

linknan_dir=$(pwd)
package_dir="$linknan_dir"/Release-LinkNan-$(LC_TIME=en_US.UTF-8 date +"%b-%d-%Y")

release_dir="$linknan_dir"/release_${data}_${commit}
nemu_dir="$release_dir"/NEMU
am_dir="$release_dir"/nexus-am
case_dir="$release_dir"/cases
env_dir="$release_dir"/env

# 1. create release dir
[ -d "$release_dir" ] && rm -rf "$release_dir"
mkdir -p "$release_dir"
mkdir -p "$env_dir"

# 2. get nexus-am and NEMU
git clone https://github.com/OpenXiangShan-Nanhu/nexus-am.git -b nact $am_dir
git clone https://github.com/OpenXiangShan-Nanhu/NEMU.git $nemu_dir
git clone https://github.com/ucb-bar/berkeley-softfloat-3.git $nemu_dir/resource/softfloat/repo
git clone https://github.com/OpenXiangShan/LibcheckpointAlpha.git $nemu_dir/resource/gcpt_restore

# 3. compile cases
mkdir -p "$case_dir"
export AM_HOME="$am_dir"
find "$am_dir/cases" -name Makefile -execdir make ARCH=riscv64-ln -j \;
find "$am_dir/cases" -name '*.bin'  -exec cp {} "$case_dir" \;

# 4. generate soc package and env
xmake soc -sgrmA -x bosc_
mv "$package_dir"/generated-src "$package_dir"/sim "$env_dir"
mv "$package_dir" "$release_dir"

cp -r "$linknan_dir"/scripts/release/Makefile "$release_dir"
cp -r "$linknan_dir"/dependencies/difftest "$env_dir"

# 5. tar and archive
tar -zcvf "$release_dir.tar.gz" "$release_dir"
[ -d "/nfs/share/home/nact-release" ] && cp "$release_dir.tar.gz" /nfs/share/home/nact-release
