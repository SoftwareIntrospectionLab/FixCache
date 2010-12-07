package Cache;

import java.util.Date;
import java.util.Comparator;

public class ComparatorLRU implements Comparator {


	public int compare(Object obj1, Object obj2) {
		CacheItem c1 = (CacheItem) obj1;
		CacheItem c2 = (CacheItem) obj2;

		//see bug #
		Date d1 = (Date) c1.getCachedDate();
		Date d2 = (Date) c2.getCachedDate();
		if(d1.equals(d2))
		{
			return 0;
		}
		else if(d1.before(d2))
		{
			return -1;
		}
		else 
		{
			return 1;
		}
	}
}
