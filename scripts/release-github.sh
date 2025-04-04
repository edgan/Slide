#!/bin/bash

RELEASE_NOTES="${1}"
export RELEASE_NOTES

./gradlew assembleWithGPlayRelease
./gradlew githubRelease
