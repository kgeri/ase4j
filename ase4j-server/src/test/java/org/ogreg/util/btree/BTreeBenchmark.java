package org.ogreg.util.btree;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ogreg.test.Benchmark;
import org.ogreg.test.Benchmark.Result;
import org.ogreg.test.TestUtils;
import org.testng.annotations.Test;

/**
 * B-Tree performance tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class BTreeBenchmark {
	int ITERATIONS = 500000;
	List<String> words = TestUtils.randomWords(ITERATIONS, 31);

	/**
	 * Tests the performance of the insert operation.
	 */
	public void testInsert01() {
		BTree<String, Integer> bt;
		int cnt = 0;

		{
			Benchmark.start();
			bt = new BTree<String, Integer>(512);

			for (String word : words) {
				bt.set(word, cnt++);
			}

			Result r = Benchmark.stop();

			System.err.printf("BTree %d puts in: %d ms using %d Kb mem\n", ITERATIONS,
					r.time(TimeUnit.MILLISECONDS), r.memory() / 1024);
		}

		{
			Benchmark.start();

			for (String word : words) {
				bt.get(word);
			}

			Result r = Benchmark.stop();

			System.err.printf("BTree %d gets in: %d ms\n", ITERATIONS,
					r.time(TimeUnit.MILLISECONDS));
		}
	}
}
