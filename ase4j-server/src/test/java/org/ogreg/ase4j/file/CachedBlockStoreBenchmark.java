package org.ogreg.ase4j.file;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;
import org.ogreg.test.TestUtils.Measurement;
import org.ogreg.test.TestUtils.Result;
import org.testng.annotations.Test;

/**
 * File based association store benchmarks.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class CachedBlockStoreBenchmark {
	private CachedBlockStore fs;

	/**
	 * Tests the performance of lots of "ordered from" inserts. The inserted
	 * association row always contains the same values and the index won't grow,
	 * so this tests the storage append time only.
	 */
	public void testInsert01() {

		try {
			File assocs = FileTestSupport.createTempFile("assocs");

			CachedBlockStore.baseCapacity = 1024 * 1024;

			fs = new CachedBlockStore();
			fs.open(assocs);

			final AssociationBlock ar = new AssociationBlock(0);

			for (int i = 0; i < 127; i++) {
				ar.merge(i, 100, Operation.AVG);
			}

			Result r = TestUtils.measure(100000, new Measurement() {
				@Override
				public void run(int step) throws Exception {
					ar.from = step;
					fs.merge(ar, Operation.AVG);
				}

				@Override
				public void after() throws Exception {
					fs.flush();
				}
			});

			System.err.printf("%.2f ordered appends per sec\n", r.stepsPerSec);
			System.err.printf("%.2f Mb/s\n", (double) assocs.length() / 1024 / 1.024 / r.timeMs);
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			NioUtils.closeQuietly(fs);
		}
	}

	/**
	 * Tests the performance of lots of "random from" inserts. The inserted
	 * association row always contains the same values and the index won't grow,
	 * so this tests the storage append and update time together.
	 * <p>
	 * This test is considerably faster than testInsert01, because it doesn't
	 * require so many appends.
	 * </p>
	 */
	public void testInsert02() {

		try {
			File assocs = FileTestSupport.createTempFile("assocs");

			CachedBlockStore.baseCapacity = 1024 * 1024;

			fs = new CachedBlockStore();
			fs.open(assocs);

			final AssociationBlock ar = new AssociationBlock(0);

			for (int i = 0; i < 127; i++) {
				ar.merge(i, 100, Operation.AVG);
			}

			final Random rnd = new Random();
			Result r = TestUtils.measure(100000, new Measurement() {
				@Override
				public void run(int iteration) throws Exception {
					ar.from = rnd.nextInt(100000);
					fs.merge(ar, Operation.AVG);
				}

				@Override
				public void after() throws Exception {
					fs.flush();
				}
			});

			System.err.printf("%.2f random appends per sec\n", r.stepsPerSec);
			System.err.printf("%.2f Mb/s\n", (double) assocs.length() / 1024 / 1.024 / r.timeMs);
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			NioUtils.closeQuietly(fs);
		}
	}
}
