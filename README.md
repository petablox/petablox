** Getting Started

You need to download LogicBlox 3.10.21 into `logicblox-3.10.21`.  Then 
run locally:

```
$ vagrant up
$ vagrant ssh
```

Inside the VM, start LogicBlox with:

```
$ source /vagrant/logicblox-3.10.21/etc/profile.d/logicblox.sh
$ lb-services start
```

*TODO: Start LogicBlox as part of provisioning.*

** Known Issues

The stable DOOP version fails if some env variables are unset.  You need at least:

```
export LD_LIBRARY_PATH=
export LOGICBLOX_HOME=/vagrant/logicblox-3.10.21/logicblox
```

