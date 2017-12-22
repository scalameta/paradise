#!/usr/bin/env bash
set -eu

if [ "$TRAVIS_SECURE_ENV_VARS" == true ]; then
  git log | head -n 20
  echo "$PGP_SECRET" | base64 --decode | gpg --import
  if [ -n "$TRAVIS_TAG" ]; then
    sbt ci-release
  else
    echo "Merge into master, no tag push."
  fi
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH"
fi
