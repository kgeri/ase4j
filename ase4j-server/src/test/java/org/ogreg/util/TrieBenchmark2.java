package org.ogreg.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.ogreg.common.utils.MemoryUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * More real-life benchmarks for tries.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "performance")
public class TrieBenchmark2 {
	String path = "src/test/resources/urls/urls01.gz";

	private Trie<Long> trie;
	private Map<String, Long> map;

	@AfterMethod
	@BeforeMethod
	public void gc() {
		System.gc();
		System.err.println(MemoryUtils.usedMem() / 1024 + "k");
		trie = null;
		map = null;
		System.gc();
	}

	/**
	 * Tests and compares the speed of search of lots of URLs in a hash ma.
	 * 
	 * @throws IOException
	 */
	public void testSearch01() throws IOException {
		long before, time;

		Long value = 0L;
		List<String> words = readGZ(path);
		map = new HashMap<String, Long>();

		resetHashCache(words);

		{
			before = System.currentTimeMillis();

			for (String word : words) {
				map.put(word, value);
			}

			time = System.currentTimeMillis() - before;

			System.err.printf("HashMap %d puts in: %d ms\n", words.size(), time);
		}

		resetHashCache(words);

		{
			before = System.currentTimeMillis();

			for (String word : words) {
				map.get(word);
			}

			time = System.currentTimeMillis() - before;

			System.err.printf("HashMap %d gets in: %d ms\n", words.size(), time);
		}
	}

	/**
	 * Tests and compares the speed of search of lots of URLs in a trie.
	 * 
	 * @throws IOException
	 */
	public void testSearch02() throws IOException {
		long before, time;

		Long value = 0L;
		List<String> words = readGZ(path);
		trie = new Trie<Long>();

		{
			before = System.currentTimeMillis();

			for (String word : words) {
				trie.set(word, value);
			}

			time = System.currentTimeMillis() - before;

			System.err.printf("Trie %d puts in: %d ms\n", words.size(), time);
		}

		{
			before = System.currentTimeMillis();

			for (String word : words) {
				trie.get(word);
			}

			time = System.currentTimeMillis() - before;

			System.err.printf("Trie %d gets in: %d ms\n", words.size(), time);
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

	static List<String> readGZ(String path) {
		List<String> ll = new LinkedList<String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(
					path))));

			String line;

			while ((line = br.readLine()) != null) {
				ll.add(line);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			IOUtils.closeQuietly(br);
		}

		return ll;
	}
}
