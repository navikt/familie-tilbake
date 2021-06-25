#!/bin/sh
echo "running init.sh script"
export CREDENTIAL_USERNAME=$(cat /secret/serviceuser/username)
export CREDENTIAL_PASSWORD=$(cat /secret/serviceuser/password)