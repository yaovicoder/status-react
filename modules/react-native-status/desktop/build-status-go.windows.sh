#!/bin/bash

export GOROOT=$1
export GOPATH=$2
export PATH=$GOROOT/bin:$GOROOT:$GOPATH:$PATH
 
cd $3/lib
go get ./
cd ..
GOOS=windows GOARCH=amd64 CGO_ENABLED=1 CC=$4 CC_FOR_TARGET=$4 CXX_FOR_TARGET=$5 make statusgo-library
echo Done