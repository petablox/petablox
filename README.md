# General Instructions

The main analyses framework in Petablox is derived from Chord and found in the `jchord` directory.  Petablox also includes the DOOP call graph/pointer analyses in `doop-r160113-bin`.  General directions follow and the framework-specific instructions are after that. 

## Manual Downloads

All downloads can be found on the Bitbucket repository's 
[download page](https://bitbucket.org/pag-lab/petablox/downloads).

## Petablox without LogicBlox

These instructions are if you want to use Petablox with BDDBDDB to run the
analyses. The simplest way to run Petablox this way is to download the pre-built
JAR file from the downloads page.

Since Petablox uses Chord internally, the instructions given [in the 
documentation](http://pag-www.gtisc.gatech.edu/chord/user_guide/index.html) 
for Chord can be used to run Petablox.

### Building the JAR
If you choose to build the JAR from the latest code in the repository, follow
these steps (you will need to have Apache Ant along with JRE for this)

```
cd petablox/jchord/main
ant
```

The JAR will be built in the same folder.

## Petablox with LogicBlox

These instructions assume you want to use a VM to run the analyses.  We use 
[Vagrant](https://www.vagrantup.com/) to provide the VM.  One important point 
is that the `petablox` source folder will be mounted inside the VM under `/vagrant`.  
See the Vagrant docs for other information.

First copy `provision/config.json.default` to `provision/config.json`.  You can 
adjust values there as needed.  This guide assumes LogicBlox version 3.10.21, but 
you can change the version in `config.json` and modify the directions accordingly.

#### LogicBlox

Download LogicBlox 3.10.21 into `logicblox-3.10.21` or whichever version 
you selected.  We recommend the latest LogicBlox 4.x version for running Chord's analyses, but you must use 3.x for DOOP.

### Provisioning the VM

Locally, just run:

```
$ vagrant up
$ vagrant ssh
```

Provisioning will start LogicBlox and update the `vagrant` user's 
profile as needed for both LogicBlox and DOOP.

# Chord #

The main part of Chord is found in `jchord/main`.  You can follow the directions [in the documentation](http://pag-www.gtisc.gatech.edu/chord/user_guide/index.html) for general information.

You can use Chord with LogicBlox in two ways, either as the main datalog engine or as an export target.  

## Using LogicBlox as the Datalog Engine

You can use LogicBlox as the Datalog engine instead of BDDBDDB (currently the default).  To do this, set the system property `chord.datalog.engine` to either `logicblox3` or `logicblox4`, depending on which version you are using, e.g. by passing:

```
-Dchord.datalog.engine=logicblox4
```

A workspace name will be generated automatically based on the absolute path of `chord.work.dir`.  You can manually override this by setting the `chord.logicblox.workspace` property.

## Exporting results to LogicBlox

You can also use the BDDBDDB solver to create all the relations and then export the result to LogicBlox for further processing.  To do this, add the `logicblox-export` analysis as the last analysis you run.  For example, if you were running 0-CFA and wanted to export the results to LogicBlox, you could pass:

```
-Dchord.run.analyses=cipa-0cfa-dlog,logicblox-export
```

By default, this exports to LogicBlox 4.  If you want to export for LogicBlox 3 you can pass:

```
-Dchord.logicblox.export.mode=logicblox3
```

# DOOP #

Below are the instructions for running DOOP.  DOOP requires a 3.x version of LogicBlox and will not work on LogicBlox 4.x out of the box.

## Manual Setup

If you are running on Linux, you can choose to perform manual setup.  You must 
install and start the Logicblox server and follow the directions in the Doop 
folder's `README` file.  You need to set `DOOP_HOME` to use the install script 
of `soot-fact-generation` and `logicblox-unit`; `LOGICBLOX_HOME` to the Logicblox 
install location; and `LD_LIBRARY_PATH` to something (even empty, see Known Issues).

#### JREs and benchmarks for DOOP

From the 
[download page](https://bitbucket.org/mayurnaik/petablox/downloads), 
save the JREs and Decapo benchmark files into `doop-VERSION/externals`.  
You need the following files:

- dacapo-2006-10-MR2-xdeps.zip
- j2re-1_3_1_20-linux-i586.bin
- j2re-1_4_2_19-linux-i586.bin
- jre-1_5_0_22-linux-amd64.bin
- jre-6u45-linux-x64.bin

**Note: You must have at least JRE 1.3 to run Doop's basic test suite.  If you only 
get one JRE, make sure it's JRE 1.3.**

Provisioning will 
unpack the JREs for you if they exist and match the names listed in 
`provision/config.json`.   If you already have a running VM, you 
can rerun `vagrant provision` from your host to unpack them, or do 
it manually from inside the VM.  

Also make sure the list inside `doop-VERSION/doop.conf` matches the JRE 
versions you have in `provision.config.json`, something like:

```
#!/bin/bash

doopconf_jre13=$doop/externals/jre1.3.1_20
doopconf_jre14=$doop/externals/j2re1.4.2_19
doopconf_jre15=$doop/externals/jre1.5.0_22
doopconf_jre16=$doop/externals/jre1.6.0_45
```


**TODO: Make doop.conf setup part of provisioning.**



### Running the Test Suite

From this point on, follow the instructions in the `README` file inside the Doop directory.  E.g., 
from inside the VM you can do:
```
$ cd /vagrant/doop-r160113-bin
$ ./compile-tests
$ ./run-testsuites basic
```

### Starting LogicBlox
If you need to start LogicBlox manually inside the VM, use:

```
$ lb-services start
```

## Known Issues

The stable DOOP version fails if some env variables are unset.  You need at least:

```
export LD_LIBRARY_PATH=
export LOGICBLOX_HOME=/vagrant/logicblox-3.10.21/logicblox
```

Provisioning sets this in the `vagrant` user's profile inside the VM automatically.
