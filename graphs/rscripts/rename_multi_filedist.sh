#!/bin/bash

dir=$1
for file in `/bin/ls $dir` ; do 
    new=`echo $file | sed 's/-.*//'`
    mv $file $new
done
