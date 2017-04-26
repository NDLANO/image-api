# IMAGE API 
[![Build Status](https://travis-ci.org/NDLANO/image-api.svg?branch=master)](https://travis-ci.org/NDLANO/image-api)

API for accessing images

# Usage
Creates, updates and returns metadata about an image. It also supports resizing and cropping images on the fly.
Implements ElasticSearch for search within the article database.
To interact with the API, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.
It also has as internal import routines for importing images from the old system to this database.

For a more detailed documentation of the API, please refer to the [API documentation](https://staging.api.ndla.no).

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

### IntegrationTest Tag and sbt run problems
Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/article-api/src/test/java/no/ndla/tag/IntegrationTest.java```. 
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail. 
Therefore we have the ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```  
This, it seems, will unfortunalty override runs on your local commandline so that ```sbt "test-only -- -n no.ndla.tag.IntegrationTest"```
 will not run unless this line gets commented out or you comment out the ```@IntegrationTest``` annotation in ```SearchServiceTest.scala```
 This should be solved better!

    sbt "test-only -- -n no.ndla.tag.IntegrationTest"


## Create Docker Image
    sbt docker

