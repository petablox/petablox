#!/usr/bin/perl

use strict;
use Getopt::Long;
use feature "switch";

######################################################################################
# Configuration of environment variables, benchmarks, analyses, and options.
#
# All things that need to be configured are in this section. They include:
# 1. Programs - benchmarks on which you want to run analyses.
# 2. Analyses - analyses you want to run on benchmarks.
# 3. Options  - system properties you want to pass to Chord. There are four levels of options:
# Higest priority options: those passed on the command line of this script, using "-D key=val" syntax.
# Second priority options: those defined in bench_options_map below. They are options specific to an (analysis, benchmark) pair.
# Third  priority options: those defined in local_options_map below. They are options specific to an analysis (but independent of the benchmark).
# Lowest priority options: those defined in global_options below. They are options independent of both the analysis and benchmark.

my $petablox_dir = &getenv("PETABLOX");
my $pjbench_dir = &getenv("PJBENCH");
my $mainBench_dir = &getenv("PAG_BENCH");


my $dacapo_dir = "dacapo/benchmarks/";
my $ashes_dir = "ashesJSuite/benchmarks/";

# Map from program name to program directory relative to $pjbench_dir
my %benchmarks = (
    "test" => "test",
    "cache4j" => "cache4j",
    "tsp" => "tsp",
    "elevator" => "elevator",
    "hedc" => "hedc",
    "weblech" => "weblech-0.0.3",
    "sor" => "sor",
    "ftp" => "ftp",
    "moldyn" => "java_grande/moldyn",
    "montecarlo" => "java_grande/montecarlo",
    "raytracer" => "java_grande/raytracer",
    "lusearch" => "$dacapo_dir/lusearch/",
    "hsqldb" => "$dacapo_dir/hsqldb/",
    "avrora" => "$dacapo_dir/avrora/",
    "antlr" => "$dacapo_dir/antlr/",
    "bloat" => "$dacapo_dir/bloat/",
    "chart" => "$dacapo_dir/chart/",
    "fop" => "$dacapo_dir/fop/",
    "luindex" => "$dacapo_dir/luindex/",
    "batik" => "$dacapo_dir/batik/",
    "pmd" => "$dacapo_dir/pmd/",
    "sunflow" => "$dacapo_dir/sunflow/",
    "xalan" => "$dacapo_dir/xalan/",
    "gj" => "$ashes_dir/gj/",
    "javasrc-p" => "$ashes_dir/javasrc-p/",
    "jpat-p" => "$ashes_dir/jpat-p/",
    "kawa-c" => "$ashes_dir/kawa-c/",
    "rhino-a" => "$ashes_dir/rhino-a/",
    "sablecc-j" => "$ashes_dir/sablecc-j/",
    "sablecc-w" => "$ashes_dir/sablecc-w/",
    "schroeder-m" => "$ashes_dir/schroeder-m/",
    "schroeder-s" => "$ashes_dir/schroeder-s/",
    "soot-c" => "$ashes_dir/soot-c/",
    "soot-j" => "$ashes_dir/soot-j/",
    "symjpack-t" => "$ashes_dir/symjpack-t/",
    "toba-s" => "$ashes_dir/toba-s/",
);
my @programs = keys %benchmarks;

my @analyses = ("thresc_hybrid", "thresc_metaback", "typestate_metaback","mustalias", "mustalias-td",  "mustalias-tdbu","mustalias-bu", "provenance-instr", "provenance-kcfa", "provenance-kobj", "provenance-typestate", "refinegen-kobj");

# Lowest priority options, but still higher than $petablox_dir/chord.properties
my @global_options = (
    "-Dchord.max.heap=8192m",
    "-Dchord.bddbddb.max.heap=4096m"
);

