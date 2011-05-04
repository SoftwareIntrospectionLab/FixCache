package edu.ucsc.sil.fixcache.database;

import java.util.ArrayList;
import java.util.Collection;

import org.dbunit.dataset.datatype.DefaultDataTypeFactory;

public class SqliteDataTypeFactory extends DefaultDataTypeFactory {
    
    public SqliteDataTypeFactory() {
        super();
    }

    public Collection<String> getValidDbProducts() {
        ArrayList<String> products = new ArrayList<String>();
        products.add("SQLite");
        return products;
    }
}
    