#!/bin/bash

[[ -n "$TRAVIS_TAG" ]] && openssl aes-256-cbc -K $encrypted_65aa329e7553_key -iv $encrypted_65aa329e7553_iv -in gradle.properties.enc -out gradle.properties -d || true
