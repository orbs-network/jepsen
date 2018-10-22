#!/bin/sh

(ps -ef | grep "[/]opt/orbs/orbs-node" | awk '{print $2}' | xargs --no-run-if-empty kill) || echo 0
echo "Orbs binary stopped"

exit 0