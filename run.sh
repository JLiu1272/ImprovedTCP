#!/bin/sh 
# A script for running the java program 
# and cleaning up any *.class files by placing it into 
# the /classes/ directory 

# compile program 
javac $1.java 

# Move all *.class files to classes directory 
mv *.class classes 

# Run the program 
cd classes 
java $1
