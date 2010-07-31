package org.ogreg.fh4j;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * File based hash tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class FileHashTest {

	static {
		FileHash.baseCapacity = 4;
	}

    private TestHash fh;

	@AfterMethod
	public void tearDown() {

        try {

			if (fh != null) {
				fh.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
	 * Tests opening an empty hash.
	 */
	public void testLoad01() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);

            assertTrue(hash.exists());

			// 24 + 4 * 4 + floor(4 * 0.8) * ( 4 + 24 + 8 )
			assertEquals(148, hash.length());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

    /**
	 * Tests opening an existing hash and does some puts and gets.
	 */
	public void testLoad02() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);
			fh.put("aaa", 1L);

            // For coverage
			fh.open(hash);
			fh.put("aaa", 2L);
			fh.flush();
			fh.close();

            fh = new TestHash();
			fh.open(hash);

            assertEquals(Long.valueOf(2), fh.get("aaa"));
			assertEquals(null, fh.get("ccc"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

    /**
	 * Tests hash bucket collision.
	 */
	public void testLoad03() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);
			fh.put("a", 1L);
			fh.put("e", 2L);

            assertEquals(Long.valueOf(1), fh.get("a"));
			assertEquals(Long.valueOf(2), fh.get("e"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

    /**
	 * Tests a String-String map (for coverage).
	 */
	public void testLoad04() {
		FileHash<String, String> fh = null;

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new FileHash<String, String>(new StringSerializer(2), new StringSerializer(2));
			fh.open(hash);
			fh.put("a", "b");

            assertEquals("b", fh.get("a"));
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {

            try {

				if (fh != null) {
					fh.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    /**
	 * Tests hash resizing.
	 */
	public void testLoad05() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);
			fh.put("a", 1L);
			fh.put("b", 2L);
			fh.put("c", 3L);
			fh.put("d", 4L);

            assertEquals(Long.valueOf(1), fh.get("a"));
            assertEquals(Long.valueOf(2), fh.get("b"));
            assertEquals(Long.valueOf(3), fh.get("c"));
            assertEquals(Long.valueOf(4), fh.get("d"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

    /**
	 * Tests hash iteration.
	 */
	public void testIterate01() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);
			fh.put("a", 1L);
			fh.put("b", 2L);

            Iterator<Entry<String, Long>> it = fh.entries().iterator();
			Entry<String, Long> e;

            assertTrue(it.hasNext());

            e = it.next();
			assertEquals("b", e.getKey());
			assertEquals(Long.valueOf(2), e.getValue());

            assertTrue(it.hasNext());

            e = it.next();
			assertEquals("a", e.getKey());
			assertEquals(Long.valueOf(1), e.getValue());

            try {
				it.next();
				fail("Expected NoSuchElementException");
			} catch (NoSuchElementException ex) {
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

    /**
	 * Tests hash removes.
	 */
	public void testRemove01() {

        try {
			File hash = FileTestSupport.createTempFile("hash.map");
			fh = new TestHash();
			fh.open(hash);
			fh.put("a", 1L);
			fh.put("e", 2L);

            assertEquals(2, fh.size());

            // Removing nonexistent key
			assertEquals(null, fh.remove("x"));
			assertEquals(2, fh.size());

            assertEquals(Long.valueOf(1), fh.remove("a"));
			assertEquals(1, fh.size());

            assertEquals(Long.valueOf(2), fh.remove("e"));
			assertEquals(0, fh.size());

            // Reusing deleted entry (for coverage)
			fh.put("a", 3L);
			assertEquals(1, fh.size());
			assertEquals(Long.valueOf(3), fh.get("a"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public void testCollision() {
		assertEquals(FileHash.indexFor("a".hashCode(), 4), FileHash.indexFor("e".hashCode(), 4));
	}

    private final class TestHash extends FileHash<String, Long> {
		public TestHash() {
			super(new StringSerializer(10), new LongSerializer());
		}
	}
}
