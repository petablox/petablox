# LLVM instrumentation based on:
#
# american fuzzy lop - LLVM instrumentation
# -----------------------------------------
#
# Written by Laszlo Szekeres <lszekeres@google.com> and
#            Michal Zalewski <lcamtuf@google.com>
#
# LLVM integration design comes from Laszlo Szekeres.
#
# Copyright 2015, 2016 Google Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
########
CLANG_PATH   = $(HOME)/clang+llvm-3.8.1
# root directory of project
PROJ_DIR    := $(dir ${CURDIR})

BIN_PATH     = $(PROJ_DIR)bin

LLVM_CONFIG ?= $(CLANG_PATH)/bin/llvm-config

CFLAGS      ?= -O3 -funroll-loops
CFLAGS      += -Wall -D_FORTIFY_SOURCE=2 -g -Wno-pointer-sign \
               -DPASS_PATH=\"$(BIN_PATH)\" -DBIN_PATH=\"$(BIN_PATH)\" \

CXXFLAGS    ?= -O3 -funroll-loops
CXXFLAGS    += -Wall -D_FORTIFY_SOURCE=2 -g -Wno-pointer-sign \
               -DVERSION=\"$(VERSION)\" -Wno-variadic-macros

CLANG_CFL    = `$(LLVM_CONFIG) --cxxflags` -fno-rtti -fpic $(CXXFLAGS)
CLANG_LFL    = `$(LLVM_CONFIG) --ldflags` $(LDFLAGS)

# User teor2345 reports that this is required to make things work on MacOS X.

ifeq "$(shell uname)" "Darwin"
  CLANG_LFL += -Wl,-flat_namespace -Wl,-undefined,suppress
endif

# We were using llvm-config --bindir to get the location of clang, but
# this seems to be busted on some distros, so using the one in $PATH is
# probably better.

ifeq "$(origin CC)" "default"
  CC         = $(CLANG_PATH)/bin/clang
  CXX        = $(CLANG_PATH)/bin/clang++
endif

PROGS        = ../bin/vivas-clang ../bin/llvmToDatalog.so

INCLUDES	 = $(shell find ./translate/ -name "*.cpp")

all: test_deps $(PROGS)

test_deps:
	@echo "[*] Checking for working '$(CC)'..."
	@which $(CC) >/dev/null 2>&1 || ( echo "[-] Oops, can't find '$(CC)'. Make sure that it's in your \$$PATH (or set \$$CC and \$$CXX)."; exit 1 )
	@echo "[+] All set and ready to build."

../bin/vivas-clang: vivas-clang.c | test_deps
	mkdir -p ../bin
	$(CC) $(CFLAGS) $< -o $@ $(LDFLAGS)
	ln -sf $@ ../bin/vivas-clang++

../bin/llvmToDatalog.so: llvmToDatalog.cpp $(INCLUDES) | test_deps
	mkdir -p ../bin
	$(CXX) $(CLANG_CFL) -I ./ -shared $< $(INCLUDES) -o $@ $(CLANG_LFL)

.NOTPARALLEL: clean

clean:
	rm -f *.o *.so *~ a.out core core.[1-9][0-9]* test-instr .test-instr0 .test-instr1 
	rm -f $(PROGS) ../bin/vivas-clang++
