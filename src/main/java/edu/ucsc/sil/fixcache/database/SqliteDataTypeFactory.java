package edu.ucsc.sil.fixcache.database;

import java.util.ArrayList;
import java.util.Collection;

import org.dbunit.dataset.datatype.DefaultDataTypeFactory;

public class SqliteDataTypeFactory extends DefaultDataTypeFactory {
    
    public SqliteDataTypeFactory() {
        super();
    }

    @Override
    public Collection getValidDbProducts() {
        ArrayList products = new ArrayList();
        products.add("SQLite");
        return products;
    }
}
    