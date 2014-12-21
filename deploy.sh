#!/bin/bash
lein do clean, uberjar && scp target/no-sports*standalone.jar NoSportsAJ: && scp secrets.edn NoSportsAJ:
