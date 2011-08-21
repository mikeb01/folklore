#!/bin/bash

RUN_HOME=`dirname 0`

java -jar $RUN_HOME/lib/runtime/folklore.jar async.client localhost 10000 100
