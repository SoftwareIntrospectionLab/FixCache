#!/usr/bin/env Rscript
# month vs hitrate
rates <- read.csv("apache.csv", comment.char="#")
maxmon = max(rates$Month)
#maxmon  = 36
plot(rates$Month, rates$HitRate, type="p", ylim=range(0,100), xlim=range(1,maxmon), xaxt="n", ylab="HitRate", xlab="Month")
axis(at=rates$Month, side=1)
#abline(h=max(HitRate), lty=2)
#abline(h=min(HitRate), lty=2)
#mtext(side=4, text=min(HitRate), las=1, at=min(HitRate))
#mtext(side=4, text=max(HitRate), las=1, at=max(HitRate))
par(new=T)
prates <- read.csv("postgres.csv", comment.char="#")
plot(prates$Month, prates$HitRate, type="p", pch=8, ylim=range(0,100), xlim=range(1,maxmon), xaxt="n", yaxt="n", xlab="", ylab="")

par(new=T)
vrates <- read.csv("volde.csv", comment.char="#")
plot(vrates$Month, vrates$HitRate, type="p", pch=3, ylim=range(0,100), xlim=range(1,maxmon), xaxt="n", yaxt="n", xlab="", ylab="")

par(new=T)
erates <- read.csv("v8.csv", comment.char="#")
plot(erates$Month, erates$HitRate, type="p", pch=2, ylim=range(0,100), xlim=range(1,maxmon), xaxt="n", yaxt="n", xlab="", ylab="")

legend(120, 25, legend=c("Apache","Postgres","Voldemort","V8"), pch=c(1,8,3,2))

