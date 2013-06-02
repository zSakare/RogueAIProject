#!/bin/bash

set -e

server() {
  (cd ../src && (java logic.Rogue -i ../test/$1 -p 30000) | grep -i moves)
}

player() {
  (cd ../src && java logic.Agent -p 30000 > /dev/null)
}

echo Compiling teh javas
(cd ../src/logic && javac Rogue.java -classpath ../)
(cd ../src/logic && javac Agent.java -classpath ../)

for TEST in inputs/*.in; do
  echo Running $( basename "$TEST" )

  server "$TEST" &
  sleep 0.2
  player || true

  wait
done
