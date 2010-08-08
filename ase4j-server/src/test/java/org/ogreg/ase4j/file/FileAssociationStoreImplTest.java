package org.ogreg.ase4j.file;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.TestData;
import static org.ogreg.ase4j.TestData.*;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.Restrictions;

import org.ogreg.common.nio.NioUtils;
import org.ogreg.common.utils.SerializationUtils;

import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.memory.StringStore;

import org.ogreg.test.FileTestSupport;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Tests for the file based association store.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "correctness")
public class FileAssociationStoreImplTest {
    StringStore sstore;
    ObjectStore<TestData> ostore;
    FileAssociationStoreImpl<String, String> simpleStore;
    FileAssociationStoreImpl<String, TestData> objStore;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {

        // Simple string based store
        sstore = new StringStore();
        sstore.init(null, null, new HashMap<String, String>());

        // Object based store
        ObjectStoreManager cfg = new ObjectStoreManager();
        cfg.add("configuration/test-ostore.xml");

        ostore = cfg.getStore("testAssocs", FileTestSupport.createTempDir("store"));
    }

    @AfterMethod public void tearDown() {
        NioUtils.closeQuietly(simpleStore);
        NioUtils.closeQuietly(objStore);
    }

    /**
     * Tests opening an empty store.
     */
    public void testLoad01() {
        File tf = FileTestSupport.createTempFile("assocs.idx");
        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.setStorageFile(tf);
        simpleStore.init();

        assertTrue(tf.exists());
        assertEquals(tf.length(), 8204);
    }

    /**
     * Tests inserts, opening an existing store and does some searching.
     */
    public void testInsert01() throws Exception {
        File tf = FileTestSupport.createTempFile("assocs.idx");
        File indexFile = FileTestSupport.createTempFile("index.idx");

        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.setStorageFile(tf);
        simpleStore.init();

        simpleStore.add("a", "b", 1.0F);
        simpleStore.add("b", "c", 0.5F);
        simpleStore.add("c", "d", 0.1F);

        // Flushing
        simpleStore.flush();

        // Closing
        simpleStore.close();
        SerializationUtils.write(indexFile, sstore);

        // Reopening
        sstore = SerializationUtils.read(indexFile, StringStore.class);
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.init();

        List<Association<String, String>> l = simpleStore.query(
                new Query(
                    Restrictions.or(Restrictions.phrase("a"), Restrictions.phrase("b"),
                        Restrictions.phrase("c"), Restrictions.phrase("d"))).limit(10));
        Collections.sort(l);

        assertEquals(l.size(), 3);
        assertEquals(l.get(0).value, 1.0F);
        assertEquals(l.get(1).value, 0.5F);
        assertEquals(l.get(2).value, 0.1F);
    }

    /**
     * Tests mass inserts.
     */
    public void testInsert02() throws Exception {
        File tf = FileTestSupport.createTempFile("assocs.idx");

        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.setStorageFile(tf);
        simpleStore.init();

        Collection<Association<String, String>> as = new LinkedList<Association<String, String>>();
        as.add(assoc("a", "b", 1.0F));
        as.add(assoc("a", "d", 2.0F));
        as.add(assoc("a", "e", 0.5F));
        as.add(assoc("b", "d", 1.0F));

        simpleStore.addAll(as);

        List<Association<String, String>> l1 = simpleStore.query(new Query(
                    Restrictions.phrase("a")).limit(10));
        Collections.sort(l1);

        assertEquals(l1.size(), 3);
        assertEquals(l1.get(0).value, 2.0F);
        assertEquals(l1.get(1).value, 1.0F);
        assertEquals(l1.get(2).value, 0.5F);

        as.add(assoc("e", "X", 5.0F));
        simpleStore.addAll(as, "f");

        List<Association<String, String>> l2 = simpleStore.query(new Query(
                    Restrictions.phrase("e")).limit(10));

        assertEquals(l2.get(0).value, 5.0F);
        assertEquals(l2.get(0).to, "f");
    }

