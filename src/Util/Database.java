package Util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Database {
	
	public static int getIntResult (PreparedStatement intQuery) throws SQLException 
	{
		int res = -1;
		ResultSet r = intQuery.executeQuery();
		if (r.next()){
			res = r.getInt(1);
		}
		assert(!r.next());
		return res;
	}
	
	
	public static ArrayList<Integer> getIntArrayResult(PreparedStatement intArrayQuery) 
	throws SQLException
	{
		ArrayList<Integer> res = new ArrayList<Integer>();
		ResultSet r = intArrayQuery.executeQuery();
		while(r.next())
		{
			res.add(r.getInt(1));
		}
		return res;
	}
	
	public static String getStringResult (PreparedStatement stringQuery) throws SQLException 
	{
		String res = null;
		ResultSet r = stringQuery.executeQuery();
		if(r.next())
		{
			res = r.getString(1);
		}
		return res;
	}

}
