package Cache;

import java.util.Comparator;

public class ComparatorBug implements Comparator {

	public int compare(Object obj1, Object obj2) {
		CacheItem c1 = (CacheItem) obj1;
		CacheItem c2 = (CacheItem) obj2;
        //question???
		if (c1.getNumberOfBugs() ==c2.getNumberOfBugs()) {
			return c1.getNumberOfBugs() - c2.getNumberOfBugs();
		}

		// cached date
		return c1.getNumberOfBugs()-c2.getNumberOfBugs();
	}


}