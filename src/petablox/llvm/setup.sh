#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}"  )" && pwd  )"
LLVM_CONFIG=~/clang+llvm-3.9.0/share/llvm/cmake
cd $DIR

# the following are $os part of the url http://llvm.org/releases/3.9.0/$os
# in order to download clang release binaries for your $os
aarch64=clang+llvm-3.9.0-aarch64-linux-gnu.tar.xz
armv7alinux=clang+llvm-3.9.0-armv7a-linux-gnueabihf.tar.xz
armv7alinux_vivid=clang+llvm-3.9.0-linux-armhf-vivid.tar.xz
debian8=clang+llvm-3.9.0-x86_64-linux-gnu-debian8.tar.xz
fedora22=clang+llvm-3.9.0-x86_64-fedora22.tar.xz
fedora23i686=clang+llvm-3.9.0-i686-fedora23.tar.xz
opensuse13_2_x64=clang+llvm-3.9.0-x86_64-opensuse13.2.tar.xz
opensuse13_2_i586=clang+llvm-3.9.0-i586-opensuse13.2.tar.xz
ubuntu_14_04=clang+llvm-3.9.0-x86_64-linux-gnu-ubuntu-14.04.tar.xz
ubuntu_16_04=clang+llvm-3.9.0-x86_64-linux-gnu-ubuntu-16.04.tar.xz
suse_linux_enterprise_server_11sp3=clang+llvm-3.9.0-linux-x86_64-sles11.3.tar.xz

# uncomment for whichever os you are on - note that there are no
# prebuilt binaries for mac os X or freeBSD :(
#os=$aarch64
#os=$armv7alinux
#os=$armv7alinux_vivid
#os=$debian8
#os=$fedora22
#os=$fedora23i686
#os=$opensuse13_2_x64
#os=$opensuse13_2_i586
#os=$ubuntu_16_04
os=$ubuntu_14_04
#os=$suse_linux_enterprise_server_11sp3

build () {
    mkdir bin
    cd $DIR/src
    make
    echo "[+] LLVM Pass built"
    echo "[+] vivas-clang built"
    cd $DIR
}

get_llvm () {
    pushd $HOME
    echo "[+] Downloading LLVM/Clang"
    wget http://llvm.org/releases/3.9.0/$os
    tar -xvf $os 
    mv "${os%.tar.xz}" "clang+llvm-3.9.0"
    echo "[+] LLVM/Clang downloaded"
    popd
    # should now be in project root dir
}

if [ ! -d "$HOME/clang+llvm-3.9.0" ]; then
    get_llvm
fi
### now compile and build SkeletonPass.so and fdanalyzer-clang
echo "[+] Compiling VIVAS"
build
echo "[!] Setup complete"
