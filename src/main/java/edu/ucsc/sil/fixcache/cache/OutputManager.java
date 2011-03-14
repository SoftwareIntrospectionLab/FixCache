package edu.ucsc.sil.fixcache.cache;

import java.io.IOException;
import com.csvreader.CsvWriter;

import edu.ucsc.sil.fixcache.util.Dates;

public class OutputManager {

    String outputDate;
    int outputSpacing = 3; // output the hit rate every 3 months
    int month = outputSpacing;
    boolean save;
    boolean filedistPrintMultiple; // whether the filedist output should happen once or more
    boolean headerPrinted;

    CsvWriter hitrateOutput; 
    String filename;
   

    /**
     * Constructs a new output manager and starts printing
     * @param sim 
     * @param start -- the initial date 
     * @param save -- whether to save to a file
     * @param outputMulti -- whether to output multiple file distribution files
     */
    OutputManager(String start, boolean save, boolean outputMulti){
        this.save = save;
        outputDate = start;
        filedistPrintMultiple = outputMulti;
        headerPrinted = false;
    }

    /**
     * Sets the filename and prints out header information
     * @param sim -- Simulator ran
     */
    private void printHeader(Simulator sim) {
        filename = sim.pid + "_" + sim.getCacheSize() + "_" + sim.blocksize + "_"
        + sim.prefetchsize + "_" + sim.cacheRep;
        
        hitrateOutput = new CsvWriter("Results/" + filename + "_hitrate.csv");
        hitrateOutput.setComment('#');
        try {
            hitrateOutput.writeComment("hitrate for every " +outputSpacing+ " months, "
                    + "used to describe the variation of hit rate with time");
            hitrateOutput.writeComment("project: " + sim.pid + ", cachesize: "
                    + sim.getCacheSize() + ", blocksize: " + sim.blocksize
                    + ", prefetchsize: " + sim.prefetchsize
                    + ", cache replacement policy: " + sim.cacheRep);
            hitrateOutput.write("Month");
            //csvWriter.write("Range");
            hitrateOutput.write("HitRate");
            hitrateOutput.write("NumCommits");
            hitrateOutput.write("NumAdds");
            hitrateOutput.write("NumNewCacheItems");
            // csvWriter.write("NumFiles"); // uncomment if using findfilecountquery
            hitrateOutput.write("NumBugFixes");
            hitrateOutput.write("FilesProcessed");
            hitrateOutput.endRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
        headerPrinted = true;
    }
    
    /**
     * Public api for maybe printing
     * @param cdate -- current date
     * @param sim
     */
    public void manage(String cdate, Simulator sim){
        if (!save) return;
        if (Dates.getMonthDuration(outputDate, cdate) >= outputSpacing
                || cdate.equals(sim.cache.endDate)) {
            if (!headerPrinted) printHeader(sim);
            outputHitRate(cdate, sim);
        }                 
    }
    /**
     * Public api for last printing.
     * Do any last printing (e.g. the filedist information) and close connections
     * @param sim
     */
    public void finish(Simulator sim) {
        if (!save) return;
        outputFileDist(sim);
        hitrateOutput.close();
    }

    /**
     * Outputs the hit rate to a csv file
     * @param cdate -- the current date
     * @param sim -- the simulator ran
     */
    private void outputHitRate(String cdate, Simulator sim) {
        // print out file distribution information at time slices
        if (filedistPrintMultiple) 
            outputFileDist(sim);

        // update the current outputDate
        if (!cdate.equals(sim.cache.endDate)) {
            outputDate = Dates.monthsLater(outputDate, outputSpacing);
        } else {
            outputDate = cdate; // = cache.endDate
        }

        // output!
        try {
            hitrateOutput.write(Integer.toString(month));
            //csvWriter.write(Dates.getRange(formerOutputDate, outputDate));
            hitrateOutput.write(Double.toString(sim.getHitRate()));
            hitrateOutput.write(Integer.toString(sim.resetCommitCount()));
            hitrateOutput.write(Integer.toString(sim.cache.resetAddCount()));
            hitrateOutput.write(Integer.toString(sim.cache.resetCICount()));
            //also prints filecount at time slice, but query is not accurate
            //csvWriter.write(Integer.toString(getFileCount(pid,cdate))); 
            hitrateOutput.write(Integer.toString(sim.resetBugCount()));
            hitrateOutput.write(Integer.toString(sim.resetFilesProcessedCount()));
            hitrateOutput.endRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //advance current month marker;
        month += outputSpacing;
        
        // recurse until more commits occur
        if (Dates.getMonthDuration(outputDate, cdate) > outputSpacing){
            outputHitRate(cdate, sim);
        }
        
    }
    
    /**
     * output info about each file that was ever in the cache. 
     * @param sim -- the simulator ran
     */
    private void outputFileDist(Simulator sim) {
        
        // set up a new csvWriter
        String pathname;
        if (filedistPrintMultiple)
            pathname = "Results/" + month + "-" + filename + "_filedist.csv";
        else
            pathname = "Results/" + filename + "_filedist.csv";        
        CsvWriter csv = new CsvWriter(pathname);
        
        // character to proceed comment lines
        csv.setComment('#');
        
        try {
            // write comments explaining file and setup
            csv.writeComment("number of hit, misses and time stayed in Cache for every file");
            csv.writeComment("project: " + sim.pid + ", cachesize: " + sim.getCacheSize()
                    + ", blocksize: " + sim.blocksize + ", prefetchsize: "
                    + sim.prefetchsize + ", cache replacement policy: " + sim.cacheRep);
            
            // column titles
            csv.write("file_id");
            csv.write("loc");
            csv.write("num_load");
            csv.write("num_hits");
            csv.write("num_misses");
            csv.write("duration");
            // csv.write("reason");
            csv.endRecord();
            
            // initial row special to store total duration
            csv.write("0");
            csv.write("0");
            csv.write("0");
            csv.write("0");
            csv.write("0");
            csv.write(Integer.toString(sim.cache.getTotalDuration()));
            // csv.write("0");
            csv.endRecord();
            
            // the file already has the correct header line
            // write out each record
            for (CacheItem ci : sim.cache){
                csv.write((ci.getFileName()));
                csv.write(Integer.toString(ci.getLOC())); // max LOC
                csv.write(Integer.toString(ci.getLoadCount()));
                csv.write(Integer.toString(ci.getHitCount()));
                csv.write(Integer.toString(ci.getMissCount()));
                csv.write(Integer.toString(ci.getDuration()));
                // csv.write(ci.getReason().toString());
                csv.endRecord();
            }

            // cleanup
            csv.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Prints all the simulator settings to System.out
     * @param sim
     */
    protected static void printSummary(Simulator sim) {
        System.out.println("Simulator specs:");
        System.out.print("Project....");
        System.out.println(sim.pid);
        System.out.print("Cache size....");
        System.out.println(sim.getCacheSize());
        System.out.print("Blk size....");
        System.out.println(sim.blocksize);
        System.out.print("Prefetch size....");
        System.out.println(sim.prefetchsize);
        System.out.print("Start date....");
        System.out.println(sim.cache.startDate);
        System.out.print("End date....");
        System.out.println(sim.cache.endDate);
        System.out.print("Cache Replacement Policy ....");
        System.out.println(sim.cacheRep.toString());
        System.out.print("saving to file....");
        System.out.println(sim.output.save);


        System.out.println("\nResults:");

        System.out.print("Hit rate...");
        System.out.println(sim.getHitRate());

        System.out.print("Num commits processed...");
        System.out.println(sim.getTotalCommitCount());

        System.out.print("Num bug fixes...");
        System.out.println(sim.getHit() + sim.getMiss());
    }


}