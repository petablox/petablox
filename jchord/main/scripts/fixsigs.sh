#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
case $OSTYPE in 
	darwin*)
		sediflag='-i '\'\'
		;;
	*)
		sediflag='-i'
		;;
esac

if [ -n "$1" ]; then
	searchdir="$1"
	shift
else
	searchdir=$(cd "$SCRIPTDIR/../src" && pwd)
fi

find "$searchdir" -name '*.logic' -exec \
	sed \
		-e 's/:inputs: IinvkArg(I,Z,V)/:inputs: IinvkArg(I0,Z0,V1)/g' \
		-e 's/:inputs: sub(T,T)/:inputs: sub(T1,T0)/g' \
		-e 's/:inputs: cha(M,T,M)/:inputs: cha(M1,T1,M0)/g' \
		-e 's/:inputs: HT(H,T)/:inputs: HT(H0,T1)/g' \
		-e 's/:inputs: MmethRet(M,Z,V)/:inputs: MmethRet(M0,Z0,V1)/g' \
		$sediflag {} \;
