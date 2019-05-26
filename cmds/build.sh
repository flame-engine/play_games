#!/bin/bash -e

# flutter update-packages

flutter analyze --flutter-repo
flutter format .
