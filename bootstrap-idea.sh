#!/bin/sh

(cd juke && lein deps && lein install)
(cd juke-cucumber && lein deps && lein install)
(cd jukebox && lein deps && lein install)
(cd end-to-end && lein deps)

(cd end-to-end && rm -f pom.xml && idea .)
