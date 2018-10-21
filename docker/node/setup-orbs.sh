#!/bin/sh

go get github.com/orbs-network/orbs-network-go
cd /opt/go/src/github.com/orbs-network/orbs-network-go
rm -rf vendor
./git-submodule-checkout.sh
go build -o /opt/orbs/orbs-node -a main.go