#!/bin/sh
set -e # exit on an error

docker exec jepsen-control bash -c "cd jepsen/orbs && lein run test --concurrency 10"