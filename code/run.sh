#!/bin/bash

# Run the apps for a number of times
# To run: ./run.sh APP_LIST NUMBER_OF_RUNS PARAMETERS_TO_RUN_JAVA
apps=$1
runs=$2
shift
shift

JAR='build/libs/dp-trace-freq-1.0-SNAPSHOT-all.jar'
CLASS='osu.presto.Main'

for APP in ${apps[@]}; do
  echo +++++ $APP +++++
  for (( i=1; i<=$runs; i++)); do
    echo "--- run $i ---"
    CMD="java -ea -cp $JAR $CLASS -app $APP $@"
    $CMD
  done
  echo
done