/*
   american fuzzy lop - LLVM-mode wrapper for clang
   ------------------------------------------------

   Written by Laszlo Szekeres <lszekeres@google.com> and
              Michal Zalewski <lcamtuf@google.com>

   LLVM integration design comes from Laszlo Szekeres.

   Copyright 2015, 2016 Google Inc. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at:

     http://www.apache.org/licenses/LICENSE-2.0

   This program is a drop-in replacement for clang, similar in most respects
   to ../afl-gcc. It tries to figure out compilation mode, adds a bunch
   of flags, and then calls the real compiler.

 */


#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

static char*  obj_path;               /* Path to runtime libraries         */
static char** cc_params;              /* Parameters passed to the real CC  */
static int  cc_par_cnt = 1;         /* Param count, including argv0      */

#define SAYF(x...)    fprintf(stderr, x)

#define FATAL(_x...) do { \
      fprintf(stderr, "*** [VIVAS] " _x); \
      fprintf(stderr, " ***\n"); \
      abort(); \
  } while (0)

#define alloc_printf(_str...) ({ \
    char* _tmp; \
    int _len = snprintf(NULL, 0, _str); \
    if (_len < 0) FATAL("Whoa, snprintf() fails?!"); \
    _tmp = malloc(_len + 1); \
    snprintf((char*)_tmp, _len + 1, _str); \
    _tmp; \
    })
/* Try to find the runtime libraries. If that fails, abort. */

static void find_obj(char* argv0) {

  char *tmp;

  // finds last instance of '/'
  char *slash = strrchr(argv0, '/');

  if (slash) {

    char *dir;

    *slash = 0;
    dir = strdup(argv0);
    *slash = '/';

    tmp = alloc_printf("%s/llvmToDatalog.so", PASS_PATH);

    if (!access(tmp, R_OK)) {
      obj_path = dir;
      free(tmp);
      return;

    }
    fprintf(stderr, "Unable to find %s\n", tmp);
    abort();

  }


}


/* Copy argv to cc_params, making the necessary edits. */

static void edit_params(int argc, char** argv) {


  /*char fortify_set = 0, asan_set = 0, bit_mode = 0;*/
  char x_set = 0, maybe_linking = 1;
  char *name;

  cc_params = malloc((argc + 128) * sizeof(char*));

  // TODO: figure out what this does
  name = strrchr(argv[0], '/');
  if (!name) name = argv[0]; else name++;

  if (!strcmp(name, "vivas-clang++")) {
    char* alt_cxx = alloc_printf("%s/clang+llvm-3.8.1/bin/clang++", getenv("HOME"));
    cc_params[0] = alt_cxx;
  } else {
    char* alt_cc = alloc_printf("%s/clang+llvm-3.8.1/bin/clang", getenv("HOME"));
    cc_params[0] = alt_cc;
  }

  cc_params[cc_par_cnt++] = "-fmodules";
  cc_params[cc_par_cnt++] = "-Xclang";
  cc_params[cc_par_cnt++] = "-load";
  cc_params[cc_par_cnt++] = "-Xclang";
  cc_params[cc_par_cnt++] = alloc_printf("%s/llvmToDatalog.so", PASS_PATH);

  cc_params[cc_par_cnt++] = "-Qunused-arguments";

  /* Detect stray -v calls from ./configure scripts. */

  if (argc == 1 && !strcmp(argv[1], "-v")) maybe_linking = 0;

  while (--argc) {
    char* cur = *(++argv);

    if (!strcmp(cur, "-x")) x_set = 1;

    if (!strcmp(cur, "-c") || !strcmp(cur, "-S") || !strcmp(cur, "-E"))
      maybe_linking = 0;

    if (!strcmp(cur, "-shared")) maybe_linking = 0;

    if (!strcmp(cur, "-Wl,-z,defs") ||
        !strcmp(cur, "-Wl,--no-undefined")) continue;

    cc_params[cc_par_cnt++] = cur;

  }

  if (maybe_linking) {

    if (x_set) {
      cc_params[cc_par_cnt++] = "-x";
      cc_params[cc_par_cnt++] = "none";
    }

  }

  cc_params[cc_par_cnt] = NULL;

}


/* Main entry point */

int main(int argc, char** argv) {

  if (argc < 2) {

    SAYF("\n"
         "This is a helper application for afl-fuzz. It serves as a drop-in replacement\n"
         "for clang, letting you recompile third-party code with the required runtime\n"
         "instrumentation. A common use pattern would be one of the following:\n\n"

         "  CC=%s/vivas-clang ./configure\n"
         "  CXX=%s/vivas-clang++ ./configure\n\n"

         "In contrast to the traditional afl-clang tool, this version is implemented as\n"
         "an LLVM pass and tends to offer improved performance with slow programs.\n\n"

         "You can specify custom next-stage toolchain via AFL_CC and AFL_CXX. Setting\n"
         "AFL_HARDEN enables hardening optimizations in the compiled code.\n\n",
         BIN_PATH, BIN_PATH);

    exit(1);

  }


  find_obj(argv[0]);

  edit_params(argc, argv);

  execvp(cc_params[0], (char**)cc_params);

  FATAL("Oops, failed to execute '%s' - check your PATH", cc_params[0]);

  return 0;

}
