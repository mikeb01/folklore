#!/bin/bash

RUN_HOME=`dirname 0`

java -jar $RUN_HOME/lib/runtime/folklore.jar async.server.edge 10000 localhost 10001
