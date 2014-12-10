#!/bin/bash
lein uberjar && scp target/no-sports*standalone.jar NoSportsAJ: && scp secrets.edn NoSportsAJ:
