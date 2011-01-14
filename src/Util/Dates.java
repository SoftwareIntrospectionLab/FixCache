package Util;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

public class Dates {

	// XXX: what is the unit of return value
	static int TimeDifference(String date1, String date2){
		// parse the two dates to figure out how many hours (?) are between them
		DateTime start = null;
		DateTime end = null;
	    return Minutes.minutesBetween(start, end).getMinutes();
	}
	
	
}
