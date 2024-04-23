#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

pushd "${DIR}"
    # extjs:aggregate-js
    mvn -f pom_for_bundle.xml -PbuildKar clean package -Dmaven.test.skip=true
popd
