#!/usr/bin/env Rscript
# file vs. loc
filedist <- read.csv("filedistfile", comment.char="#")
filedist <- filedist[-c(1),]
attach(filedist)
plot(sort(loc, decreasing=T), type="o", xlab="File", ylab="LOC", ylim=range(0, max(loc)))
