#!/bin/bash
JAVA_HOME=/cad2/ece419s/java/jdk1.6.0/

# $1 = Location address of game server
# $2 = Port of game server
# $3 = 0 for GUI client | 1 for RobotClient
${JAVA_HOME}/bin/java Mazewar $1 $2 $3
