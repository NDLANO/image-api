# IMAGE API 
[![Build Status](https://travis-ci.org/GlobalDigitalLibraryio/image-api.svg?branch=master)](https://travis-ci.org/GlobalDigitalLibraryio/image-api)

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

### IntegrationTest Tag and sbt run problems
Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/article-api/src/test/java/no/ndla/tag/IntegrationTest.java```. 
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail. 
Therefore we have the ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```  

    sbt "test-only -- -n no.ndla.tag.IntegrationTest"

### Working with the fork
This repository is forked from [NDLA](https://github.com/NDLANO/image-api) (called `upstream` below).
We want to keep this repo in sync with `upstream` to use features and bug fixes from NDLA.
We also want to merge _some_ of our own changes with the `upstream`, so NDLA can benefit from them as well.
There are 2 long living branches in this repo: `master` and `ndla`. `master` is the branch with `image-api` being
deployable on the GDL platform, with all the GDL specifics. `ndla` is synced with the `upstream/master`, and
should never contain any GDL specifics.

Note:
Before using any of the git commands below, make sure you've added NDLA's repo as upstream:
```
git remote add upstream git@github.com:NDLANO/image-api.git
```
The `ndla` branch is configured to be _protected_ on GitHub, to avoid accidentally deleting it.

Here's how to work with branches and pull requests to do so:

#### Case #1: Adding GDL specific features (of no interest to NDLA)
1. Create feature branch from `master`, e.g. `Issue#123-new_feature`.
2. Open pull request, and merge it into `master`.

#### Case #2: Add changes from NDLA to our fork
1. Run `git fetch upstream/master` to fetch all new changes from NDLA.
2. Checkout `ndla` branch.
3. Run `git merge upstream/master` to merge these changes into the `ndla` branch.
4. `git push`.
5. Open pull request, merging `ndla` into `master`.


#### Case #3: Adding features that should be merged upstream (i.e. they are of interest to NDLA)
1. Run `git fetch upstream/master` to fetch all new changes from NDLA.
2. Checkout `ndla` branch.
3. Run `git merge upstream/master` to merge these changes into the `ndla` branch.
4. Checkout a new feature branch from `ndla`, e.g. `Issue#321-new_mutual_beneficial_feature`.
5. Edit and commit changes.
6. Open pull request, merging `Issue#321-new_mutual_beneficial_feature` into `ndla`.
7. Do internal QA in GDL.
8. Open pull request, merging `ndla` into `upstream/master`.
9. Let NDLA do QA of this.
10. When this is done, fetch changes from `upstream` (see #2) to update our `master` branch with the same changes.
