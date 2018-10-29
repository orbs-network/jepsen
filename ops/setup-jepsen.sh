#!/bin/sh
set -e # exit on an error

JEPSEN_ROOT=`pwd`
echo "JEPSEN_ROOT set to: $JEPSEN_ROOT"

cd docker
sh up.sh --daemon