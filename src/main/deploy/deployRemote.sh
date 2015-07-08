#!/bin/sh

PROJECT=ndla/image-api
VER=v0.1
GIT_HASH=`git log --pretty=format:%h -n 1`

VERSION=${VER}_${GIT_HASH}

SSH="ssh -i ./init/amazon/NDLA.pem ubuntu@52.28.51.79"

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
echo ">>>>>>>> DEPLOY $PROJECT:$VERSION with options : $SSH            "
echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"

set -e

echo '>>> Pushing local image to Docker Hub'
docker push $PROJECT:$VERSION

echo '>>> Get old container id'
CID=$($SSH docker ps | grep "$PROJECT" | awk '{print $1}')
echo "Old container id: "$CID

echo '>>> Stopping old container'
if [ "$CID" != "" ];
then
  $SSH docker stop $CID
fi

echo '>>> Starting new container'
$SSH docker run -p 80:80 -d --restart=always -t $PROJECT:$VERSION
