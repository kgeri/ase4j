package org.ogreg.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import org.ogreg.common.utils.MemoryUtils;
import org.ogreg.test.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Benchmarks for tries.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class TrieBenchmark {
	int ITERATIONS = 500000;

	private HashMap<String, Long> map;
	private Trie<Long> trie;

	@BeforeMethod
	@AfterMethod
	public void gc() {
		System.gc();
		System.err.println(MemoryUtils.usedMem() / 1024 + "k");
		trie = null;
		map = null;
		System.gc();
	}

	/**
	 * Tests the speed of inserts and searches of random words in a hash map.
	 */
	public void testHashMap() {
		long before, time, cnt = 0;
		List<String> words = TestUtils.randomWords(ITERATIONS, 31);
		resetHashCache(words);

		{
			before = System.currentTimeMillis();
			map = new HashMap<String, Long>();
			for (String word : words) {
				map.put(word, cnt++);
			}
			time = System.currentTimeMillis() - before;

			System.err.printf("HashMap %d puts in: %d ms\n", ITERATIONS, time);
		}

		resetHashCache(words);

		{
			before = System.currentTimeMillis();
			for (String word : words) {
				map.get(word);
			}
			time = System.currentTimeMillis() - before;

			System.err.printf("HashMap %d gets in: %d ms\n", ITERATIONS, time);
		}
	}

	/**
	 * Tests the speed of inserts and searches in a trie.
	 */
	public void testTrie() {
		long before, time, cnt = 0;
		List<String> words = TestUtils.randomWords(ITERATIONS, 31);
		resetHashCache(words);

		{
			before = System.currentTimeMillis();
			trie = new Trie<Long>();
			for (String word : words) {
				trie.set(word, cnt++);
			}
			time = System.currentTimeMillis() - before;

			System.err.printf("Trie %d puts in: %d ms\n", ITERATIONS, time);
		}

		{
			before = System.currentTimeMillis();
			for (String word : words) {
				trie.get(word);
			}
			time = System.currentTimeMillis() - before;

			System.err.printf("Trie %d gets in: %d ms\n", ITERATIONS, time);
		}
	}

	private void resetHashCache(List<String> words) {
		Field hf;

		try {
			hf = String.class.getDeclaredField("hash");
			hf.setAccessible(true);

			for (String word : words) {
				hf.setInt(word, 0);
			}
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
