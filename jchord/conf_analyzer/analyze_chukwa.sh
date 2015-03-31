#!/bin/bash

#
#  Modify the line below to point to your CHUKWA_HOME.
#
CHUKWA_HOME=/Users/asrabkin/workspace/chukwa_trunk

ant run -Dchord.work.dir=`pwd`/examples/chukwa -DchukwaHome=$CHUKWA_HOME -Ddictionary.name=agent.dict -Dchord.main.class=org.apache.hadoop.chukwa.datacollection.agent.ChukwaAgent -Dentrypoints.file=examples/chukwa/agent.entry

ant run -Dchord.work.dir=`pwd`/examples/chukwa -DchukwaHome=$CHUKWA_HOME -Ddictionary.name=collector.dict -Dchord.main.class=org.apache.hadoop.chukwa.util.DumpChunks -Dentrypoints.file=examples/chukwa/collector.entry
