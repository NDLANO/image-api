# IMAGE API
[![Build Status](https://travis-ci.org/NDLANO/image-api.svg?branch=master)](https://travis-ci.org/NDLANO/image-api)

API for accessing images

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Publish to nexus
    sbt publish

## Create Docker Image
    sbt docker

## Deploy Docker Image
    See Deployment-project
        
## Test
    curl http://$DOCKER_ADDR:30001/images/
    

