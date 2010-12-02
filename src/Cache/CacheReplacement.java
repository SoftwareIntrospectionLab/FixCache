package Cache;

import java.util.Comparator;
import java.util.List;

public class CacheReplacement {

	public static enum Policy{LRU,BUGS,CHANGES,AUTHORS};
	static final CacheReplacement.Policy REPDEFAULT = CacheReplacement.Policy.LRU;


	protected Comparator compareFunc;
	
	public CacheReplacement(Policy p){
		switch (p){
		case BUGS: compareFunc = new ComparatorBug();break;
		case CHANGES: compareFunc = new ComparatorChange();break;
		case AUTHORS: compareFunc = new ComparatorAuthors();break;
		case LRU: compareFunc = new ComparatorLRU();
		}
	}

	public CacheItem minimum(CacheItem o1, CacheItem o2){
		if (compareFunc.compare(o1, o2) <=0 )
			return o1;
		else
			return o2;
	}

}
