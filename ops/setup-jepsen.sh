#!/bin/sh
set -e # exit on an error

#$JEPSEN_ROOT=pwd
echo pwd
echo "JEPSEN_ROOT set to: $JEPSEN_ROOT"
exit 0
cd docker
sh up.sh --daemon