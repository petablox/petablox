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
my $mainBench_dir = &getenv("PETABLOX_BENCH");


my $dacapo_dir = "dacapo/benchmarks/";
my $ashes_dir = "ashesJSuite/benchmarks/";

# Map from program name to program directory relative to $mainBench_dir
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

my @analyses = ("thresc_hybrid", "thresc_metaback", "typestate_metaback","mustalias", "mustalias-td",  "mustalias-tdbu","mustalias-bu", "provenance-instr", "provenance-kcfa", "provenance-kobj", "provenance-typestate", "refinegen-kobj", "0cfa", "cg", "union");

# Lowest priority options, but still higher than $petablox_dir/petablox.properties
my @global_options = (
    "-Dpetablox.max.heap=8192m",
    "-Dpetablox.bddbddb.max.heap=4096m"
);

# Medium priority options
my %local_options_map = (
    "0cfa" =>
        [
            "-Dpetablox.reflect.kind=dynamic",
            "-Dpetablox.run.analyses=cipa-0cfa-dlog",
            "-Dpetablox.scope.kind=rta",
            "-Dpetablox.datalog.engine=bddbddb",
            #"-Dpetablox.datalog.engine=logicblox4",
            #"-Dpetablox.logicblox.workspace=LBexpt1",
            #"-Dpetablox.logicblox.work.dir=/home/sulekha/exptDir1",
            #"-Dpetablox.multipgm.mode=populate",
        ],
    "cg" =>
        [
            "-Dpetablox.reflect.kind=dynamic",
            "-Dpetablox.run.analyses=cg-java",
            "-Dpetablox.scope.kind=rta",
            "-Dpetablox.datalog.engine=bddbddb",
        ],
    "union" =>
        [
            "-Dpetablox.reflect.kind=dynamic",
            "-Dpetablox.run.analyses=union-java",
            "-Dpetablox.scope.kind=rta",
            "-Dpetablox.datalog.engine=bddbddb",
        ],
    "thresc_hybrid" =>
        [
            "-Dpetablox.rhs.timeout=300000",
            "-Dpetablox.escape.optimize=false",
            "-Dpetablox.escape.both=false",
            "-Dpetablox.print.results=true",
            "-Dpetablox.rhs.merge=pjoin",
            "-Dpetablox.ssa.kind=nophi",
            "-Dpetablox.reuse.scope=true",
            "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
            "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
            "-Dpetablox.run.analyses=cipa-0cfa-dlog,queryE,path-thresc-java,full-thresc-java"
        ],
    "thresc_metaback" =>
        [
            "-Dpetablox.iter-thresc-java.optimize=false",
            "-Dpetablox.iter-thresc-java.explode=1000",
            "-Dpetablox.iter-thresc-java.disjuncts=5",
            "-Dpetablox.iter-thresc-java.timeout=300000",
            "-Dpetablox.iter-thresc-java.iterlimit=100",
            "-Dpetablox.iter-thresc-java.xmlToHtmlTask=thresc-xml2html",
            "-Dpetablox.iter-thresc-java.jobpatch=100",
            "-Dpetablox.iter-thresc-java.negate=true",
            "-Dpetablox.iter-thresc-java.prune=true",
            "-Dpetablox.reflect.kind=dynamic",
            "-Dpetablox.ssa.kind=nophi",
            "-Dpetablox.rhs.timeout=300000",
            "-Dpetablox.rhs.merge=pjoin",
            "-Dpetablox.rhs.trace=shortest",
            "-Dpetablox.reuse.scope=true",
            "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
            "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
            "-Dpetablox.run.analyses=queryE,iter-thresc-java"
        ],
    "typestate_metaback" =>
        [
            "-Dpetablox.iter-typestate-java.optimize=false",
            "-Dpetablox.iter-typestate-java.explode=1000",
            "-Dpetablox.iter-typestate-java.disjuncts=5",
            "-Dpetablox.iter-typestate-java.timeout=300000",
            "-Dpetablox.iter-typestate-java.iterlimit=100",
            "-Dpetablox.iter-typestate-java.xmlToHtmlTask=typestate-xml2html",
            "-Dpetablox.iter-typestate-java.jobpatch=30",
            "-Dpetablox.iter-typestate-java.negate=true",
            "-Dpetablox.iter-typestate-java.prune=true",
            "-Dpetablox.reflect.kind=dynamic",
            "-Dpetablox.ssa.kind=nophi",
            "-Dpetablox.rhs.timeout=300000",
            "-Dpetablox.rhs.merge=pjoin",
            "-Dpetablox.rhs.trace=shortest",
            "-Dpetablox.reuse.scope=true",
            "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
            "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
            "-Dpetablox.run.analyses=cipa-0cfa-dlog,iter-typestate-java",
	    "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	    "-Dpetablox.scope.exclude=com.,sun."
        ],

    "mustalias" =>
        [
            "-Dpetablox.run.analyses=mustaliasoracle-java",
            "-Dpetablox.mustaliaslibanalysis.type=oracle",
            "-Dpetablox.typestate.specfile=generic_typestatespec.txt",
            "-Dpetablox.typestate.cipa=cipa-java",
            "-Dpetablox.typestate.cicg=cicg-java",
            "-Dpetablox.typestate.maxdepth=2",
            "-Dpetablox.rhs.merge=pjoin",
            "-Dpetablox.ssa.kind=nomovephi",

            "-Dpetablox.reflect.exclude=true",
            "-Dpetablox.reuse.scope=true",
            "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
            "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",

            "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
    "mustalias-td" =>
        [
            "-Dpetablox.run.analyses=cipa-0cfa-dlog,hybrid-mustalias-java",
	    "-Dpetablox.mustalias.tdlimit=-1",
	    "-Dpetablox.mustalias.bupelimit=1000000",
            "-Dpetablox.mustalias.cipa=cipa-java",
            "-Dpetablox.mustalias.cicg=cicg-java",
            "-Dpetablox.mustalias.maxdepth=2",
            "-Dpetablox.ssa.kind=nophi",
	    "-Dpetablox.mustalias.buallms=true",
	    "-Dpetablox.mustalias.statistics=true",
	    "-Dpetablox.mustalias.jumpempty=false",
	    "-Dpetablox.rhs.merge=pjoin",

            "-Dpetablox.reflect.exclude=true",
            "-Dpetablox.reuse.scope=false",
	    "-Dpetablox.reflect.kind=dynamic",
#           "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
#           "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
	    "-Dpetablox.scope.exclude=com.,sun.",
            "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
    "mustalias-tdbu" =>
        [
            "-Dpetablox.run.analyses=cipa-0cfa-dlog,hybrid-mustalias-java",
	    "-Dpetablox.mustalias.tdlimit=5",
	    "-Dpetablox.mustalias.bupelimit=1000000",
            "-Dpetablox.mustalias.cipa=cipa-java",
            "-Dpetablox.mustalias.cicg=cicg-java",
            "-Dpetablox.mustalias.maxdepth=2",
            "-Dpetablox.ssa.kind=nophi",
	    "-Dpetablox.mustalias.buallms=true",
	    "-Dpetablox.mustalias.statistics=true",
	    "-Dpetablox.mustalias.jumpempty=false",
	    "-Dpetablox.rhs.merge=pjoin",

            "-Dpetablox.reflect.exclude=true",
            "-Dpetablox.reuse.scope=false",
	    "-Dpetablox.reflect.kind=dynamic",
#           "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
#           "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
	    "-Dpetablox.scope.exclude=com.,sun.",
            "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],
     "mustalias-bu" =>
        [
            "-Dpetablox.run.analyses=cipa-0cfa-dlog,bu-mustalias-java",
	    "-Dpetablox.mustalias.bupelimit=1000000",
            "-Dpetablox.mustalias.cipa=cipa-java",
            "-Dpetablox.mustalias.cicg=cicg-java",
            "-Dpetablox.ssa.kind=nophi",
	    "-Dpetablox.mustalias.buallms=true",
	    "-Dpetablox.mustalias.statistics=true",
	    "-Dpetablox.mustalias.jumpempty=false",
	    "-Dpetablox.bottom-up.merge=pjoin",

            "-Dpetablox.reflect.exclude=true",
            "-Dpetablox.reuse.scope=false",
	    "-Dpetablox.reflect.kind=dynamic",
#           "-Dpetablox.reflect.file=\${petablox.work.dir}/reflect.txt",
#           "-Dpetablox.methods.file=\${petablox.work.dir}/methods.txt",
	    "-Dpetablox.scope.exclude=com.,sun.",
            "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org."
        ],   
    "provenance-instr" =>
     	[
	    "-Dpetablox.run.analyses=cipa-0cfa-dlog,ctxts-java,argCopy-dlog,provenance-instr",
	    "-Dpetablox.scope.exclude=java.,javax.,com.,sun.,org.",
	],
    "print-polysite" =>
    	[
	    "-Dpetablox.run.analyses=cipa-0cfa-dlog,ctxts-java,argCopy-dlog,cspa-kcfa-dlog,polysite-dlog,provenance-vis",
	    "-Dpetablox.provenance.out_r=polySite",
	    "-Dpetablox.ctxt.kind=cs",
	],
     	"provenance-kcfa" =>
     [
      	"-Dpetablox.run.analyses=kcfa-refiner",
	"-Dpetablox.scope.exclude=com,sun",
        "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dpetablox.provenance.cfa2=false",
	"-Dpetablox.provenance.query=all",
	"-Dpetablox.provenance.heap=true",
	"-Dpetablox.provenance.mono=true",
	"-Dpetablox.provenance.queryWeight=0",
	"-Dpetablox.provenance.boolDomain=true",
	"-Dpetablox.provenance.invkK=5",
	"-Dpetablox.provenance.allocK=5",
	"-Dpetablox.max.heap=16g",
        "-Dpetablox.reflect.exclude=true",
        "-Dpetablox.reuse.scope=false",
	"-Dpetablox.reflect.kind=dynamic",

     ],
     	"refinegen-kobj" =>
     [
      	"-Dpetablox.run.analyses=kobj-gen",
	"-Dpetablox.scope.exclude=com.,sun.",
        "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dpetablox.max.heap=16g",
        "-Dpetablox.reflect.exclude=true",
        "-Dpetablox.reuse.scope=false",
	"-Dpetablox.reflect.kind=dynamic",
     ],
     	"provenance-kobj" =>
     [
      	"-Dpetablox.run.analyses=kobj-refiner",
	"-Dpetablox.scope.exclude=com.,sun.",
        "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dpetablox.provenance.obj2=false",
	"-Dpetablox.provenance.query=all",
	"-Dpetablox.provenance.heap=true",
	"-Dpetablox.provenance.mono=true",
	"-Dpetablox.provenance.queryWeight=0",
	"-Dpetablox.provenance.boolDomain=true",
	"-Dpetablox.provenance.invkK=10",
	"-Dpetablox.provenance.allocK=10",
	"-Dpetablox.max.heap=16g",
        "-Dpetablox.reflect.exclude=true",
        "-Dpetablox.reuse.scope=false",
	"-Dpetablox.reflect.kind=dynamic",

     ],
  	"provenance-typestate" =>
     [
      	"-Dpetablox.run.analyses=cipa-0cfa-dlog,typestate-refiner",
	"-Dpetablox.scope.exclude=com.,sun.",
        "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher.,org.",
	"-Dpetablox.provenance.query=all",
	"-Dpetablox.provenance.heap=true",
	"-Dpetablox.provenance.mono=true",
	"-Dpetablox.provenance.queryWeight=0",
	"-Dpetablox.max.heap=16g",
	"-Dpetablox.bddbddb.max.heap=4g",
	"-Dpetablox.ssa.kind=nomovephi",
        "-Dpetablox.reflect.exclude=true",
        "-Dpetablox.reuse.scope=false",
	"-Dpetablox.reflect.kind=dynamic",
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
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dpetablox.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dpetablox.reflect.kind=none"
 	    ]

   	},
    "mustalias-td" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dpetablox.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dpetablox.reflect.kind=none"
 	    ]


   	},
    "mustalias-bu" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "toba-s" => [
		"-Dpetablox.reflect.kind=none"
	    ],
	    "kawa-c" => [
		"-Dpetablox.reflect.kind=none"
 	    ]

   	},

    "typestate_metaback" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ]


   	},
    "provenance-kcfa" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ]


   	},
