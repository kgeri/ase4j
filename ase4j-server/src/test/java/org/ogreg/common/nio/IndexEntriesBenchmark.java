package org.ogreg.common.nio;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.ogreg.common.nio.IndexEntries;
import org.testng.annotations.Test;

/**
 * Size and speed benchmarks for the index.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class IndexEntriesBenchmark {

	public void testInsert01() {

		try {
			File file = new File("target/insert01.idx");
			file.delete();

			RandomAccessFile raf = new RandomAccessFile(file, "rw");

			int ITERATIONS = 10000000;

			IndexEntries ie = new IndexEntries();
			ie.map(raf.getChannel(), 0);

			long before = System.currentTimeMillis();

			for (int i = 0; i < ITERATIONS; i++) {
				ie.set(i, 1);

				if ((i % 100000) == 0) {
					System.err.println(i);
				}
			}

			long time = System.currentTimeMillis() - before;

			System.err.println(ITERATIONS + " inserts in " + time + " ms");
			System.err.println("Final file size: " + file.length() + " bytes");

			ie.unmap();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	public void testGet01() {

		try {
			File file = new File("target/insert01.idx");
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			// Shamelessly reusing previous test results...

			int ITERATIONS = 10000000;

			Random r = new Random();
			IndexEntries ie = new IndexEntries();
			ie.map(raf.getChannel(), 0);

			long before = System.currentTimeMillis();

			for (int i = 0; i < ITERATIONS; i++) {
				ie.get(r.nextInt(ITERATIONS));
			}

			long time = System.currentTimeMillis() - before;

			System.err.println(ITERATIONS + " gets in " + time + " ms");
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
}
