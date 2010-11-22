package Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

public class N_CacheReplacementBUG extends N_CacheReplacement {
	
    Hashtable ht = new Hashtable();
    List cacheList = new ArrayList();
    List outList = new ArrayList();
    N_CacheItem outItem = new N_CacheItem();
	
	public List compute_replacement_set(int outSize, N_Cache cache)
	{
		cacheList = cache.toList();        		
		Collections.sort(cacheList, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				N_CacheItem c1 = (N_CacheItem) obj1;
				N_CacheItem c2 = (N_CacheItem) obj2;

				if (c1.numberOfBugs==c2.numberOfBugs) {
					return c1.numberOfBugs - c2.numberOfBugs;
				}

				// cached date
				return c1.numberOfBugs-c2.numberOfBugs;
			}
		});
		
        for(int i=0;i<outSize;i++)
        {
        	outItem = (N_CacheItem)cacheList.get(i);
        	outList.add(outItem);
        }
        return outList;
	}
	

}