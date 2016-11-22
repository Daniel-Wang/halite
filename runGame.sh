#!/bin/bash

export v2="MyBot"
export v1="submission6/MyBot"
javac "$v1".java
javac "$v2".java
./halite -d "30 30" "java RandomBot" "java $v2" | tail -5
./halite -d "30 30" "java $v2" "java RandomBot" | tail -5
