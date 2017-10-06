#!/bin/bash

VERSION="$1"
source ./build.properties
PROJECT="$GDLOrganization/$GDLComponentName"

if [ -z $VERSION ]
then
    VERSION="SNAPSHOT"
fi

sbt -Ddocker.tag=$VERSION docker
echo "BUILT $PROJECT:$VERSION"