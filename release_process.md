# How to publish a new release

* set new version in the pom
* create a commit for the change
* tag the commit with a signed tag
* push the commit and the tag
* update the pom to the new development version
* create a commit for the change
* push the commit

## Example

* a minor version is to be released
* current version in pom is `0.1.0-SNAPSHOT`

Steps:

``` shell
mvn versions:set -DnewVersion=0.1.0
git commit -am "Prepare for release 0.1.0"
git tag -s 0.1.0 -m "0.1.0"
git push && git push --tags
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT
git commit -am "Start next development iteration with 0.2.0-SNAPSHOT"
git push
```
