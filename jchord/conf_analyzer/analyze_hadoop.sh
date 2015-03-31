#!/bin/bash

#  This is a script for analyzing Hadoop.
#  To use it, you need to set HADOOP_HOME below, or else have it in your environment
#
#  Modify the line below to point to your HADOOP_HOME.
#
HADOOP_HOME=/Users/asrabkin/workspace/hadoop-0.20.2

if [[ $0 != "/"* ]]; then
HERE=`pwd`
else
HERE=`dirname $0`
fi
echo "script is in $HERE"

ant run -Dchord.work.dir=$HERE/examples/hadoop20 \
-Ddictionary.name=hdfs.dict \
-Dchord.main.class=org.apache.hadoop.hdfs.server.datanode.DataNode \
-Dhadoophome=$HADOOP_HOME \
-Dentrypoints.file=examples/hadoop20/entrypoints-20-hdfs.txt \
-Dchord.props.file=$HERE/examples/hadoop20/nomain.properties

ant run -Dchord.work.dir=$HERE/examples/hadoop20 \
-Ddictionary.name=mapred.dict \
-Dchord.main.class=org.apache.hadoop.mapred.TaskTracker \
-Dhadoophome=$HADOOP_HOME \
-Dentrypoints.file=examples/hadoop20/entrypoints-20-mapred.txt \
-Dchord.props.file=$HERE/examples/hadoop20/nomain.properties