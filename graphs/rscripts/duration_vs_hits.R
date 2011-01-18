#!/usr/bin/env Rscript
# duration vs. hits
filedist <- read.csv("filedistfile", comment.char="#")
filedist <- filedist[-c(1),]
filedist <- filedist[order(filedist$duration), ]
attach(filedist)
# sort based on duration
plot(duration, num_hits, type="p", xlab="Total Time in Cache (min)", ylab="Number of Cache Hits")