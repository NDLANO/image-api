# IMAGE API 
API for accessing images

# Updating dependencies

## SwaggerUI
[Swagger UI](https://github.com/swagger-api/swagger-ui) is cloned into the project as a submodule. (Ref. http://stackoverflow.com/questions/7813030/how-can-i-have-linked-dependencies-in-a-git-repo)

### Initializing
Upon cloning the repository run

    git submodule init
    git submodule update

### Updating dependency
If you want to update the dependency run

    cd lib/swagger-ui
    git pull
    git add
    git commit
    
### Acknowledge updated dependency when pulling
When pulling a repo with an updated dependency, the dependency folder will be marked as modified. In order to update the dependency locally run

    git submodule update

# Building and distribution

## Compile
    sbt compile

## Package and run locally
    sbt assembly
    export PORT=8080
    java -jar target/scala-2.11/image-api.jar

## Create Docker Image
    sbt docker

You need to have a docker daemon running locally. Ex: [boot2docker](http://boot2docker.io/)

## Deploy Docker Image to Amazon (via DockerHub)
    cd src/main/deploy  
    deployRemote.sh

