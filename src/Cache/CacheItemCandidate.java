package Cache;

import java.util.Date;

import Cache.CacheItem.CacheReason;

public class CacheItemCandidate {
	
	public enum CacheCandidateReason{NewEntity, ModifiedEntity}
	private int fileId;
	private CacheCandidateReason casheCandidateReason;
	private int LOC;
	
	public CacheItemCandidate(int file_id, CacheCandidateReason cReason, int loc)
	{
		this.fileId = file_id;
		this.casheCandidateReason = cReason;
		this.LOC = loc;
	}
	public int getFileId()
	{
		return fileId;
	}
	
	
	public CacheCandidateReason getCacheCandidateReason()
	{
		return casheCandidateReason;
	}
	
	public int getLOC()
	{
		return LOC;
	}

}
