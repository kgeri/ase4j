package org.ogreg.common.nio;

import org.ogreg.test.FileTestSupport;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * Index entry tests.
 *
 * @author  Gergely Kiss
 */
@Test(groups = "correctness")
public class IndexEntriesTest {

    private File idx;
    private IndexEntries index;
    private RandomAccessFile raf;

    @BeforeMethod public void setUp() {
        idx = FileTestSupport.createTempFile("test.idx");
    }

    @AfterMethod public void tearDown() {

        if (index != null) {

            try {
                index.unmap();
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
        index = open();
    }

    /**
     * Tests opening an existing index and does some searching.
     */
    public void testLoad02() {

        try {
            index = open();
            index.set(0, 1000);
            index.set(1, 1001);
            index.set(2, 1002);
            index.set(3, 1003);
            index.flush();
            index.unmap();

            index = open();

            assertEquals(4, index.getCapacity());
            assertEquals(1002, index.get(2));
            assertEquals(0, index.get(5));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests setting the first index entry.
     */
    public void testInsert01() {

        try {
            index = open();
            index.set(0, 1000);
            index.flush();
            index.unmap();

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
            index = open();
            index.set(1, 999);
            index.set(3, 1000);
            index.flush();
            index.unmap();

            FileTestSupport.assertBinaryEqual("index/testInsert02.idx", idx.getAbsolutePath());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Tests a bogus index.
     */
    public void testBogus01() {
        index = open();
        // No flush has ever happened

        try {
            index = open();
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
        ie.unmap();
        NioUtils.closeQuietly(raf);
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
