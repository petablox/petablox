#!/bin/bash

source "$1/etc/profile.d/logicblox.sh"
if ! lb status | grep -qF "Server is 'ON'"; then
	echo "Starting Logicblox..."
	lb-services start
	exit $?
else
	echo "Logicblox appears to be running."
fi
exit 0
