package org.ogreg.fh4j;

import java.io.File;
import java.io.IOException;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * File hash benchmarks.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class FileHashBenchmark {

	static {
		FileHash.baseCapacity = 2 * 1024 * 1024;
	}

	/**
	 * Tests some random puts and gets. In the second run, the number of puts
	 * will reach the threshold, so resizing will occur.
	 */
	public void testPutGet01() {

		try {
			File hash = FileTestSupport.createTempFile("hash.map");

			FileHash<String, Long> fh = new FileHash<String, Long>(new StringSerializer(64),
					new LongSerializer());
			fh.open(hash);

			int ITERATIONS = 1000000;
			Long lv = Long.valueOf(1);

			String[] str = createStrings(ITERATIONS, 64);

			{ // PUTs

				long before = System.currentTimeMillis();

				for (int i = 0; i < ITERATIONS; i++) {
					fh.put(str[i], lv);
				}

				long time = System.currentTimeMillis() - before;

				System.err.println((ITERATIONS * 1000 / time) + " puts per sec");
				System.err.println(((ITERATIONS * (128 + 8)) / 1024 / 1.024 / time) + " Mb/s");
			}

			{ // GETs

				long before = System.currentTimeMillis();

				for (int i = 0; i < ITERATIONS; i++) {
					fh.get(str[i]);
				}

				long time = System.currentTimeMillis() - before;

				System.err.println((ITERATIONS * 1000 / time) + " gets per sec");
				System.err.println(((ITERATIONS * (128 + 8)) / 1024 / 1.024 / time) + " Mb/s");
			}

			{ // PUTs with 1 resizing

				str = createStrings(ITERATIONS * 2, 64);

				long before = System.currentTimeMillis();

				for (int i = 0; i < (ITERATIONS * 2); i++) {
					fh.put(str[i], lv);
				}

				long time = System.currentTimeMillis() - before;

				System.err.println((ITERATIONS * 2 * 1000 / time) + " puts per sec");
				System.err.println(((ITERATIONS * 2 * (128 + 8)) / 1024 / 1.024 / time) + " Mb/s");
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	static String[] createStrings(int num, int maxLen) {
		String[] ret = new String[num];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Integer.toString(i);
		}

		return ret;
	}
}
