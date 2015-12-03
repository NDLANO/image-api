# IMAGE API 
API for accessing images

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Package and run locally
    sbt assembly
    export PORT=8080
    export NDLACOMPONENT=image-api
    java -jar target/scala-2.11/image-api.jar

## Publish to nexus
    sbt publish

## Create Docker Image
    sbt docker

You need to have a docker daemon running locally. Ex: [boot2docker](http://boot2docker.io/)

## Deploy Docker Image
    ndla deploy <environment> image-api
    
# Setup local environment
## Database
    ndla deploy local postgres 9.4
    psql --host $(DOCKER_ADDR) --port 30005 --username "postgres" --password -d postgres -f src/main/db/local_testdata.sql
    
## Search-engine
    ndla deploy local search-engine
    
## Index the metadata to search-engine
    ndla index local image-api
    
## Test
    curl http://$DOCKER_ADDR:30001/images/
    

