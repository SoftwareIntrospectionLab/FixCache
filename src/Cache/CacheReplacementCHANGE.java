package Cache;

import java.util.Comparator;

public class CacheReplacementCHANGE implements Comparator {


	public int compare(Object obj1, Object obj2) {
		CacheItem c1 = (CacheItem) obj1;
		CacheItem c2 = (CacheItem) obj2;

		//see bug #
		return c1.getNumberOfChanges() - c2.getNumberOfChanges();

	}

}
