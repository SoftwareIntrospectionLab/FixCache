package Util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
