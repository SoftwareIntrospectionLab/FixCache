package Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

public class N_CacheReplacementCHANGE extends N_CacheReplacement {
	
    Hashtable ht = new Hashtable();
    ArrayList cacheList = new ArrayList();
    ArrayList outList = new ArrayList();
    N_CacheItem outItem = new N_CacheItem();
	
	public ArrayList compute_replacement_set(int outSize, N_Cache cache)
	{
		cacheList = cache.toList();        		
		Collections.sort(cacheList, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				N_CacheItem c1 = (N_CacheItem) obj1;
				N_CacheItem c2 = (N_CacheItem) obj2;

				 //see bug #
					return c1.numberOfChanges - c2.numberOfChanges;

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
