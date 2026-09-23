#!/bin/sh
export KEYSTORE_PATH="${KEYSTORE_PATH:-/app/applet/my-upload-key.jks}"
export STORE_PASSWORD="${STORE_PASSWORD:-android}"
export KEY_PASSWORD="${KEY_PASSWORD:-android}"
exec gradle "$@"
