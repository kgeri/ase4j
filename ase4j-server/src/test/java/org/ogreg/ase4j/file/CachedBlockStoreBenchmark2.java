package org.ogreg.ase4j.file;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.test.Benchmark;
import org.ogreg.test.Benchmark.Result;
import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * File based association store benchmarks of a huge store.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class CachedBlockStoreBenchmark2 {
	private CachedBlockStore fs;

	/**
	 * Tests the performance of lots of semi-random inserts.
	 */
	public void testInsert01() {

		try {
			int ASSOCS = 10000000;
			int WORDS = 100000;

			File assocs = FileTestSupport.createTempFile("assocs");

			CachedBlockStore.baseCapacity = 1024;

			fs = new CachedBlockStore();
			fs.open(assocs);
			fs.setMaxCached(10000000);

			Random rnd = new Random(0);

			Benchmark.start();

			for (int i = 0; i < ASSOCS; i++) {
				AssociationBlock ab = new AssociationBlock(rnd.nextInt(WORDS));
				ab.merge(rnd.nextInt(WORDS), 1.0F, Operation.OVERWRITE);

				fs.merge(ab, Operation.OVERWRITE);
			}

			System.err.println("Flushing...");
			fs.flush();

			Result r = Benchmark.stop();

			System.err.printf("%.2f appends per sec\n",
					(ASSOCS * 1000.0) / r.time(TimeUnit.MILLISECONDS));
			System.err.printf("%.2f Mb/s\n",
					(double) assocs.length() / 1024 / 1.024 / r.time(TimeUnit.MILLISECONDS));
			System.err.printf("%.2f seconds\n", r.time(TimeUnit.MILLISECONDS) / 1000.0);
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			NioUtils.closeQuietly(fs);
		}
	}
}
