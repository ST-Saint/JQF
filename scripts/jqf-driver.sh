#!/bin/bash

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

ROOT_DIR=`dirname $SCRIPT_DIR`

# Find JQF classes and JARs
project="jqf"
version="1.8-SNAPSHOT"

FUZZ_DIR="${ROOT_DIR}/fuzz/target/"
INST_DIR="${ROOT_DIR}/instrument/target/"

FUZZ_JAR="${FUZZ_DIR}/$project-fuzz-$version.jar"

# INST_JAR="${INST_DIR}/$project-instrument-$version.jar"
INST_JAR="/home/yayu/Project/Upgrade-Fuzzing/jacoco/jacoco/target/lib/jacocoagent.jar"
# INST_JAR="/home/yayu/Downloads/jacoco/lib/jacocoagent.jar"

# INST_JAR="/home/yayu/Downloads/jacoco/lib/jacocoagent.jar=destfile=/home/yayu/tmp/hadoop/hadoop-jacoco.exec,classdumpdir=/home/yayu/hadoop/hadoop-class/,output=file,address=localhost"

# Compute classpaths (the /classes are only for development;
#   if empty the JARs will have whatever is needed)
# INST_CLASSPATH="${INST_DIR}/classes:${INST_JAR}:${INST_DIR}/dependency/asm-9.1.jar"
INST_CLASSPATH=$"${INST_DIR}/classes:${INST_JAR}"
FUZZ_CLASSPATH="${FUZZ_DIR}/classes:${FUZZ_DIR}/dependency/*:${FUZZ_JAR}"



# If user-defined classpath is not set, default to '.'
if [ -z "${CLASSPATH}" ]; then
  CLASSPATH="."
fi

export CLASSPATH="${CLASSPATH}:${INST_DIR}/classes"

# Java Agent config (can be turned off using env var)
if [ -z "$JQF_DISABLE_INSTRUMENTATION" ]; then
    JAVAAGENT="-javaagent:${INST_JAR}=includes=PngTest,excludes=java.*:jdk.*:org.jacoco.*,output=tcpserver"
fi

# Run Java
if [ -n "$JAVA_HOME" ]; then
    java="$JAVA_HOME"/bin/java
else
    java="java"
fi

echo "$java" -ea \
  ${JAVAAGENT} \
  -cp "${FUZZ_CLASSPATH}:${CLASSPATH}" \
  ${JVM_OPTS} \
  $@


"$java" -ea \
  ${JAVAAGENT} \
  -cp "${FUZZ_CLASSPATH}:${CLASSPATH}" \
  ${JVM_OPTS} \
  $@
