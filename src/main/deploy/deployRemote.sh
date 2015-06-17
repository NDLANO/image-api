#!/bin/sh

PROJECT=ndla/image-api
VER=v0.1
GIT_HASH=`git log --pretty=format:%h -n 1`

VERSION=${VER}_${GIT_HASH}

#TODO: Fix dette når sertifikater begynner å fungere igjen
#DOCKER_CERTS="--tlscacert=init/docker/certs/ca.pem --tlscert=init/docker/certs/client-cert.pem --tlskey=init/docker/certs/client-key.pem --tlsverify"
#export DOCKER_OPTIONS="$DOCKER_CERTS -H 52.28.51.79:4243"
export DOCKER_OPTIONS="-H 52.28.51.79:4243"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
echo ">>>>>>>> DEPLOY $PROJECT:$VERSION with params: $DOCKER_OPTIONS"
echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"

set -e

echo '>>> Pushing local image to Docker Hub'
docker push $PROJECT:$VERSION

## Remove local settings when communicating remotely
DOCKER_HOST_LOCAL=$DOCKER_HOST
DOCKER_TLS_VERIFY_LOCAL=$DOCKER_TLS_VERIFY
DOCKER_CERT_PATH_LOCAL=$DOCKER_CERT_PATH
unset DOCKER_HOST
unset DOCKER_TLS_VERIFY
unset DOCKER_CERT_PATH

echo '>>> Get old container id'
CID=$(docker $DOCKER_OPTIONS ps | grep "$PROJECT" | awk '{print $1}')
echo "Old container id: "$CID

echo '>>> Stopping old container'
if [ "$CID" != "" ];
then
  docker $DOCKER_OPTIONS stop $CID
fi

echo '>>> Starting new container'
docker $DOCKER_OPTIONS run -p 80:80 -d --restart=always -t $PROJECT:$VERSION

# Reestablish local settings when finished
export DOCKER_HOST=$DOCKER_HOST_LOCAL
export DOCKER_TLS_VERIFY=$DOCKER_TLS_VERIFY_LOCAL
export DOCKER_CERT_PATH=$DOCKER_CERT_PATH_LOCAL