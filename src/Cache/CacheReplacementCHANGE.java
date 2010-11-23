package Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

public class CacheReplacementCHANGE extends CacheReplacement {

	
    CacheReplacementCHANGE (){
		compareFunc = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				CacheItem c1 = (CacheItem) obj1;
				CacheItem c2 = (CacheItem) obj2;

				 //see bug #
					return c1.numberOfChanges - c2.numberOfChanges;

			}
		};
    }

}
