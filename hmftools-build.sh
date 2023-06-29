#!/bin/bash

set -e

# Verify input
if [ $# -ne 1 ]; then
  echo "Invalid arguments. Usage: hmftools-build semver-version"
  exit 1
fi

SEMVER="$1"
# The regex pattern is <module_name>-<semver>
SEMVER_REGEX="^([a-zA-Z0-9_-]+)-([0-9]+)\.([0-9]+)\.([0-9]+)(-(alpha|beta)\.[0-9]+)?$"
if ! [[ "$SEMVER" =~ $SEMVER_REGEX ]]; then
  echo "Invalid semver version: $SEMVER"
  exit 1
fi

BUILD_MODULE=${BASH_REMATCH[1]}

# Check if this module exists
if ! [[ -d "$BUILD_MODULE" ]]; then
  echo "Invalid semver version: module $BUILD_MODULE does not exist"
  exit 1
fi

## set the property version in the parent
mvn -f "pom.xml" versions:set-property -DgenerateBackupPoms=false -Dproperty="${BUILD_MODULE}.version" -DnewVersion="${SEMVER}"

## set the property version of the shared libraries
# hmf-common
mvn -f "pom.xml" versions:set-property -DgenerateBackupPoms=false -Dproperty="hmf-common.version" -DnewVersion="${SEMVER}"
mvn -f "pom.xml" versions:set-property -DgenerateBackupPoms=false -Dproperty="orange-datamodel.version" -DnewVersion="${SEMVER}"
mvn -f "pom.xml" versions:set-property -DgenerateBackupPoms=false -Dproperty="patient-db.version" -DnewVersion="${SEMVER}"
## set the version of the actual parent
mvn -f "pom.xml" versions:set -DgenerateBackupPoms=false -DnewVersion=${SEMVER}



echo "done!"
