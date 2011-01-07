package Cache;

import java.util.Date;
import java.util.Comparator;

public class ComparatorLRU implements Comparator<CacheItem> {

	public int compare(CacheItem c1, CacheItem c2) {
		Date d1 = c1.getCachedDate();
		Date d2 = c2.getCachedDate();
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
