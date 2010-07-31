package org.ogreg.ostore;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.test.FileTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * File based object store tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class ObjectStoreImplTest {
	Configuration config;
	ObjectStore<TestData> store;

	@BeforeMethod
	public void before() throws ObjectStoreException {
		config = new Configuration();
		config.add("configuration/test-ostore.xml");
	}

	@AfterMethod
	public void tearDown() throws IOException {
		if (store != null) {
			((Closeable) store).close();
		}
	}

	/**
	 * Tests the add operation with the object store.
	 */
	@Test
	public void testAdd01() throws Exception {
		File dir = FileTestSupport.createTempDir("ostore");
		store = config.createStore(TestData.class, dir);

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
		store = config.createStore(TestData.class, dir);
	}

	/**
	 * Tests the put operation with the object store (and also the business key
	 * usage).
	 */
	@Test
	public void testPut01() throws Exception {
		store = config.createStore(TestData.class, FileTestSupport.createTempDir("ostore"));

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
	 * Tests the put operation with dynamic objects in the object store.
	 */
	@Test
	public void testPut02() throws Exception {
		File dir = FileTestSupport.createTempDir("ostore");
		store = config.createStore(TestData.class, dir);

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

		store = config.createStore(TestData.class, dir);

		assertEquals(store.get(1).getExt("EXT1"), "aaa");
		assertEquals(store.get(1).getExt("EXT2"), new Date(0));
	}

	/**
	 * Tests the getField operation with static and dynamic objects.
	 */
	@Test
	public void testGetField01() throws Exception {
		File dir = FileTestSupport.createTempDir("ostore");
		store = config.createStore(TestData.class, dir);

		TestData d1 = data(1, "abc", new Date(3));
		d1.putExt("EXT1", "aaa");

		store.save(d1);

		assertEquals(store.getField(1, "url"), "abc");
		assertEquals(store.getField(1, "extensions.EXT1"), "aaa");
	}

	/**
	 * Tests some corner cases (for coverage).
	 */
	@Test
	public void testCornerCases01() throws Exception {
		// No business key (though needed for save)
		try {
			ObjectStore<Object> badStore = config.createStore(Object.class,
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
			config.createStore(Date.class, FileTestSupport.createTempDir("ostore"));
			fail("Expected ObjectStoreException");
		} catch (ObjectStoreException e) {
		}

		// Incompatible PropertyStore
		File file = FileTestSupport.createTempFile("incompatibleProp");

		PropertyStore<String> ps1 = new PropertyStore<String>();
		ps1.setType(String.class);
		ps1.setSerializer(SerializerManager.findSerializerFor(String.class));
		ps1.open(file);
		ps1.flush();
		ps1.close();

		PropertyStore<Date> ps2 = new PropertyStore<Date>();
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

	// Test data type
	static class TestData {
		String url;

		int id;
		Date added;
		Map<String, Object> extensions = new HashMap<String, Object>();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((added == null) ? 0 : added.hashCode());
			result = prime * result + id;
			result = prime * result + ((url == null) ? 0 : url.hashCode());
			return result;
		}

		public void putExt(String key, Object value) {
			extensions.put(key, value);
		}

		public Object getExt(String key) {
			return extensions.get(key);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestData other = (TestData) obj;
			if (added == null) {
				if (other.added != null)
					return false;
			} else if (!added.equals(other.added))
				return false;
			if (id != other.id)
				return false;
			if (url == null) {
				if (other.url != null)
					return false;
			} else if (!url.equals(other.url))
				return false;
			return true;
		}
	}
}
