shord -- Soot-based STAMP
=========================

Preparation
-----------

To be able to build and run the STAMP tools, prepare your system as follows
(the package names provided are for an Ubuntu installation):

* install required packages:
    * Apache ant version >= 1.8 (package 'ant')
    * sqlite3 version >= 3  
    * Python version V, where 2.7 <= V < 3.0 (package 'python')
    * Java JDK version 1.6 (package 'openjdk-6-jdk' or 'sun-java6-jdk')
    * g++ version 1.7 (optional, for the SolverGen backend)
* install the Android SDK:
    * download the SDK starter package from developer.android.com/sdk/index.html
    * unpack the archive
    * add the following lines to your shell startup script (e.g. ~/.bashrc):
        export ANDROID_SDK_DIR=/path/to/unpacked/archive
        export PATH=$PATH:$ANDROID_SDK_DIR/tools:$ANDROID_SDK_DIR/platform-tools
    * start the Android SDK Manager: run 'android' on a new terminal (this uses
      a GUI, so you'll need to forward X if you're running it remotely, i.e.
      connect using ssh -Y ...)
    * install the following components:
        * Android SDK Tools (latest version)
        * Android SDK Platform-tools (latest version)
        * Android SDK Build-tools (latest version)
        * Android Support Library (latest version)
        * SDK Platform (Android 4.0.3 -- API 15)
        * Google APIs (Android 4.0.3 -- API 15)
    * if you're running on a 64-bit machine, you also need packages 'libc6-i386'
      and 'ia32-libs', to run some 32-bit Android build utilities

Action
-------

Run `<shord-dir>/stamp [dyn]analyze <app-dir> [-Dstamp.backend=solvergen]`
