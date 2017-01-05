#!/bin/bash

export v2="MyBot"
export v1="MyExpJavaBot"
javac "$v1".java
javac "$v2".java
./halite -d "40 40" "java $v1" "java $v2" | tail -5
./halite -d "30 30" "java $v1" "java $v2" | tail -5
./halite -d "45 45" "java $v2" "java $v1" "java $v2" "java $v1" | tail -5