    /**
     * Tests various searches.
     */
    public void testQuery01() throws Exception {
        File tf = FileTestSupport.createTempFile("assocs.idx");

        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.setStorageFile(tf);
        simpleStore.init();

        Collection<Association<String, String>> as = new LinkedList<Association<String, String>>();
        as.add(assoc("a", "b", 1.0F));
        as.add(assoc("a", "d", 0.5F));
        as.add(assoc("a", "e", 0.1F));
        as.add(assoc("b", "d", 1.0F));
        as.add(assoc("c", "e", 0.1F));

        simpleStore.addAll(as);

        List<Association<String, String>> l;

        // Simple AND query
        l = simpleStore.query(
                new Query(Restrictions.and(Restrictions.phrase("a"), Restrictions.phrase("b")))
                .limit(10));
        assertEquals(l.size(), 1);
        assertEquals(l.get(0).to, "d");
        assertEquals(l.get(0).value, 0.75F);

        // Simple AND query with limit
        l = simpleStore.query(new Query(Restrictions.phrase("a")).limit(3));
        assertEquals(l.size(), 3);
        l = simpleStore.query(new Query(Restrictions.phrase("a")).limit(1));
        assertEquals(l.size(), 1);

        // Testing negation 1
        l = simpleStore.query(new Query(
                    Restrictions.and(Restrictions.phrase("a"), Restrictions.not("b"))));
        Collections.sort(l);

        assertEquals(l.size(), 2);
        assertEquals(l.get(0).to, "b");
        assertEquals(l.get(1).to, "e");

        // Testing negation 2
        l = simpleStore.query(new Query(
                    Restrictions.and(Restrictions.not("a"), Restrictions.phrase("b"))));
        assertEquals(l.size(), 0);

        // Testing double negation
        l = simpleStore.query(new Query(Restrictions.not(Restrictions.not("a"))));
        assertEquals(l.size(), 3);
    }

    /**
     * Tests filtered searches.
     */
    public void testQuery02() throws Exception {
        File tf = FileTestSupport.createTempFile("assocs.idx");

        objStore = new FileAssociationStoreImpl<String, TestData>();
        objStore.setFromStore(sstore);
        objStore.setToStore(ostore);
        objStore.setStorageFile(tf);
        objStore.init();

        Collection<Association<String, TestData>> as =
            new LinkedList<Association<String, TestData>>();
        as.add(assoc("a", data("bbb", new Date(), 10), 1.0F));
        as.add(assoc("a", data("ccc", new Date(0), 20), 0.5F));
        as.add(assoc("a", data("ddd", new Date(0), 30), 0.1F));
        as.add(assoc("b", data("ddd", new Date(), 40), 1.0F));

        objStore.addAll(as);

        List<Association<String, TestData>> l;

        // Testing EQ
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.eq("url", "bbb")));
        assertEquals(l.size(), 1);
        assertEquals(l.get(0).to.url, "bbb");
        assertEquals(l.get(0).value, 1.0F);

        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.eq("created", new Date(0))));
        assertEquals(l.size(), 1);
        assertEquals(l.get(0).to.url, "ccc");

        // Testing NE
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.ne("url", "bbb")));
        assertEquals(l.size(), 2);

        // Testing GT
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.gt("length", 20L)));
        assertEquals(l.size(), 1);

        // Testing GE
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.ge("length", 20L)));
        assertEquals(l.size(), 2);

        // Testing LT
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.lt("length", 20L)));
        assertEquals(l.size(), 1);

        // Testing LE
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.le("length", 20L)));
        assertEquals(l.size(), 2);

        // Testing regex matcher
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.matches("url", Pattern.compile(".*?b.*"))));
        assertEquals(l.size(), 1);
        assertEquals(l.get(0).to.url, "bbb");

        // Testing a complex filter
        l = objStore.query(new Query(Restrictions.phrase("a")).filter(
                    Restrictions.and(Restrictions.eq("url", "bbb"),
                        Restrictions.gt("length", 5L))));
        assertEquals(l.size(), 1);
        assertEquals(l.get(0).to.url, "bbb");
    }

    /**
     * Tests invalid queries.
     */
    public void testInvalidQuery01() throws Exception {
        File tf = FileTestSupport.createTempFile("assocs.idx");

        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.setFromStore(sstore);
        simpleStore.setToStore(sstore);
        simpleStore.setStorageFile(tf);
        simpleStore.init();

        // Invalid query (AND result set too large)
        try {
            simpleStore.query(new Query(
                    Restrictions.and(Restrictions.not("a"), Restrictions.not("b"))));
            fail("Expected QueryExecutionException");
        } catch (QueryExecutionException e) {
        }

        // Invalid query (OR result set too large)
        try {
            simpleStore.query(new Query(
                    Restrictions.or(Restrictions.not("a"), Restrictions.not("b"))));
            fail("Expected QueryExecutionException");
        } catch (QueryExecutionException e) {
        }

        // Invalid query (no select part)
        try {
            simpleStore.query(new Query(null));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests the store for error handling.
     */
    public void testErrors() {

        try {

            // From store must be set
            simpleStore = new FileAssociationStoreImpl<String, String>();
            simpleStore.init();
            fail("Expected AssertionError");
        } catch (AssertionError e) {
        }

        try {

            // To store must be set
            simpleStore = new FileAssociationStoreImpl<String, String>();
            simpleStore.setFromStore(sstore);
            simpleStore.init();
            fail("Expected AssertionError");
        } catch (AssertionError e) {
        }

        // For coverage
        simpleStore = new FileAssociationStoreImpl<String, String>();
        simpleStore.getFromStore();
        simpleStore.getToStore();
        simpleStore.getStorageFile();
    }

    <T> Association<String, T> assoc(String from, T to, float value) {
        return new Association<String, T>(from, to, value);
    }
}
