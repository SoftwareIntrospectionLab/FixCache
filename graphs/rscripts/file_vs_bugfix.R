#!/usr/bin/env Rscript
# file vs. hits + misses
filedist <- read.csv("filedistfile", comment.char="#")
# table(filedist$num_hits)/nrow(filedist)
filedist <- filedist[-c(1),]
attach(filedist)
bugfixes = num_hits + num_misses
plot(sort(bugfixes, decreasing=T), type="o", xlab="File", ylab="Number of Bug Fixes", ylim=range(0, max(bugfixes)))
