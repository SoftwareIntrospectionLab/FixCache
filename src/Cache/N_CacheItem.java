package Cache;

import java.util.Date;
import java.util.List;

public class N_CacheItem {
	static final int BUG_FETCH = 1;

	static final int NEW_FETCH = 2;

	static final int CHANGE_FETCH = 3;

	static final int SPATIAL_FETCH = 4;

	static final int INIT_FETCH = 5;

	static final String cacheReason[] = { "Unknown", "Bug entity",
			"New entity", "Changed entity", "Co-changed entity",
			"Initial fetched" };

	String entityId;
	
	int loadType;

	Date loadDate;
	
	int LOC;

	int numberOfChanges;

	int numberOfBugs;
	
	int numberOfAuthors;

	//List coChanges;
	
	public N_CacheItem(){}
	/**
	 * @return Returns the entityId.
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * @param cachedDate
	 *            The cachedDate to set.
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	
	/**
	 * @return Returns the cachedDate.
	 */
	public Date getCachedDate() {
		return loadDate;
	}

	/**
	 * @param cachedDate
	 *            The cachedDate to set.
	 */
	public void setCachedDate(Date cachedDate) {
		this.loadDate = cachedDate;
	}


	/**
	 * @return Returns the LOC.
	 */
	public String getLOC() {
		return entityId;
	}

	/**
	 * @param LOC
	 *            The LOC to set.
	 */
	public void setLOC(int LOC) {
		this.LOC = LOC;
	}
	
	/**
	 * @return Returns the numberOfChanges.
	 */
	public int getNumberOfChanges() {
		return numberOfChanges;
	}

	/**
	 * @param numberOfChanges
	 *            The numberOfChanges to set.
	 */
	public void setNumberOfChanges(int numberOfChanges) {
		this.numberOfChanges = numberOfChanges;
	}
	

	/**
	 * @return Returns the numberOfBugs.
	 */
	public int getNumberOfBugs() {
		return numberOfBugs;
	}

	/**
	 * @param numberOfBugs
	 *            The numberOfBugs to set.
	 */
	public void setNumberOfBugs(int numberOfBugs) {
		this.numberOfBugs = numberOfBugs;
	}
	
	/**
	 * @return Returns the numberOfAuthors
	 */
	public int getNumberOfAuthors() {
		return numberOfBugs;
	}

	/**
	 * @param numberOfauthors
	 *            The numberOAuthors to set.
	 */
	public void setNumberOfAuthors(int numberOfAuthors) {
		this.numberOfAuthors = numberOfAuthors;
	}

//	/**
//	 * @return Returns the coChanges.
//	 */
//	public List getCochanges() {
//		return coChanges;
//	}

	/**
	 * @param coChanges
	 *            The coChanges to set.
	 */
//	public void setCoChanges(List coChanges) {
//		this.coChanges = coChanges;
//	}

}
