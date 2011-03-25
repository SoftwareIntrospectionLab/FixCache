locfile = read.csv("filedistfile", comment.char='#')
locfile <- locfile[-c(1),]
locfile$bugfixes = locfile$num_hits + locfile$num_misses
#locfile = locfile[(order(locfile$loc)), decreasing=T]
#locfile = locfile[-1,]
locfile = locfile[(order(locfile$bugfixes)),]
plot(locfile$bugfixes, locfile$loc, ylab = "LOC", xlab="Number of Bugs")

