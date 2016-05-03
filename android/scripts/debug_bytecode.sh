#!/bin/bash

STAMP_DIR="$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)"

ARG_DESCRIPTION="class"
source "$STAMP_DIR/scripts/check_invocation.sh"

ant -f "$STAMP_DIR/build.xml" -Ddebug.class="$ARG" debug_bytecode
