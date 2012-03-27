#!/bin/bash
JAVA_HOME=/cad2/ece419s/java/jdk1.6.0/

# $1 = Port of game server
# $2 = Max number of players (optional)
# $3 = -recover optional switch for recovering after failure
${JAVA_HOME}/bin/java MazewarServer $1 $2 $3
