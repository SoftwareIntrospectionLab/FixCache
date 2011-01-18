#!/bin/bash

# verify that there are two arguments
if [[ $# -lt 2 ]]; then
	echo "Usage: $0 x_rates.txt x_filedist.txt"
	exit
fi

# verify that $1 contains string "rates"
if [[ ! $1 =~ "rates" ]]; then
	echo "Usage: first file must contain the substring \"rates\""
fi

# verify that $2 contains string "filedist"
if [[ ! $2 =~ "filedist" ]]; then
	echo "Usage: second file must contain the substring \"filedist\""
fi

# remove extension from arguments
rname=${1%.*}
fdname=${2%.*}

cp $1 rscripts/ratesfile
cp $2 rscripts/filedistfile

cd rscripts

# run the scripts
./month_vs_rate.R
mv Rplots.pdf ../${rname}-month_vs_rate.pdf

./file_vs_bugfix.R 
mv Rplots.pdf ../${fdname}-file_vs_bugfix.pdf

./file_vs_duration.R
mv Rplots.pdf ../${fdname}-file_vs_duration.pdf

./duration_vs_hits.R
mv Rplots.pdf ../${fdname}-duration_vs_hits.pdf

./file_vs_loc.R
mv Rplots.pdf ../${fdname}-file_vs_loc.pdf

# cleanup
rm ratesfile
rm filedistfile

# display the graphs
cd ../
open ${rname}-month_vs_rate.pdf
open ${fdname}-file_vs_bugfix.pdf
open ${fdname}-file_vs_duration.pdf
open ${fdname}-duration_vs_hits.pdf
open ${fdname}-file_vs_loc.pdf