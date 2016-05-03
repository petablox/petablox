#!/bin/bash

STAMP_DIR="$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)"

ant -f "$STAMP_DIR/build.xml" doc_tools
