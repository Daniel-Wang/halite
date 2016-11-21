#!/bin/bash

export v2="MyBot"
export v1="RandomBot"
javac "$v1".java
javac "$v2".java
./halite -d "30 30" -s 1962120952 "java $v2" "java $v1" | tail -5
./halite -d "30 30" -s 1964998304 "java $v1" "java $v2" | tail -5
