#!/bin/bash

export v1="MyBot"
export v2="MyNBot"

javac "$v1".java
javac "$v2".java
./halite -d "30 30" "java MyBot" "java $v2"
