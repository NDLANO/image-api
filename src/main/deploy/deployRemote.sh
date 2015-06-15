#!/bin/sh

DOCKER_CERTS="--tlscacert=init/docker/certs/ca.pem --tlscert=init/docker/certs/client-cert.pem --tlskey=init/docker/certs/client-key.pem"
export DOCKER_OPTIONS="$DOCKER_CERTS -H 52.28.51.79:4243"

docker $DOCKER_OPTIONS run hello-world

