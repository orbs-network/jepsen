#!/bin/sh
set -e # exit on an error

docker exec jepsen-control cd orbs && lein run test --concurrency 10