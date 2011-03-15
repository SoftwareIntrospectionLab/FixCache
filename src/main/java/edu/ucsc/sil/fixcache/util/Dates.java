package edu.ucsc.sil.fixcache.util;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import java.lang.StringBuilder;

public class Dates {

    static StringBuilder range;
    public enum dbtype{mysql, sqlite};

    static final DateTimeFormatter mysqlfmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");
    static final DateTimeFormatter sqlitefmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    
    private static DateTimeFormatter fmt;

    public static void initializeFormat(dbtype type){
        if(fmt==null) {
            switch (type){
            case mysql: fmt = mysqlfmt;break;
            case sqlite: fmt = sqlitefmt;break;      
            }  
        }
    }


    // must first call initializeFormat
    public static DateTime toDateTime(String date){
        assert (fmt != null);
        return fmt.parseDateTime(date);
    }

    // must first call initializeFormat
    public static String toString(DateTime date)
    {
        assert (fmt != null);
        return date.toString(fmt);
    }

    public static int getMinuteDuration(String start, String end)
    {
        return Minutes.minutesBetween(toDateTime(start), toDateTime(end)).getMinutes();
    }

    public static int getMonthDuration(String start, String end)

    {
        int mo =  Months.monthsBetween(toDateTime(start), toDateTime(end)).getMonths();
        return mo;
    }

    public static int getWeekDuration(String start, String end)
    {
        return Weeks.weeksBetween(toDateTime(start), toDateTime(end)).getWeeks();
    }


    public static String monthsLater(String start, int numMonths)
    {
        return toString(toDateTime(start).plusMonths(numMonths));
    }

    public static String weeksLater(String start, int numMonths)
    {
        return toString(toDateTime(start).plusWeeks(numMonths));
    }

    public static void main(String args[])
    {
        DateTime d = new DateTime("2000-05-04T01:02:03");
        System.out.print(toString(d));
    }

    public static String getRange(String rangeStart, String rangeEnd) {
        DateTime start = toDateTime(rangeStart);
        DateTime end = toDateTime(rangeEnd);
        range = new StringBuilder();
        range.append(start.getYear());
        range.append(".");
        range.append(start.getMonthOfYear());
        range.append(".");
        range.append(start.getDayOfMonth());
        range.append("~");
        range.append(end.getYear());
        range.append(".");
        range.append(end.getMonthOfYear());
        range.append(".");
        range.append(end.getDayOfMonth());
        return range.toString();
    }
}
