package org.ogreg.test;

import java.util.concurrent.TimeUnit;

/**
 * Support methods for benchmarking.
 * 
 * @author Gergely Kiss
 */
public class Benchmark {
	private static final ThreadLocal<Long> time = new ThreadLocal<Long>();

	public static void start() {
		time.set(System.nanoTime());
	}

	public static Result stop() {
		long duration = System.nanoTime() - time.get();
		Runtime rt = Runtime.getRuntime();
		long freeMemBefore = rt.freeMemory();
		System.gc();
		long memory = rt.freeMemory() - freeMemBefore;

		Result res = new Result(duration, memory);
		return res;
	}

	public static class Result {
		private final long duration;
		private final long memory;

		public Result(long duration, long memory) {
			this.duration = duration;
			this.memory = memory;
		}

		public long time(TimeUnit unit) {
			return unit.convert(duration, TimeUnit.NANOSECONDS);
		}

		public long memory() {
			return memory;
		}
	}
}
