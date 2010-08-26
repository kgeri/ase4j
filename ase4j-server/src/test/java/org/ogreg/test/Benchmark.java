package org.ogreg.test;

import java.util.concurrent.TimeUnit;

/**
 * Support methods for benchmarking.
 * 
 * @author Gergely Kiss
 */
public class Benchmark {
	private static final ThreadLocal<Long> time = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> used = new ThreadLocal<Long>();

	public static void start() {
		time.set(System.nanoTime());
		used.set(usedmem());
	}

	public static Result stop() {
		long duration = System.nanoTime() - time.get();
		long memory = usedmem() - used.get();

		Result res = new Result(duration, memory);
		return res;
	}

	private static long usedmem() {
		Runtime rt = Runtime.getRuntime();
		System.gc();
		return rt.totalMemory() - rt.freeMemory();
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
