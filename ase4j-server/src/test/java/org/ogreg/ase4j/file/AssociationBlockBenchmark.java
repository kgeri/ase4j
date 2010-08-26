package org.ogreg.ase4j.file;

import java.util.Random;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.testng.annotations.Test;

/**
 * Association block benchmarks.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class AssociationBlockBenchmark {
	int ITERATIONS = 100000;

	/**
	 * Tests a simple association merge.
	 */
	public void testMerge01() {
		long before, time;
		AssociationBlock block;
		Random r = new Random();

		{
			before = System.currentTimeMillis();
			block = new AssociationBlock(0);
			for (int i = 0; i < ITERATIONS; i++) {
				block.merge(r.nextInt(ITERATIONS), 1.0F, Operation.OVERWRITE);
			}
			time = System.currentTimeMillis() - before;

			System.err.printf("AssociationBlock %d merges in: %d ms\n", ITERATIONS, time);
			// TODO Random inserts are slow - luckily we rarely do that
		}
	}
}
