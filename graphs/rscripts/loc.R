loc = read.csv("bugsvsloc")
loc = loc[(order(loc$numbugs)),]
plot(loc$numbugs, loc$maxloc, ylab = "LOC", xlab="Number of Bugs")

