package org.ogreg.ase4j.file;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.ogreg.common.nio.NioUtils;
import org.ogreg.test.FileTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * File based association store tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class CachedBlockStoreTest {
	private CachedBlockStore fs;

	@BeforeMethod
	public void setUp() {
		AssociationBlock.baseCapacity = 4;
		CachedBlockStore.baseCapacity = 4;
	}

	@AfterMethod
	public void tearDown() {
		NioUtils.closeQuietly(fs);
		AssociationBlock.baseCapacity = 64;
		CachedBlockStore.baseCapacity = 1024;
	}

	/**
	 * Tests opening an empty store.
	 */
	public void testLoad01() {

		try {
			File store = FileTestSupport.createTempFile("assocs");
			fs = new CachedBlockStore();
			fs.open(store);

			assertTrue(store.exists());

			// Store size: 4 (ASE4J) + 8 (size) + 4 (index capacity) + 4 (index
			// maxKey) + 4 * 8 (index entries)
			assertEquals(store.length(), 52);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests opening an existing store and does some searching.
	 */
	public void testLoad02() {

		try {
			File store = FileTestSupport.createTempFile("assocs");

			fs = new CachedBlockStore();
			fs.open(store);
			fs.merge(assoc(0, 1, 100));
			fs.flush();

			// For coverage
			fs.open(store);
			fs.flush();
			fs.close();

			fs = new CachedBlockStore();
			fs.open(store);
			fs.merge(assoc(0, 1, 100)); // For coverage
			fs.close();

			fs = new CachedBlockStore();
			fs.open(store);

			assertEquals(100, fs.get(0, 1));
			assertEquals(0, fs.get(1, 2));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests saving associations to a store.
	 */
	public void testInsert01() {

		try {
			File store = FileTestSupport.createTempFile("assocs");

			fs = new CachedBlockStore();
			fs.open(store);
			fs.merge(assoc(0, 1, 100));
			fs.merge(assoc(0, 1, 200));
			fs.merge(assoc(0, 2, 50));
			fs.merge(assoc(0, 3, 50));
			fs.merge(assoc(0, 4, 50));
			fs.merge(assoc(0, 5, 50));

			assertEquals(150, fs.get(0, 1));
			assertEquals(50, fs.get(0, 2));

			fs.flush();
			fs.open(store);
			fs.merge(assoc(0, 1, 50));
			fs.flush();

			assertEquals(100, fs.get(0, 1));

			// For coverage (finalizer)
			fs = null;
			System.gc();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests growing the association store.
	 */
	public void testGrow01() {

		try {
			File store = FileTestSupport.createTempFile("assocs");

			fs = new CachedBlockStore();
			fs.open(store);
			fs.merge(assoc(0, 1, 10));
			fs.merge(assoc(1, 1, 20));
			// Testing storage holes also
			fs.merge(assoc(3, 1, 40));

			// Original size: 4 (AS4J) + 8 (size) + 4 (index capacity) + 4
			// (index maxKey) + 4 * 8 (index entries) + 4 * (12 + 4 * 8)
			// (association blocks of default size 4) = 220
			assertEquals(store.length(), 184);

			// Growing will occur here
			fs.merge(assoc(4, 1, 50));
			fs.merge(assoc(2, 1, 30));

			// Target size: 4 (AS4J) + 8 (size) + 4 (index capacity) + 4 (index
			// maxKey) + 8 * 8 (index entries) + 5 * (12 + 4 * 8) (association
			// blocks of default size 4) = 252
			assertEquals(store.length(), 304);

			fs.flush();

			assertEquals(fs.get(0, 1), 10);
			assertEquals(fs.get(1, 1), 20);
			assertEquals(fs.get(2, 1), 30);
			assertEquals(fs.get(3, 1), 40);
			assertEquals(fs.get(4, 1), 50);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests growing and flushing the association store.
	 */
	public void testGrow02() {

		try {
			File store = FileTestSupport.createTempFile("assocs");

			fs = new CachedBlockStore();
			fs.open(store);
			fs.setMaxCached(2);
			fs.merge(assoc(0, 1, 10));
			fs.merge(assoc(1, 1, 20));

			// Flushing will occur here

			fs.merge(assoc(4, 1, 50));
			fs.merge(assoc(5, 1, 60));

			// Growing will occur here
			fs.merge(assoc(2, 1, 30));
			fs.merge(assoc(3, 1, 40));

			// Flushing will occur here
			fs.merge(assoc(6, 1, 70));

			fs.flush();

			assertEquals(10, fs.get(0, 1));
			assertEquals(20, fs.get(1, 1));
			assertEquals(30, fs.get(2, 1));
			assertEquals(40, fs.get(3, 1));
			assertEquals(50, fs.get(4, 1));
			assertEquals(60, fs.get(5, 1));
			assertEquals(70, fs.get(6, 1));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	AssociationBlock assoc(int from, int to, int value) {
		AssociationBlock assoc = new AssociationBlock(from);
		assoc.merge(to, value);

		return assoc;
	}
}
