package Cache;

import java.util.Comparator;
import java.util.List;

public abstract class CacheReplacement {

	protected Comparator compareFunc;

	public CacheItem minimum(CacheItem o1, CacheItem o2){
		if (compareFunc.compare(o1, o2) <=0 )
			return o1;
		else
			return o2;
	}

}
