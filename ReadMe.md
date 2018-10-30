# IMAGE API 
[![Build Status](https://travis-ci.org/NDLANO/image-api.svg?branch=master)](https://travis-ci.org/NDLANO/image-api)

## Usage
Creates, updates and returns metadata about an image. It also supports resizing and cropping images on the fly.
Implements ElasticSearch for search within the article database.
To interact with the API, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.
It also has as internal import routines for importing images from the old system to this database.

For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).

## Developer documentation

**Compile:** sbt compile

**Run tests:** sbt test

**Create Docker Image:** sbt docker
