package org.ogreg.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.ogreg.test.TestUtils;
import org.testng.annotations.Test;

/**
 * Trie data structre tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class TrieTest {

	/**
	 * Tests some simple trie inserts.
	 */
	public void testInsert01() {
		Trie<Integer> t = new Trie<Integer>();

		List<String> words = new ArrayList<String>();
		words.add("abab");
		words.add("aba");
		words.add("abc");
		words.add("bac");
		words.add("bad");

		for (int i = 0; i < words.size(); i++) {
			t.set(words.get(i), i);
		}

		StringListCallback w = new StringListCallback();
		t.getWords(w);
		assertEquals(w.size(), words.size());

		words.removeAll(w);
		assertEquals(0, words.size());

		// For coverage
		t.get("ababa");
	}

	/**
	 * Tests trie inserts.
	 */
	public void testInsert02() {
		Trie<Integer> t = new Trie<Integer>();

		List<String> words = TestUtils.randomWords(1000, 31);

		for (int i = 0; i < words.size(); i++) {
			t.set(words.get(i), i);
		}

		StringListCallback w = new StringListCallback();
		t.getWords(w);
		assertEquals(w.size(), words.size());

		words.removeAll(w);
		assertEquals(0, words.size());
	}

	/**
	 * Tests trie searches.
	 */
	public void testSearch01() {
		Trie<Integer> t = new Trie<Integer>();

		List<String> words = TestUtils.randomWords(1000, 31);

		for (int i = 0; i < words.size(); i++) {
			t.set(words.get(i), i);
		}

		for (int i = 0; i < words.size(); i++) {
			t.set(words.get(i), i);
			assertEquals(Integer.valueOf(i), t.get(words.get(i)));
		}

		// For coverage
		t.get("alma");
		t.root.toString();
	}

	/**
	 * Just some coverage tests.
	 */
	@Test
	public void testCoverage() {

		try {
			new TrieDictionary((String[]) null);
			fail("Expected IAE");
		} catch (IllegalArgumentException e) {
		}

		try {
			new TrieDictionary(
					"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
			fail("Expected IAE");
		} catch (IllegalArgumentException e) {
		}

		TrieDictionary.EN.toString();

		TrieDictionary.EN.encode("á");

		new Trie<Integer>().set(TrieDictionary.EN.encode("aaa"), 0);

		new Trie<Integer>().set("", 0);

		new Trie<Integer>().set(new byte[0], 0);
	}

	class StringListCallback extends ArrayList<String> implements Callback<String> {
		private static final long serialVersionUID = 1L;

		@Override
		public void callback(String value) {
			add(value);
		}
	}
}
