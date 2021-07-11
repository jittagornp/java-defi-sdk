#!/bin/bash

path=.
abi=$1

web3j generate solidity -a $path/src/main/resources/abi/$abi -o $path/src/main/java -p me.jittagornp.defi.smartcontract
