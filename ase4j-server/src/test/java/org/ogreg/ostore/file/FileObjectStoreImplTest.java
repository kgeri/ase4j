package org.ogreg.ostore.file;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.dynamo.DynamicObject;
import org.ogreg.common.dynamo.DynamicType;
import org.ogreg.common.nio.serializer.SerializerManager;

import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.ObjectStoreManager;

import org.ogreg.test.FileTestSupport;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * File based object store tests.
 *
 * @author  Gergely Kiss
 */
@SuppressWarnings("unchecked")
@Test(groups = "correctness")
public class FileObjectStoreImplTest {
    ObjectStoreManager config;
    ObjectStoreManager dynconfig;
    ObjectStore<TestData> store;
    ObjectStore<DynamicObject> dynstore;

    @BeforeMethod public void before() throws ObjectStoreException {
        config = new ObjectStoreManager();
        config.add("configuration/test-ostore.xml");

        dynconfig = new ObjectStoreManager();
        dynconfig.add("configuration/test-ostore-dynamo.xml");
    }

    /**
     * Tests the add operation with the object store.
     */
    @Test public void testAdd01() throws Exception {
        File dir = FileTestSupport.createTempDir("ostore");
        store = config.createStore("test", dir);

        TestData d1 = data(1, "abc", new Date(3));
        TestData d2 = data(2, "def", new Date(5));
        TestData d3 = data(3, null, new Date(5));

        store.add(1, d1);
        store.add(2, d2);
        store.add(3, d3);

        assertEquals(store.get(1), d1);
        assertEquals(store.get(2), d2);
        assertEquals(store.get(3), d3);

        // Testing updates
        d1 = data(1, "abcdef", new Date(5));
        d2 = data(2, "ghi", new Date(5));

        store.add(1, d1);
        store.add(2, d2);

        assertEquals(store.get(1), d1);
        assertEquals(store.get(2), d2);

        // Testing store reopen
        ((Flushable) store).flush();
        store = config.createStore("test", dir);
    }

    /**
     * Tests the add operation with the dynamic object store.
     */
    @Test public void _testAdd02() throws Exception {
        File dir = FileTestSupport.createTempDir("ostoredyn");
        dynstore = dynconfig.createStore("test", dir);

        DynamicObject d1 = dyndata(1, "abc", new Date(3));
        DynamicObject d2 = dyndata(2, "def", new Date(5));
        DynamicObject d3 = dyndata(3, null, new Date(5));

        dynstore.add(1, d1);
        dynstore.add(2, d2);
        dynstore.add(3, d3);

        assertEquals(dynstore.get(1), d1);
        assertEquals(dynstore.get(2), d2);
        assertEquals(dynstore.get(3), d3);

        // Testing updates
        d1 = dyndata(1, "abcdef", new Date(5));
        d2 = dyndata(2, "ghi", new Date(5));

        dynstore.add(1, d1);
        dynstore.add(2, d2);

        assertEquals(dynstore.get(1), d1);
        assertEquals(dynstore.get(2), d2);

        // Testing store reopen
        ((Flushable) dynstore).flush();
        dynstore = dynconfig.createStore("test", dir);
    }

    /**
     * Tests the put operation with the object store (and also the business key usage).
     */
    @Test public void testPut01() throws Exception {
        store = config.createStore("test", FileTestSupport.createTempDir("ostore"));

        TestData d1 = data(1, "abc", new Date(3));
        TestData d2 = data(2, "def", new Date(5));
        TestData d3 = data(3, null, new Date(5));

        store.save(d1);
        store.save(d2);
        store.add(3, d3);

        assertEquals(store.get(1), d1);
        assertEquals(store.get(2), d2);
        assertEquals(store.get(3), d3);

        // Testing updates
        d1 = data(5, "abc", new Date(5));
        d2 = data(2, "ghi", new Date(5));

        store.saveOrUpdate(d1);

        long d2key = store.saveOrUpdate(d2);

        assertEquals(store.get(1), d1);
        assertEquals(store.get(d2key), d2);

        // Testing flush
        ((Flushable) store).flush();
    }

    /**
     * Tests the put operation with extended objects in the object store.
     */
    @Test public void testPut02() throws Exception {
        File dir = FileTestSupport.createTempDir("ostore");
        store = config.createStore("test", dir);

        TestData d1 = data(1, "abc", new Date(3));
        d1.putExt("EXT1", "aaa");

        store.save(d1);

        assertEquals(store.get(1).getExt("EXT1"), "aaa");
        assertEquals(store.get(1).getExt("EXT2"), null);

        d1.putExt("EXT2", new Date(0));

        // Testing updates
        store.saveOrUpdate(d1);

        assertEquals(store.get(1).getExt("EXT1"), "aaa");
        assertEquals(store.get(1).getExt("EXT2"), new Date(0));

        // Testing flush and close
        ((Flushable) store).flush();
        ((Closeable) store).close();

        store = config.createStore("test", dir);

        assertEquals(store.get(1).getExt("EXT1"), "aaa");
        assertEquals(store.get(1).getExt("EXT2"), new Date(0));
    }

