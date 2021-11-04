#!/bin/bash
set -e

VERSION="$1"
source ./build.properties
PROJECT="$NDLAOrganization/$NDLAComponentName"

if [ -z $VERSION ]
then
    VERSION="SNAPSHOT"
fi

sbt -Ddocker.tag=$VERSION "set Test / test := {}" docker
echo "BUILT $PROJECT:$VERSION"
