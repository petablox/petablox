#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}"  )" && pwd  )"
BUILD_DIR=$DIR/src/build

# If using a make system, include IS_MAKE=1 
if [[ $IS_MAKE == "1" ]]; then
    make
# Otherwise, use clang as normal
else
    $DIR/bin/vivas-clang $1
    rm -rf a.out
fi
