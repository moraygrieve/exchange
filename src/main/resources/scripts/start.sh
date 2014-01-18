#!/bin/bash
#  Script to start the exchange
#
makeDir() { if [ ! -d $1 ]; then mkdir -pm 755 $1; fi }

VERSION=${pom.version}
LOG_DIR=log; makeDir $LOG_DIR
CONFIG_DIR=config; makeDir $CONFIG_DIR
VAR_DIR=var; makeDir $VAR_DIR
DATE=`date +%Y%m%d-%H%m%S`

if [ $# -ne 1 ]
then
    echo "Usage: $0 [database]"
    exit
else
    if [ -f $VAR_DIR/ecn.pid ] && kill -0 `cat $VAR_DIR/ecn.pid` 2>/dev/null
    then
        echo Exchange is running, please shut it down before attempting to start ...
    else
        CWD=`pwd`
        java -Dlog4j.configuration=file:///$CWD/etc/server-log4j.properties
        -Decn.database.path=$1 -Dsqlite4java.library.path=$CWD/lib -jar -Dlog.dir=$LOG_DIR
        -Dconfig.dir=$CONFIG_DIR -Dlog.timestamp=$DATE $CWD/lib/exchange-$VERSION.jar
        $CWD/etc/server.cfg  >> $LOG_DIR/ecn-$DATE.out 2>> $LOG_DIR/ecn-$DATE.err &
        echo $! > $VAR_DIR/ecn.pid
    fi
fi