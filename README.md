## Manual Setup

If you are running on Linux, you can choose to perform manual setup.  You must 
install and start the Logicblox server and follow the directions in the Doop 
folder's `README` file.  You need to set `DOOP_HOME` to use the install script 
of `soot-fact-generation` and `logicblox-unit`; `LOGICBLOX_HOME` to the Logicblox 
install location; and `LD_LIBRARY_PATH` to something (even empty, see Known Issues).

## Getting Started

These instructions assume you want to use a VM to run the analyses.  We use 
[Vagrant](https://www.vagrantup.com/) to provide the VM.  One important point 
is that the `petablox` source folder will be mounted inside the VM under `/vagrant`.  
See the Vagrant docs for other information.

First copy `provision/config.json.default` to `provision/config.json`.  You can 
adjust values there as needed.  This guide assumes LogicBlox version 3.10.21, but 
you can change the version in `config.json` and modify the directions accordingly.

### Manual Downloads

All downloads can be found on the Bitbucket repository's 
[download page](https://bitbucket.org/mayurnaik/petablox/downloads).

#### LogicBlox

Download LogicBlox 3.10.21 into `logicblox-3.10.21` or whichever version 
you selected.

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


### Provisioning the VM

Locally, just run:

```
$ vagrant up
$ vagrant ssh
```

Provisioning will start LogicBlox and update the `vagrant` user's 
profile as needed for both LogicBlox and DOOP.

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