# Medium priority options
my %local_options_map = (
    "thresc_hybrid" =>
        [
            "-Dchord.rhs.timeout=300000",
            "-Dchord.escape.optimize=false",
            "-Dchord.escape.both=false",
            "-Dchord.print.results=true",
            "-Dchord.rhs.merge=pjoin",
            "-Dchord.ssa.kind=nophi",
            "-Dchord.reuse.scope=true",
            "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
            "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
            "-Dchord.run.analyses=cipa-0cfa-dlog,queryE,path-thresc-java,full-thresc-java"
        ],
    "thresc_metaback" =>
        [
            "-Dchord.iter-thresc-java.optimize=false",
            "-Dchord.iter-thresc-java.explode=1000",
            "-Dchord.iter-thresc-java.disjuncts=5",
            "-Dchord.iter-thresc-java.timeout=300000",
            "-Dchord.iter-thresc-java.iterlimit=100",
            "-Dchord.iter-thresc-java.xmlToHtmlTask=thresc-xml2html",
            "-Dchord.iter-thresc-java.jobpatch=100",
            "-Dchord.iter-thresc-java.negate=true",
            "-Dchord.iter-thresc-java.prune=true",
            "-Dchord.reflect.kind=dynamic",
            "-Dchord.ssa.kind=nophi",
            "-Dchord.rhs.timeout=300000",
            "-Dchord.rhs.merge=pjoin",
            "-Dchord.rhs.trace=shortest",
            "-Dchord.reuse.scope=true",
            "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
            "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
            "-Dchord.run.analyses=queryE,iter-thresc-java"
        ],
    "typestate_metaback" =>
        [
            "-Dchord.iter-typestate-java.optimize=false",
            "-Dchord.iter-typestate-java.explode=1000",
            "-Dchord.iter-typestate-java.disjuncts=5",
            "-Dchord.iter-typestate-java.timeout=300000",
            "-Dchord.iter-typestate-java.iterlimit=100",
            "-Dchord.iter-typestate-java.xmlToHtmlTask=typestate-xml2html",
            "-Dchord.iter-typestate-java.jobpatch=30",
            "-Dchord.iter-typestate-java.negate=true",
            "-Dchord.iter-typestate-java.prune=true",
            "-Dchord.reflect.kind=dynamic",
            "-Dchord.ssa.kind=nophi",
            "-Dchord.rhs.timeout=300000",
            "-Dchord.rhs.merge=pjoin",
            "-Dchord.rhs.trace=shortest",
            "-Dchord.reuse.scope=true",
            "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
            "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
            "-Dchord.run.analyses=cipa-0cfa-dlog,iter-typestate-java",
	    "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	    "-Dchord.scope.exclude=com.,sun."
        ],

    "mustalias" =>
        [
            "-Dchord.run.analyses=mustaliasoracle-java",
            "-Dchord.mustaliaslibanalysis.type=oracle",
            "-Dchord.typestate.specfile=generic_typestatespec.txt",
            "-Dchord.typestate.cipa=cipa-java",
            "-Dchord.typestate.cicg=cicg-java",
            "-Dchord.typestate.maxdepth=2",
            "-Dchord.rhs.merge=pjoin",
            "-Dchord.ssa.kind=nomovephi",

            "-Dchord.reflect.exclude=true",
            "-Dchord.reuse.scope=true",
            "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
            "-Dchord.methods.file=\${chord.work.dir}/methods.txt",

            "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
    "mustalias-td" =>
        [
            "-Dchord.run.analyses=cipa-0cfa-dlog,hybrid-mustalias-java",
	    "-Dchord.mustalias.tdlimit=-1",
	    "-Dchord.mustalias.bupelimit=1000000",
            "-Dchord.mustalias.cipa=cipa-java",
            "-Dchord.mustalias.cicg=cicg-java",
            "-Dchord.mustalias.maxdepth=2",
            "-Dchord.ssa.kind=nophi",
	    "-Dchord.mustalias.buallms=true",
	    "-Dchord.mustalias.statistics=true",
	    "-Dchord.mustalias.jumpempty=false",
	    "-Dchord.rhs.merge=pjoin",

            "-Dchord.reflect.exclude=true",
            "-Dchord.reuse.scope=false",
	    "-Dchord.reflect.kind=dynamic",
#           "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
#           "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
	    "-Dchord.scope.exclude=com.,sun.",
            "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
    "mustalias-tdbu" =>
        [
            "-Dchord.run.analyses=cipa-0cfa-dlog,hybrid-mustalias-java",
	    "-Dchord.mustalias.tdlimit=5",
	    "-Dchord.mustalias.bupelimit=1000000",
            "-Dchord.mustalias.cipa=cipa-java",
            "-Dchord.mustalias.cicg=cicg-java",
            "-Dchord.mustalias.maxdepth=2",
            "-Dchord.ssa.kind=nophi",
	    "-Dchord.mustalias.buallms=true",
	    "-Dchord.mustalias.statistics=true",
	    "-Dchord.mustalias.jumpempty=false",
	    "-Dchord.rhs.merge=pjoin",

            "-Dchord.reflect.exclude=true",
            "-Dchord.reuse.scope=false",
	    "-Dchord.reflect.kind=dynamic",
#           "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
#           "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
	    "-Dchord.scope.exclude=com.,sun.",
            "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
     "mustalias-bu" =>
        [
            "-Dchord.run.analyses=cipa-0cfa-dlog,bu-mustalias-java",
	    "-Dchord.mustalias.bupelimit=1000000",
            "-Dchord.mustalias.cipa=cipa-java",
            "-Dchord.mustalias.cicg=cicg-java",
            "-Dchord.ssa.kind=nophi",
	    "-Dchord.mustalias.buallms=true",
	    "-Dchord.mustalias.statistics=true",
	    "-Dchord.mustalias.jumpempty=false",
	    "-Dchord.bottom-up.merge=pjoin",

            "-Dchord.reflect.exclude=true",
            "-Dchord.reuse.scope=false",
	    "-Dchord.reflect.kind=dynamic",
#           "-Dchord.reflect.file=\${chord.work.dir}/reflect.txt",
#           "-Dchord.methods.file=\${chord.work.dir}/methods.txt",
	    "-Dchord.scope.exclude=com.,sun.",
            "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],   
    "provenance-instr" =>
     	[
	    "-Dchord.run.analyses=cipa-0cfa-dlog,ctxts-java,argCopy-dlog,provenance-instr",
	    "-Dchord.scope.exclude=java.,javax.,com.,sun.,org.",
	],
    "print-polysite" =>
    	[
	    "-Dchord.run.analyses=cipa-0cfa-dlog,ctxts-java,argCopy-dlog,cspa-kcfa-dlog,polysite-dlog,provenance-vis",
	    "-Dchord.provenance.out_r=polySite",
	    "-Dchord.ctxt.kind=cs",
	],
     	"provenance-kcfa" =>
     [
      	"-Dchord.run.analyses=kcfa-refiner",
	"-Dchord.scope.exclude=com,sun",
        "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dchord.provenance.cfa2=false",
	"-Dchord.provenance.query=all",
	"-Dchord.provenance.heap=true",
	"-Dchord.provenance.mono=true",
	"-Dchord.provenance.queryWeight=0",
	"-Dchord.provenance.boolDomain=true",
	"-Dchord.provenance.invkK=5",
	"-Dchord.provenance.allocK=5",
	"-Dchord.max.heap=16g",
        "-Dchord.reflect.exclude=true",
        "-Dchord.reuse.scope=false",
	"-Dchord.reflect.kind=dynamic",

     ],
     	"refinegen-kobj" =>
     [
      	"-Dchord.run.analyses=kobj-gen",
	"-Dchord.scope.exclude=com.,sun.",
        "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dchord.max.heap=16g",
        "-Dchord.reflect.exclude=true",
        "-Dchord.reuse.scope=false",
	"-Dchord.reflect.kind=dynamic",
     ],
     	"provenance-kobj" =>
     [
      	"-Dchord.run.analyses=kobj-refiner",
	"-Dchord.scope.exclude=com.,sun.",
        "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dchord.provenance.obj2=false",
	"-Dchord.provenance.query=all",
	"-Dchord.provenance.heap=true",
	"-Dchord.provenance.mono=true",
	"-Dchord.provenance.queryWeight=0",
	"-Dchord.provenance.boolDomain=true",
	"-Dchord.provenance.invkK=10",
	"-Dchord.provenance.allocK=10",
	"-Dchord.max.heap=16g",
        "-Dchord.reflect.exclude=true",
        "-Dchord.reuse.scope=false",
	"-Dchord.reflect.kind=dynamic",

     ],
  	"provenance-typestate" =>
     [
      	"-Dchord.run.analyses=cipa-0cfa-dlog,typestate-refiner",
	"-Dchord.scope.exclude=com.,sun.",
        "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dchord.provenance.query=all",
	"-Dchord.provenance.heap=true",
	"-Dchord.provenance.mono=true",
	"-Dchord.provenance.queryWeight=0",
	"-Dchord.max.heap=16g",
	"-Dchord.bddbddb.max.heap=4g",
	"-Dchord.ssa.kind=nomovephi",
        "-Dchord.reflect.exclude=true",
        "-Dchord.reuse.scope=false",
	"-Dchord.reflect.kind=dynamic",
     ],
);

# Higher priority options, but lower than @cmdline_options below, which are highest.
my %bench_options_map = (
    "thresc_metaback" =>
        {
            "elevator" => [ ]
        },
    "mustalias-tdbu" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dchord.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dchord.reflect.kind=none"
 	    ]

   	},
    "mustalias-td" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dchord.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dchord.reflect.kind=none"
 	    ]


   	},
    "mustalias-bu" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dchord.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dchord.reflect.kind=none"
 	    ]

   	},

    "typestate_metaback" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ]


   	},
    "provenance-kcfa" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ]


   	},
