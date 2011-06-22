package edu.ucsc.sil.fixcache.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import au.com.bytecode.opencsv.CSVWriter;

import edu.ucsc.sil.fixcache.util.Dates;

public class OutputManager {

    String outputDate;
    int outputSpacing = 3; // output the hit rate every 3 months
    int month = outputSpacing;
    boolean save;
    boolean filedistPrintMultiple; // whether the filedist output should happen once or more
    boolean headerPrinted;

    CSVWriter hitrateOutput; 
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
        
        try {
            boolean success = new File("results").mkdir();
        } catch (SecurityException e) {
            String msg = "Can't create results directory due to security.";
            System.err.println(msg);
            e.printStackTrace();
        }
    }
    
    private void writeComment(CSVWriter writer, String comment) {
        String[] line = {"# " + comment};
        writer.writeNext(line);
    }

    /**
     * Sets the filename and prints out header information
     * @param sim -- Simulator ran
     */
    private void printHeader(Simulator sim) {
        filename = sim.pid + "_" + sim.getCacheSize() + "_" + sim.blocksize + "_"
        + sim.prefetchsize + "_" + sim.cacheRep;
        
        try {
            hitrateOutput = new CSVWriter(
                new FileWriter("results/" + filename + "_hitrate.csv"), '\t');

            writeComment(hitrateOutput, "hitrate for every " +outputSpacing+ " months, "
                    + "used to describe the variation of hit rate with time");
            writeComment(hitrateOutput, "project: " + sim.pid + ", cachesize: "
                    + sim.getCacheSize() + ", blocksize: " + sim.blocksize
                    + ", prefetchsize: " + sim.prefetchsize
                    + ", cache replacement policy: " + sim.cacheRep);
            
            // Before refactoring, a column called "range" and one called
            // "numFiles" were here, but commented out
            String[] columns = {"Month", "HitRate", "NumCommits", "NumAdds", 
                "NumNewCacheItems", "NumBugFixes", "FilesProcessed"
            };
            
            hitrateOutput.writeNext(columns);
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
        outputFileDist(sim, true);
        outputFinalFileDist(sim);
        
        try {
            hitrateOutput.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

    /**
     * Outputs the hit rate to a csv file
     * @param cdate -- the current date
     * @param sim -- the simulator ran
     */
    private void outputHitRate(String cdate, Simulator sim) {
        // print out file distribution information at time slices
        if (filedistPrintMultiple) 
            outputFileDist(sim, false);

        // update the current outputDate
        if (!cdate.equals(sim.cache.endDate)) {
            outputDate = Dates.monthsLater(outputDate, outputSpacing);
        } else {
            outputDate = cdate; // = cache.endDate
        }

        // output!
        String[] record = {Integer.toString(month),
                           Double.toString(sim.getHitRate()),
                           Integer.toString(sim.resetCommitCount()),
                           Integer.toString(sim.cache.resetAddCount()),
                           Integer.toString(sim.cache.resetCICount()),
                           //also prints filecount at time slice, but query is not accurate
                           //csvWriter.write(Integer.toString(getFileCount(pid,cdate))); 
                           Integer.toString(sim.resetBugCount()),
                           Integer.toString(sim.resetFilesProcessedCount())
        };
        
        hitrateOutput.writeNext(record);
        
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
    private void outputFileDist(Simulator sim, boolean last) {
        
        // set up a new csvWriter
        String pathname;
        if (filedistPrintMultiple && !last)
            pathname = "results/" + month + "-" + filename + "_filedist.csv";
        else
            pathname = "results/" + filename + "_filedist.csv";        
        
        try {
            CSVWriter csv = new CSVWriter(new FileWriter(pathname), '\t');

            // write comments explaining file and setup
            writeComment(csv, "number of hit, misses and time stayed in Cache for every file");
            writeComment(csv, "project: " + sim.pid + ", cachesize: " + sim.getCacheSize()
                    + ", blocksize: " + sim.blocksize + ", prefetchsize: "
                    + sim.prefetchsize + ", cache replacement policy: " + sim.cacheRep);
            
            // column titles. "reason" used to be here, but was commented out.
            String columns[] = {"file_id", "file_name", "loc", "num_load", "num_hits",
                "num_misses", "duration"};
            
            csv.writeNext(columns);
            
            // initial row special to store total duration
            String initialRecord[] = {"0", "0", "0", "0", "0", "0", 
                Integer.toString(sim.cache.getTotalDuration())};
            
            csv.writeNext(initialRecord);
            
            // the file already has the correct header line
            // write out each record
            for (CacheItem ci : sim.cache.allCacheValues()){
                String[] record = {Integer.toString(ci.getFileId()),
                    ci.getFilePath(),
                    Integer.toString(ci.getLOC()), // max LOC
                    Integer.toString(ci.getLoadCount()),
                    Integer.toString(ci.getHitCount()),
                    Integer.toString(ci.getMissCount()),
                    Integer.toString(ci.getDuration())
                };
                    // csv.write(ci.getReason().toString());
                
                csv.writeNext(record);
            }

            // cleanup
            csv.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Output the information that is in the cache at the end
     * @param sim -- the simulator ran
     */
    private void outputFinalFileDist(Simulator sim) {
        try {
            CSVWriter csv = new CSVWriter(
              new FileWriter("results/" + filename + "_final.csv"));
            
            String columns[] = {"file_id", "file_name"};
                            
            csv.writeNext(columns);
            
            for (CacheItem ci : sim.cache) {
                String[] record = {Integer.toString(ci.getFileId()),
                                   ci.getFilePath()             
                };
                
                csv.writeNext(record);
            }
            
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