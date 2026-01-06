#!/bin/sh

export PATH=/app/jre/bin:/usr/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/app/lib

jar_dir=/app/lib/
jar_file=com.gitlab.Hague.forkyz.jar

java -jar $jar_dir/$jar_file "$@"
