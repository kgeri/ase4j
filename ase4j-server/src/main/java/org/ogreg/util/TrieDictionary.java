package org.ogreg.util;

import java.io.Serializable;

/**
 * A fast String - byte mapping for {@link Trie}s.
 * 
 * @author Gergely Kiss
 */
public class TrieDictionary implements Serializable {
	private static final long serialVersionUID = -7230069946129401046L;

	/**
	 * The case insensitive English basic dictionary.
	 * <p>
	 * The mapping is as follows (unknown characters are mapped to ' '):
	 * </p>
	 * 
	 * <pre>
	 *         abcdefghijklmnopqrstuvwxyz0123456789
	 *         ABCDEFGHIJKLMNOPQRSTUVWXYZ
	 * </pre>
	 */
	public static final TrieDictionary EN = new TrieDictionary(
			" abcdefghijklmnopqrstuvwxyz0123456789", " ABCDEFGHIJKLMNOPQRSTUVWXYZ");

	/**
	 * The case insensitive Hungarian basic dictionary.
	 * <p>
	 * The mapping is as follows (unknown characters are mapped to ' '):
	 * </p>
	 * 
	 * <pre>
	 *         aábcdeéfghiíjklmnoóöőpqrstuúüűvwxyz0123456789
	 *         AÁBCDEÉFGHIÍJKLMNOÓÖŐPQRSTUÚÜŰVWXYZ
	 * </pre>
	 */
	public static final TrieDictionary HU = new TrieDictionary(
			" aábcdeéfghiíjklmnoóöőpqrstuúüűvwxyz0123456789",
			" AÁBCDEÉFGHIÍJKLMNOÓÖŐPQRSTUÚÜŰVWXYZ");

	/**
	 * The case insensitive English basic dictionary for URLs.
	 * <p>
	 * The mapping is as follows (unknown characters are mapped to ' '):
	 * </p>
	 * 
	 * <pre>
	 *         abcdefghijklmnopqrstuvwxyz0123456789:/.?#&=%!+-_,
	 *         ABCDEFGHIJKLMNOPQRSTUVWXYZ
	 * </pre>
	 */
	public static final TrieDictionary URL = new TrieDictionary(
			" abcdefghijklmnopqrstuvwxyz0123456789:/.?#&=%!+-_,", " ABCDEFGHIJKLMNOPQRSTUVWXYZ");

	private byte size;
	private char[] code2char;
	private byte[] char2code;

	/**
	 * Creates a Trie dictionary based on the characters.
	 * <p>
	 * You may specify an array of arrays, where elements of each array will be
	 * mapped to their array index. This way it is possible to map multiple
	 * characters to the same code (the encoding is lossy) - when decoding, the
	 * first array's characters will be used.
	 * </p>
	 * <p>
	 * Note: for performance reasons, every unknown character will be mapped to
	 * the 0th char of this string.
	 * </p>
	 * 
	 * @param dictionary
	 */
	public TrieDictionary(String... characters) {

		if ((characters == null) || (characters.length < 1)) {
			throw new IllegalArgumentException("The mapped array can not be empty");
		}

		// Determining bounds
		int maxLen = 0;
		int maxChr = 0;

		for (String chars : characters) {
			maxLen = (chars.length() > maxLen) ? chars.length() : maxLen;

			for (char c : chars.toCharArray()) {
				maxChr = (c > maxChr) ? c : maxChr;
			}
		}

		if (maxLen > 127) {
			throw new IllegalArgumentException(
					"Trie dictionary only supports at most 127 different characters");
		}

		// Initializing
		size = (byte) maxLen;
		code2char = new char[maxLen];
		char2code = new byte[maxChr + 1];

		for (int i = 1; i < characters.length; i++) {
			char[] chars = characters[i].toCharArray();

			for (byte j = 0; j < chars.length; j++) {
				char c = chars[j];
				char2code[c] = j;
				code2char[j] = c;
			}
		}

		// For decoding, the first array will be used
		char[] chars = characters[0].toCharArray();

		for (byte j = 0; j < chars.length; j++) {
			char c = chars[j];
			char2code[c] = j;
			code2char[j] = c;
		}
	}

	public byte[] encode(String word) {
		byte[] ret = new byte[word.length()];
		int len = char2code.length;

		for (int i = 0; i < ret.length; i++) {
			int idx = word.charAt(i);
			ret[i] = (idx < len) ? char2code[idx] : 0;
		}

		return ret;
	}

	public String decode(byte[] word, int offset, int count) {
		char[] ret = new char[count];

		for (int i = 0; i < count; i++) {
			ret[i] = code2char[word[i + offset]];
		}

		return new String(ret);
	}

	public byte size() {
		return size;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < char2code.length; i++) {
			byte c = char2code[i];

			if (c == 0) {
				continue;
			}

			if (buf.length() > 0) {
				buf.append(", ");
			}

			buf.append((char) i).append('=').append(char2code[i]);
		}

		return buf.toString();
	}

	/**
	 * Creates a trie dictionary by a predefined <code>name</code>.
	 * <p>
	 * Supported names are:
	 * <ul>
	 * <li>
	 * </ul>
	 * </p>
	 * 
	 * @param name
	 * @return
	 */
	public static TrieDictionary createByName(String name) {
		// URL dictionary
		if ("url".equalsIgnoreCase(name)) {
			return TrieDictionary.URL;
		}
		// Hungarian dictionary
		if ("hu".equalsIgnoreCase(name)) {
			return TrieDictionary.HU;
		}
		// The default dictionary is english
		else {
			return TrieDictionary.EN;
		}

		// TODO Custom dictionary by specifying value set
	}
}
