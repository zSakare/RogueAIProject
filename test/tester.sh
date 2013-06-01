#!/bin/bash

set -e

server() {
  (cd ../bin/logic && java Rogue -i ../$1 -p 1337 | grep -i moves)
}

player() {
  ./agent -p 1337 > /dev/null
}

echo Compiling teh javas
(cd server && javac ../bin/logic/Rogue.java)

for TEST in inputs/*.in; do
  echo Running $( basename "$TEST" )

  #server "$TEST" &
  #sleep 0.2
  #player || true

  #wait
done
