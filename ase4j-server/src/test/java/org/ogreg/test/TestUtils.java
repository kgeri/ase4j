package org.ogreg.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Common test helper methods.
 * 
 * @author Gergely Kiss
 */
public class TestUtils {

	/**
	 * Generates <code>count</code> unique, random strings of length
	 * <code>length</code>, using only the characters [a-z].
	 * 
	 * @param count
	 * @return
	 */
	public static List<String> randomWords(int count, int length) {
		StringBuilder sb = new StringBuilder();
		Set<String> ret = new HashSet<String>();
		Random r = new Random(0);

		while (ret.size() < count) {
			int len = r.nextInt(length) + 1;

			for (int j = 0; j < len; j++) {
				sb.append((char) (r.nextInt(26) + 'a'));
			}

			ret.add(sb.toString());
			sb.setLength(0);
		}

		return new ArrayList<String>(ret);
	}

	/**
	 * Creates a random string of length between <code>minLen</code> (iclusive)
	 * and <code>maxLen</code> (exclusive).
	 * 
	 * @param minLen
	 * @param maxLen
	 * @return
	 */
	public static String randomString(int minLen, int maxLen) {
		Random rnd = new Random(0);

		char[] chrs = new char[rnd.nextInt(maxLen - minLen) + minLen];

		for (int i = 0; i < chrs.length; i++) {
			chrs[i] = (char) (rnd.nextInt(26) + 'a');
		}
		return new String(chrs);
	}

	/**
	 * Executes and measures the performance of the given code.
	 * 
	 * @param iterations
	 * @param code
	 * @return Execution time in seconds
	 */
	public static Result measure(int iterations, Measurement code) {
		try {
			long before = System.currentTimeMillis();

			code.before();

			for (int i = 0; i < iterations; i++) {
				code.run(i);
			}

			code.after();

			long time = System.currentTimeMillis() - before;
			return new Result(iterations, time);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError("Performance check failed");
		}
	}

	public static void sleep(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
		}
	}

	public static class Result {
		public final long timeMs;
		public final double timeSec;
		public final double stepsPerSec;

		Result(int iterations, long timeMs) {
			this.timeMs = timeMs;
			this.timeSec = timeMs / 1000.0;
			this.stepsPerSec = iterations / timeSec;
		}
	}

	/**
	 * Interface for performance measurement assistance.
	 * 
	 * @author Gergely Kiss
	 */
	public static abstract class Measurement {

		/**
		 * The implementor may have (measured) initialization code here.
		 * 
		 * @throws Exception
		 */
		public void before() throws Exception {
		}

		/**
		 * The implementor should provide one iteration step here.
		 * 
		 * @param iteration
		 */
		public abstract void run(int iteration) throws Exception;

		/**
		 * The implementor may have (measured) cleanup code here.
		 * 
		 * @throws Exception
		 */
		public void after() throws Exception {
		}
	}
}
