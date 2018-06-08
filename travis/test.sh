#!/bin/bash -eux

./install.sh

systemctl --user start pnyao-server.service
sleep 10
curl -sS http://localhost:9000 > /dev/null

