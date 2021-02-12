#!/bin/bash

VERSION="$1"
source ./build.properties
PROJECT="$NDLAOrganization/$NDLAComponentName"

if [ -z $VERSION ]
then
    VERSION="SNAPSHOT"
fi

sbt -Ddocker.tag=$VERSION "set test in Test := {}" docker
echo "BUILT $PROJECT:$VERSION"
