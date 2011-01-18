#!/usr/bin/env Rscript
# file vs. total number of residence time w/ max possible total residence time line
filedist <- read.csv("filedistfile", comment.char="#")
max_dur = filedist$duration[1]
filedist <- filedist[-c(1),]
attach(filedist)
plot(sort(duration, decreasing=T), type="o", xlab="File", ylab="Total Time in Cache (min)", ylim=range(0, max_dur))
abline(h=max_dur, lty=2)
