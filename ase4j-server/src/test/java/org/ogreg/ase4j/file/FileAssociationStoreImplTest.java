package org.ogreg.ase4j.file;

import static org.ogreg.ase4j.TestData.data;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.ase4j.Params;
import org.ogreg.ase4j.TestData;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.Restrictions;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.memory.StringStore;
import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the file based association store.
 * 
 * @author Gergely Kiss
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
		File dir = FileTestSupport.createTempDir("sstore");
		sstore = new StringStore();
		sstore.init(null, dir, new HashMap<String, String>());

		// Object based store
		ObjectStoreManager cfg = new ObjectStoreManager();
		cfg.add("configuration/test-ostore.xml");

		ostore = cfg.getStore("testAssocs", FileTestSupport.createTempDir("store"));
	}

	@AfterMethod
	public void tearDown() {
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
		assertEquals(tf.length(), 8212);
	}

	/**
	 * Tests inserts, opening an existing store and does some searching.
	 */
	public void testInsert01() throws Exception {
		File tf = FileTestSupport.createTempFile("assocs.idx");

		simpleStore = new FileAssociationStoreImpl<String, String>();
		simpleStore.setFromStore(sstore);
		simpleStore.setToStore(sstore);
		simpleStore.setStorageFile(tf);
		simpleStore.init();

		simpleStore.add("a", "b", 1.0F, null);
		simpleStore.add("b", "c", 0.5F, null);
		simpleStore.add("c", "d", 0.1F, null);

		// Flushing
		simpleStore.flush();
		sstore.flush();

		// Closing
		simpleStore.close();
		sstore.close();

		// Reopening
		File dir = new File("target/sstore");
		sstore = new StringStore();
		sstore.init(null, dir, new HashMap<String, String>());
		simpleStore.setFromStore(sstore);
		simpleStore.setToStore(sstore);
		simpleStore.init();

		List<Association<String, String>> l = simpleStore.query(new Query(Restrictions.or(
				Restrictions.phrase("a"), Restrictions.phrase("b"), Restrictions.phrase("c"),
				Restrictions.phrase("d"))).limit(10));
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

		simpleStore.addAll(as, null);
		simpleStore.flush();

		List<Association<String, String>> l1 = simpleStore
				.query(new Query(Restrictions.phrase("a")).limit(10));
		Collections.sort(l1);

		assertEquals(l1.size(), 3);
		assertEquals(l1.get(0).value, 2.0F);
		assertEquals(l1.get(1).value, 1.0F);
		assertEquals(l1.get(2).value, 0.5F);

		as.add(assoc("e", "X", 5.0F));
		simpleStore.addAll(as, "f", null);
		simpleStore.flush();

		List<Association<String, String>> l2 = simpleStore
				.query(new Query(Restrictions.phrase("e")).limit(10));

		assertEquals(l2.get(0).value, 5.0F);
		assertEquals(l2.get(0).to, "f");
	}

	/**
	 * Tests different operation inserts.
	 */
	public void testInsert03() throws Exception {
		File tf = FileTestSupport.createTempFile("assocs.idx");

		simpleStore = new FileAssociationStoreImpl<String, String>();
		simpleStore.setFromStore(sstore);
		simpleStore.setToStore(sstore);
		simpleStore.setStorageFile(tf);
		simpleStore.init();

		// Average
		simpleStore.add("a", "b", 1.0F, new Params(Operation.AVG));
		simpleStore.add("a", "b", 0.5F, new Params(Operation.AVG));

		// Logarithmic sum
		simpleStore.add("a", "c", 1.0F, new Params(Operation.LOGSUM));
		simpleStore.add("a", "c", 0.5F, new Params(Operation.LOGSUM));

		// Overwrite
		simpleStore.add("a", "d", 1.0F, new Params(Operation.OVERWRITE));
		simpleStore.add("a", "d", 0.5F, new Params(Operation.OVERWRITE));

		// Sum
		simpleStore.add("a", "e", 1.0F, new Params(Operation.SUM));
		simpleStore.add("a", "e", 0.5F, new Params(Operation.SUM));

		simpleStore.flush();

		List<Association<String, String>> l1 = simpleStore
				.query(new Query(Restrictions.phrase("a")));

		// Sort by 'to'
		Collections.sort(l1, new Comparator<Association<String, String>>() {
			@Override
			public int compare(Association<String, String> o1, Association<String, String> o2) {
				return o1.to.compareTo(o2.to);
			}
		});

		assertEquals(l1.size(), 4);
		assertEquals(l1.get(0).value, 0.75F); // AVG
		assertEquals(l1.get(1).value, 0.79673296F); // LOGSUM
		assertEquals(l1.get(2).value, 0.5F); // OVERWRITE
		assertEquals(l1.get(3).value, 1.5F); // SUM
	}

	/**
	 * Tests lots of random inserts.
	 */
	public void testInsert04() throws Exception {
		File dir = FileTestSupport.createTempDir("sstore");
		sstore = new StringStore();
		sstore.init(null, dir, new HashMap<String, String>());

		File tf = FileTestSupport.createTempFile("assocs.idx");

		simpleStore = new FileAssociationStoreImpl<String, String>();
		simpleStore.setFromStore(sstore);
		simpleStore.setToStore(sstore);
		simpleStore.setStorageFile(tf);
		simpleStore.init();

		int ASSOCS = 100000;
		int WORDS = 10000;

		List<String> words = TestUtils.randomWords(WORDS, 31);
		Map<String, Set<String>> control = new LinkedHashMap<String, Set<String>>();

		Random rnd = new Random(0);

		for (int i = 0; i < ASSOCS; i++) {
			String from = words.get(rnd.nextInt(WORDS));
			String to = words.get(rnd.nextInt(WORDS));

			simpleStore.add(from, to, 1.0F, null);

			Set<String> tos = control.get(from);
			if (tos == null) {
				tos = new HashSet<String>();
				control.put(from, tos);
			}
			tos.add(to);
		}

		sstore.flush();
		simpleStore.flush();
		sstore.close();
		simpleStore.close();

		sstore = new StringStore();
		sstore.init(null, dir, new HashMap<String, String>());

		simpleStore = new FileAssociationStoreImpl<String, String>();
		simpleStore.setFromStore(sstore);
		simpleStore.setToStore(sstore);
		simpleStore.setStorageFile(tf);
		simpleStore.init();

		// Check by control map
		for (Entry<String, Set<String>> e : control.entrySet()) {
			String from = e.getKey();
			Set<String> tos = e.getValue();

			int fid = (int) sstore.save(from);
			AssociationBlock assoc = simpleStore.getAssociation(fid);

			for (int i = 0; i < assoc.size; i++) {
				int tid = assoc.tos[i];
				String to = sstore.get(tid);

				if (!tos.remove(to)) {
					throw new AssertionError("Association " + from + " - " + to
							+ " was erroneously added to the association block!");
				}
			}

			if (!tos.isEmpty()) {
				throw new AssertionError("Associations " + from + " - " + tos
						+ " were missing from the association block!");
			}
		}
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

		simpleStore.addAll(as, null);
		simpleStore.flush();

		List<Association<String, String>> l;

		// Simple AND query
		l = simpleStore.query(new Query(Restrictions.and(Restrictions.phrase("a"),
				Restrictions.phrase("b"))).limit(10));
		assertEquals(l.size(), 1);
		assertEquals(l.get(0).to, "d");
		assertEquals(l.get(0).value, 1.5F);

		// Simple AND query with limit
		l = simpleStore.query(new Query(Restrictions.phrase("a")).limit(3));
		assertEquals(l.size(), 3);
		l = simpleStore.query(new Query(Restrictions.phrase("a")).limit(1));
		assertEquals(l.size(), 1);

		// Testing negation 1
		l = simpleStore.query(new Query(Restrictions.and(Restrictions.phrase("a"),
				Restrictions.not("b"))));
		Collections.sort(l);

		assertEquals(l.size(), 2);
		assertEquals(l.get(0).to, "b");
		assertEquals(l.get(1).to, "e");

		// Testing negation 2
		l = simpleStore.query(new Query(Restrictions.and(Restrictions.not("a"),
				Restrictions.phrase("b"))));
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

		Collection<Association<String, TestData>> as = new LinkedList<Association<String, TestData>>();
		as.add(assoc("a", data("bbb", new Date(), 10), 1.0F));
		as.add(assoc("a", data("ccc", new Date(0), 20), 0.5F));
		as.add(assoc("a", data("ddd", new Date(0), 30), 0.1F));
		as.add(assoc("b", data("ddd", new Date(), 40), 1.0F));

		objStore.addAll(as, null);
		objStore.flush();

		List<Association<String, TestData>> l;

		// Testing EQ
		l = objStore
				.query(new Query(Restrictions.phrase("a")).filter(Restrictions.eq("url", "bbb")));
		assertEquals(l.size(), 1);
		assertEquals(l.get(0).to.url, "bbb");
		assertEquals(l.get(0).value, 1.0F);

		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions.eq("created",
				new Date(0))));
		assertEquals(l.size(), 1);
		assertEquals(l.get(0).to.url, "ccc");

		// Testing NE
		l = objStore
				.query(new Query(Restrictions.phrase("a")).filter(Restrictions.ne("url", "bbb")));
		assertEquals(l.size(), 2);

		// Testing GT
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions
				.gt("length", 20L)));
		assertEquals(l.size(), 1);

		// Testing GE
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions
				.ge("length", 20L)));
		assertEquals(l.size(), 2);

		// Testing LT
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions
				.lt("length", 20L)));
		assertEquals(l.size(), 1);

		// Testing LE
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions
				.le("length", 20L)));
		assertEquals(l.size(), 2);

		// Testing regex matcher
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions.matches("url",
				Pattern.compile(".*?b.*"))));
		assertEquals(l.size(), 1);
		assertEquals(l.get(0).to.url, "bbb");

		// Testing a complex filter
		l = objStore.query(new Query(Restrictions.phrase("a")).filter(Restrictions.and(
				Restrictions.eq("url", "bbb"), Restrictions.gt("length", 5L))));
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
			simpleStore.query(new Query(Restrictions.and(Restrictions.not("a"),
					Restrictions.not("b"))));
			fail("Expected QueryExecutionException");
		} catch (QueryExecutionException e) {
		}

		// Invalid query (OR result set too large)
		try {
			simpleStore.query(new Query(Restrictions.or(Restrictions.not("a"),
					Restrictions.not("b"))));
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
