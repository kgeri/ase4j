package org.ogreg.ostore.memory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.test.Benchmark;
import org.ogreg.test.Benchmark.Result;
import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;
import org.testng.annotations.Test;

/**
 * StringStore speed tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class StringStoreBenchmark {

	/**
	 * Tests a lot of random adds, then a flush. Also compares the results with
	 * standard serialization.
	 */
	public void testSave01() throws Exception {
		int ITERATIONS = 500000;
		List<String> words = TestUtils.randomWords(ITERATIONS, 31);
		File dir = FileTestSupport.createTempDir("sstore");
		File file2 = FileTestSupport.createTempFile("sstore-2.ser");
		File file3 = FileTestSupport.createTempFile("sstore-3.ser");

		StringStore store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		for (String word : words) {
			store.save(word);
		}

		Result res;

		Benchmark.start();
		store.flush();
		res = Benchmark.stop();
		System.err.printf("NIO flush completed in %d ms\n", res.time(TimeUnit.MILLISECONDS));

		Benchmark.start();
		SerializationUtils.write(file3, store);
		res = Benchmark.stop();
		System.err.printf("Java Serialization completed in %d ms\n",
				res.time(TimeUnit.MILLISECONDS));

		// For whatever reason, the naiive dump implementation is hell fast...
		Benchmark.start();
		store.dump(file2.getAbsolutePath());
		res = Benchmark.stop();
		System.err.printf("Dump completed in %d ms\n", res.time(TimeUnit.MILLISECONDS));

		Benchmark.start();
		store.init(null, dir, new HashMap<String, String>());
		res = Benchmark.stop();
		System.err.printf("NIO read completed in %d ms\n", res.time(TimeUnit.MILLISECONDS));

		Benchmark.start();
		SerializationUtils.read(file3, StringStore.class);
		res = Benchmark.stop();
		System.err.printf("Java Deserialization completed in %d ms\n",
				res.time(TimeUnit.MILLISECONDS));
	}
}