"refinegen-kobj" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		 "luindex" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"schroeder-m" => [
	    	"-Dpetablox.max.heap=64000m",
	    ],

   	},
"provenance-kobj" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "luindex" => [
		    "-Dpetablox.max.heap=64000m",
		    "-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "bloat" => [
	    	"-Dpetablox.max.heap=64000m",
	    ],
	    "chart" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "fop" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "pmd" => [
	    	"-Dpetablox.max.heap=64000m",
	    ],
	    "xalan" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "schroeder-m" => [
		    "-Dpetablox.max.heap=64000m",
	    ],

   	},
    "provenance-typestate" =>
   	{
    	    "lusearch" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
		"luindex" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "avrora" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "hsqldb" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "antlr" => [
	    	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "batik" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "sunflow" => [
	    	"-Dpetablox.max.heap=16000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
	    ],
	    "rhino-a" => [
	   	"-Dpetablox.max.heap=64000m",
		"-Dpetablox.check.exclude=java.,com.,sun.,sunw.,javax.,launcher."
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
	$bench_dir = "$mainBench_dir/$benchmarks{$chosen_program}";
}

my $bench_options = $bench_options_map{$chosen_analysis}{$chosen_program};
if (!$bench_options) { @$bench_options = (); }
# NOTE: order of cmdline, bench, local, global options on following line is important
my @options = (@global_options, @$local_options, @$bench_options, @cmdline_options);
@options = map { s/\${petablox.work.dir}/$bench_dir/; $_ } @options;
unshift (@options, "-Dpetablox.work.dir=$bench_dir");
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
    my @final_options = ("-Dpetablox.out.dir=./petablox_output_$chosen_analysis", @_);
    runcmd_in_background(@final_options);
}

sub run_master {
    my @final_options = (("-Dpetablox.parallel.mode=master", "-Dpetablox.parallel.host=$master_host", "-Dpetablox.parallel.port=$master_port",
        "-Dpetablox.out.dir=./petablox_output_$chosen_analysis/Master"), @_);
    runcmd_in_background(@final_options);
}

sub run_worker {
    my @final_options = (("-Dpetablox.parallel.mode=worker", "-Dpetablox.parallel.host=$master_host", "-Dpetablox.parallel.port=$master_port"), @_);
    for (my $i = 1; $i <= $num_workers; $i++) {
        runcmd_in_background("-Dpetablox.out.dir=./petablox_output_$chosen_analysis/Worker$i", @final_options);
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

