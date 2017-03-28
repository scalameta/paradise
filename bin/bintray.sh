#!/usr/bin/env bash
set -eu

if [[ "$DRONE_BRANCH" == "master" ]]; then
  mkdir -p $HOME/.bintray
  cat > $HOME/.bintray/.credentials <<EOF
realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_USERNAME
password = $BINTRAY_API_KEY
EOF
  /usr/bin/sbt "very publish"
fi