"refinegen-kobj" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"schroeder-m" => [
	    	"-Dchord.max.heap=64000m",
	    ],

   	},
"provenance-kobj" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "luindex" => [
		    "-Dchord.max.heap=64000m",
		    "-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "bloat" => [
	    	"-Dchord.max.heap=64000m",
	    ],
	    "chart" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "fop" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "pmd" => [
	    	"-Dchord.max.heap=64000m",
	    ],
	    "xalan" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "schroeder-m" => [
		    "-Dchord.max.heap=64000m",
	    ],

   	},
    "provenance-typestate" =>
   	{
    	    "lusearch" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dchord.max.heap=16000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dchord.max.heap=64000m",
		"-Dchord.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ]


   	},

);

######################################################################################
# Process command line arguments

my $help = 0;
my $mode;
my $chosen_program;
my $chosen_analysis;
my $master_host;
my $master_port;
my $num_workers;
my @cmdline_options;

GetOptions(
    "mode=s" => \$mode,
    "program=s" => \$chosen_program,
    "analysis=s" => \$chosen_analysis,
    "host=s" => \$master_host,
    "port=i" => \$master_port,
    "workers=i" => \$num_workers,
    "D=s" => \@cmdline_options
);

my $error = 0;

my @modes = ("master", "worker", "parallel", "serial");
if (!grep {$_ eq $mode} @modes) {
    print "ERROR: expected mode=one of: @modes\n";
    $error = 1;
}

