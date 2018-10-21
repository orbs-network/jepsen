#!/bin/sh

ps -ef | grep orbs | awk '{print $2}' | xargs kill
echo "Orbs binary stopped"

exit 0