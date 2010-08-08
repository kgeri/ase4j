package org.ogreg.ase4j;

import java.util.Date;


public class TestData {
    public String url;
    public Date created;
    public long length;

    public static TestData data(String url, Date created, long length) {
        TestData td = new TestData();
        td.url = url;
        td.created = created;
        td.length = length;

        return td;
    }
}