if (!grep {$_ eq $chosen_program} @programs) {
    #print "ERROR: expected program=one of: @programs\n";
	print "WARNING: not one of the default expected program: @programs\n";
    #$error = 1;
}

if (!grep {$_ eq $chosen_analysis} @analyses) {
    print "ERROR: expected analysis=one of: @analyses\n";
    $error = 1;
}

if ($mode eq "master" || $mode eq "worker" || $mode eq "parallel") {
    if (!$master_host) {
        $master_host = "localhost";
        print "WARN: 'host' undefined, setting it to $master_host\n";
    }
    if (!$master_port) {
        $master_port = 8888;
        print "WARN: 'port' undefined, setting it to $master_port\n";
    }
}

if ($mode eq "worker" || $mode eq "parallel") {
    if ($num_workers <= 0) {
        print "ERROR: expected workers=<NUM WORKERS>\n";
        $error = 1;
    }
}

if ($error) {
    print "Usage: $0 -mode=[@modes] -program=[@programs] -analysis=[@analyses] -D key1=val1 ... -D keyN=valN\n";
    exit 1;
}

@cmdline_options = map { "-D$_" } @cmdline_options;
print "INFO: Command line system properties: @cmdline_options\n";

######################################################################################

my $petablox_jar_path = "$petablox_dir/petablox.jar";
my $local_options = $local_options_map{$chosen_analysis};
if (!$local_options) { @$local_options = (); }

my $bench_dir;

if (!grep {$_ eq $chosen_program} @programs) {
   $bench_dir = "$mainBench_dir/$chosen_program";
}else{
	$bench_dir = "$pjbench_dir/$benchmarks{$chosen_program}";
}

my $bench_options = $bench_options_map{$chosen_analysis}{$chosen_program};
if (!$bench_options) { @$bench_options = (); }
# NOTE: order of cmdline, bench, local, global options on following line is important
my @options = (@global_options, @$local_options, @$bench_options, @cmdline_options);
@options = map { s/\${chord.work.dir}/$bench_dir/; $_ } @options;
unshift (@options, "-Dchord.work.dir=$bench_dir");
given ($mode) {
    when("master") {
        &run_master(@options);
    }
    when("worker") {
        &run_worker(@options);
    }
    when("parallel") {
        &run_master(@options);
        &run_worker(@options);
    }
    when("serial") {
        &run_serial(@options);
    }
    default { die "Unknown mode: $mode\n"; }
}

######################################################################################

sub run_serial {
    my @final_options = ("-Dchord.out.dir=./chord_output_$chosen_analysis", @_);
    runcmd_in_background(@final_options);
}

sub run_master {
    my @final_options = (("-Dchord.parallel.mode=master", "-Dchord.parallel.host=$master_host", "-Dchord.parallel.port=$master_port",
        "-Dchord.out.dir=./chord_output_$chosen_analysis/Master"), @_);
    runcmd_in_background(@final_options);
}

sub run_worker {
    my @final_options = (("-Dchord.parallel.mode=worker", "-Dchord.parallel.host=$master_host", "-Dchord.parallel.port=$master_port"), @_);
    for (my $i = 1; $i <= $num_workers; $i++) {
        runcmd_in_background("-Dchord.out.dir=./chord_output_$chosen_analysis/Worker$i", @final_options);
    }
}

sub runcmd_in_background {
    my @cmd = getcmd(@_);
    my $cmd_str = join(" ", map { "\"" . $_ . "\"" } @cmd) . " &";
    print "INFO: Running command: $cmd_str\n";
    system($cmd_str);
}

sub getcmd {
    return ("nohup", "java", "-cp", $petablox_jar_path, @_, "petablox.project.Boot");
}

sub getenv {
    my $key = $_[0];
    my $val = $ENV{$key};
    if (!$val) {
        print "ERROR: Environment variable '$key' undefined.\n";
        exit 1;
    }
    return $val;
}

