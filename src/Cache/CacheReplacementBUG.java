package Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

public class CacheReplacementBUG extends CacheReplacement {

	CacheReplacementBUG (){
		compareFunc = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				CacheItem c1 = (CacheItem) obj1;
				CacheItem c2 = (CacheItem) obj2;

				if (c1.numberOfBugs==c2.numberOfBugs) {
					return c1.numberOfBugs - c2.numberOfBugs;
				}

				// cached date
				return c1.numberOfBugs-c2.numberOfBugs;
			}
		};

	}

}