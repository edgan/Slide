#!/bin/bash

./gradlew bundleWithGPlayRelease

RC="${?}"

if [ "${RC}" -ne 0 ]; then
  exit "${RC}"
fi

scripts/release-github.sh
