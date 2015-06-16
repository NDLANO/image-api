#!/bin/sh

DOCKER_CERTS="--tlscacert=init/docker/certs/ca.pem --tlscert=init/docker/certs/client-cert.pem --tlskey=init/docker/certs/client-key.pem"
export DOCKER_OPTIONS="$DOCKER_CERTS -H 52.28.51.79:4243"

PROJECT=ndla/image-api
VERSION=latest

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
echo ">>>>>>>> DEPLOY $PROJECT:$VERSION with params: $DOCKER_OPTIONS"
echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"

set -e

#echo ">>> Building and tagging new image: $PROJECT:$VERSION"
## analyse the log to find out if build passed (see https://github.com/dotcloud/docker/issues/1875)
#docker $DOCKER_OPTIONS build --rm -t "$PROJECT:$VERSION" . | tee /tmp/docker_build_result-$PROJECT.log
#RESULT=$(cat /tmp/docker_build_result-$PROJECT.log | tail -n 1)
#if [[ "$RESULT" != *Successfully* ]];
#then
#  exit -1
#fi

#echo '>>> Pushing local image to Docker Hub'
#docker push $PROJECT:$VERSION

echo '>>> Get old container id'
CID=$(docker $DOCKER_OPTIONS ps | grep "$PROJECT" | awk '{print $1}')
echo "Old container id: "$CID

echo '>>> Stopping old container'
if [ "$CID" != "" ];
then
  docker $DOCKER_OPTIONS stop $CID
fi

echo '>>> Starting new container'
docker $DOCKER_OPTIONS run -p 80:80 -d --restart=always -t $PROJECT:$latest