    /**
     * Tests the put operation with dynamic objects in the object store.
     */
    @Test public void testPut03() throws Exception {
        dynstore = dynconfig.createStore("test", FileTestSupport.createTempDir("ostoredyn"));

        DynamicObject d1 = dyndata(1, "abc", new Date(3));
        DynamicObject d2 = dyndata(2, "def", new Date(5));
        DynamicObject d3 = dyndata(3, null, new Date(5));

        dynstore.save(d1);
        dynstore.save(d2);
        dynstore.add(3, d3);

        assertEquals(dynstore.get(1), d1);
        assertEquals(dynstore.get(2), d2);
        assertEquals(dynstore.get(3), d3);

        // Testing updates
        d1 = dyndata(5, "abc", new Date(5));
        d2 = dyndata(2, "ghi", new Date(5));

        dynstore.saveOrUpdate(d1);

        long d2key = dynstore.saveOrUpdate(d2);

        assertEquals(dynstore.get(1), d1);
        assertEquals(dynstore.get(d2key), d2);

        // Testing flush
        ((Flushable) dynstore).flush();
    }

    /**
     * Tests the getField operation with static and extended objects.
     */
    @Test public void testGetField01() throws Exception {
        File dir = FileTestSupport.createTempDir("ostore");
        store = config.createStore("test", dir);

        TestData d1 = data(1, "abc", new Date(3));
        d1.putExt("EXT1", "aaa");

        store.save(d1);

        assertEquals(store.getField(1, "url"), "abc");
        assertEquals(store.getField(1, "extensions.EXT1"), "aaa");
    }

    /**
     * Tests some corner cases (for coverage).
     */
    @Test public void testCornerCases01() throws Exception {

        // No business key (though needed for save)
        try {
            ObjectStore<Object> badStore = config.createStore("error",
                    FileTestSupport.createTempDir("ostore"));
            badStore.save(new Object());
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        // Empty business key
        try {
            TestData d3 = data(3, null, new Date(5));
            store.save(d3);
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }

        // Non-unique field query
        try {
            store.uniqueResult("id", 1);
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }

        // Unconfigured type
        try {
            config.createStore("missing", FileTestSupport.createTempDir("ostore"));
            fail("Expected ConfigurationException");
        } catch (ConfigurationException e) {
        }

        // Incompatible PropertyStore
        File file = FileTestSupport.createTempFile("incompatibleProp");

        FilePropertyStore<String> ps1 = new FilePropertyStore<String>();
        ps1.setType(String.class);
        ps1.setSerializer(SerializerManager.findSerializerFor(String.class));
        ps1.open(file);
        ps1.flush();
        ps1.close();

        FilePropertyStore<Date> ps2 = new FilePropertyStore<Date>();
        ps2.setType(Date.class);

        try {
            ps2.open(file);
            fail("Expected IOException");
        } catch (IOException e) {
        }
    }

    TestData data(int id, String url, Date added) {
        TestData td = new TestData();
        td.id = id;
        td.url = url;
        td.added = added;

        return td;
    }

    DynamicObject dyndata(int id, String url, Date added) {
        Map<String, Class<?>> fields = new LinkedHashMap<String, Class<?>>();
        fields.put("url", String.class);
        fields.put("id", Integer.class);
        fields.put("added", Date.class);
        fields.put("extensions", Map.class);

        DynamicObject td = new DynamicObject(new DynamicType(fields));

        try {
            td.safeSet("id", id);
            td.safeSet("url", url);
            td.safeSet("added", added);
            td.safeSet("extensions", new HashMap<String, Object>());
        } catch (IllegalAccessException e) {
            fail("Failed to initialize test data", e);
        }

        return td;
    }

    // Test data type
    static class TestData {
        String url;

        int id;
        Date added;
        Map<String, Object> extensions = new HashMap<String, Object>();

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result) + ((added == null) ? 0 : added.hashCode());
            result = (prime * result) + id;
            result = (prime * result) + ((url == null) ? 0 : url.hashCode());

            return result;
        }

        public void putExt(String key, Object value) {
            extensions.put(key, value);
        }

        public Object getExt(String key) {
            return extensions.get(key);
        }

        @Override public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }

            TestData other = (TestData) obj;

            if (added == null) {

                if (other.added != null) {
                    return false;
                }
            } else if (!added.equals(other.added)) {
                return false;
            }

            if (id != other.id) {
                return false;
            }

            if (url == null) {

                if (other.url != null) {
                    return false;
                }
            } else if (!url.equals(other.url)) {
                return false;
            }

            return true;
        }
    }
}
