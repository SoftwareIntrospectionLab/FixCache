#!/usr/bin/env Rscript
# duration vs. hits
filedist <- read.csv("filedistfile", comment.char="#")
filedist$duration <- (filedist$duration)/10080 # 7 * 24 * 60 = days
filedist <- filedist[-c(1),]
filedist <- filedist[order(filedist$duration), ]
attach(filedist)
# sort based on duration
plot(duration, num_hits, type="p", xlab="Total Time in Cache (weeks)", ylab="Number of Cache Hits")