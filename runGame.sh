#!/bin/bash

export v2="MyBot"
export v1="RandomBot"
javac "$v1".java
javac "$v2".java
./halite -d "30 30" "java $v1" "java $v2"
