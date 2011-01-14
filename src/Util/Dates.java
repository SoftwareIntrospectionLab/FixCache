package Util;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Dates {
    
    static final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");

	public static DateTime toDateTime(String date){
	    
	    return fmt.parseDateTime(date);
	}
	
	public static String toString(DateTime date)
	{
	    return date.toString(fmt);
	}

	public static int getDuration(String start, String end)
	{
	    
	    return Minutes.minutesBetween(toDateTime(start), toDateTime(end)).getMinutes();
	}
	
	public static void main(String args[])
	{
	    DateTime d = new DateTime("2000-05-04T01:02:03");
	    System.out.print(toString(d));
	}
}
