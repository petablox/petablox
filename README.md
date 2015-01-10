## Getting Started

First copy `provision/config.json.default` to `provision/config.json`.  You can 
adjust values there as needed.  This guide assumes LogicBlox version 3.10.21, but 
you can change the version in `config.json` and modify the directions accordingly.

### Manual Downloads

#### LogicBlox

Download LogicBlox 3.10.21 into `logicblox-3.10.21` or whichever version 
you selected.

#### JREs for DOOP

Due to Oracle's license restrictions, provisioning doesn't acquire 
the JREs required by DOOP automatically.  You need to download them 
into `doop-VERSION/externals` yourself.   However, provisioning will 
unpack them for you if they exist and match the names listed in 
`provision/config.json`.   If you already have a running VM, you 
can rerun `vagrant provision` from your host to unpack them, or do 
it manually from inside the VM.

Also make sure the list inside `doop-VERSION/doop.conf` matches the JRE 
versions you have in `provision.config.json`, something like:

```
#! /bin/bash

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
