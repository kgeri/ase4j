package org.ogreg.common.nio;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Index entry tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class IndexEntriesTest {

	private File idx;
	private IndexEntries i;
	private RandomAccessFile raf;

	@BeforeMethod
	public void setUp() {
		idx = FileTestSupport.createTempFile("test.idx");
	}

	@AfterMethod
	public void tearDown() {

		if (i != null) {

			try {
				i.unmap();
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tests opening an empty index.
	 */
	public void testLoad01() {
		i = open();
	}

	/**
	 * Tests opening an existing index and does some searching.
	 */
	public void testLoad02() {

		try {
			i = open();
			i.set(0, 1000);
			i.set(1, 1001);
			i.set(2, 1002);
			i.set(3, 1003);
			i.flush();
			i.unmap();

			i = open();

			assertEquals(4, i.getCapacity());
			assertEquals(1002, i.get(2));
			assertEquals(0, i.get(5));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests setting the first index entry.
	 */
	public void testInsert01() {

		try {
			i = open();
			i.set(0, 1000);
			i.flush();
			i.unmap();

			FileTestSupport.assertBinaryEqual("index/testInsert01.idx", idx.getAbsolutePath());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Tests setting two index entries.
	 */
	public void testInsert02() {

		try {
			i = open();
			i.set(1, 999);
			i.set(3, 1000);
			i.flush();
			i.unmap();

			FileTestSupport.assertBinaryEqual("index/testInsert02.idx", idx.getAbsolutePath());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	// /**
	// * Tests growing the index.
	// */
	// public void testGrow01() {
	//
	// try {
	// i = open();
	// i.set(3, 1003);
	// i.flush();
	//
	// assertEquals(4, i.getCapacity());
	//
	// try {
	// i.set(4, 1004);
	// fail("Expected IndexOutOfBoundsException");
	// } catch (IndexOutOfBoundsException e) {
	// }
	//
	// i.grow(5);
	// i.set(4, 1004);
	//
	// assertEquals(8, i.getCapacity());
	//
	// i.flush();
	// i.unmap();
	//
	// FileTestSupport.assertBinaryEqual("index/testGrow01.idx",
	// idx.getAbsolutePath());
	// } catch (IOException e) {
	// throw new AssertionError(e);
	// }
	// }

	/**
	 * Tests a bogus index.
	 */
	public void testBogus01() {
		i = open();
		// No flush has ever happened

		try {
			i = open();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	/**
	 * Come dummy cases for full coverage.
	 */
	public void testCoverage01() throws IOException {

		// baseCapacity map
		IndexEntries ie = new IndexEntries();
		raf = new RandomAccessFile(idx, "rw");
		ie.map(raf.getChannel(), 0);
	}

	private IndexEntries open() {

		try {

			if (raf != null) {
				raf.close();
			}

			IndexEntries ie = new IndexEntries();
			raf = new RandomAccessFile(idx, "rw");
			ie.map(raf.getChannel(), 0, 4);

			return ie;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
