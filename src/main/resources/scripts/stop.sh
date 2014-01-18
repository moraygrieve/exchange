#!/bin/bash
#  Script to stop the exchange
#
VAR_DIR=var

if [ -f $VAR_DIR/ecn.pid ] && kill -0 `cat $VAR_DIR/ecn.pid` 2>/dev/null
then
    kill -9 `cat $VAR_DIR/ecn.pid`
    rm $VAR_DIR/ecn.pid
else
    echo Exchange is not running ...
fi