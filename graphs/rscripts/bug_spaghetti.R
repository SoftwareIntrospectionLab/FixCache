# files names must be numbers with no extension
# must be in current directory
# /bin/ls | sed s/\.csv// | gawk '{print "mv " $1 ".csv " $1}' | bash
#     new=`echo $file | sed 's/-.*\./\./'`
require(lattice)
full <- c()
a <- list.files()
for (x in a) {
  u<-read.csv(x, header = T, comment.char='#')
  u$month = x
  u$month <- as.numeric(u$month)
  u$bugs = u$num_misses + u$num_hits
  u <- u[order(u$bugs, decreasing=T), ]
  ranktmp = seq(along=u$month)
  u$rank = pmin(ranktmp * u$bugs, ranktmp)
  full <- rbind(full, u)
}
full = full[(full$rank != 0),]
full = full[(order(full$month)),]
#top = max(full[(full$month == max(full$month)),]$rank) * .05
top = 30
full = full[(full$rank <= top),]
xyplot(rank~month, groups=file_id, data= full, type="o", ylim=c(max(full$rank)+1, 0), xlim=c(min(full$month), max(full$month)))