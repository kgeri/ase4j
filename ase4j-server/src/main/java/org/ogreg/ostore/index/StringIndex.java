package org.ogreg.ostore.index;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.util.Trie;
import org.ogreg.util.TrieDictionary;

/**
 * A {@link Trie}-based implementation of a {@link UniqueIndex}.
 * 
 * @author Gergely Kiss
 */
public class StringIndex implements UniqueIndex, Serializable {
	private static final long serialVersionUID = -2371533415602619440L;

	/** The {@link Trie} to map Strings to integers. */
	private Trie<Integer> toInt;

	/** The dictionary to use for storing strings. */
	private TrieDictionary dictionary;

	public StringIndex() {
	}

	@Override
	public Long getKey(Object value) {
		Integer key = toInt.get((String) value);
		return key == null ? null : Long.valueOf(key.longValue());
	}

	@Override
	public void setKey(Object value, long identifier) {
		toInt.set((String) value, (int) identifier);
	}

	@Override
	public void loadFrom(File indexFile, Map<String, String> params) throws IOException {
		if (indexFile.exists()) {
			StringIndex idx = SerializationUtils.read(indexFile, StringIndex.class);
			this.toInt = idx.toInt;
			this.dictionary = idx.dictionary;
		} else {
			String dictName = params.get("dictionary");
			this.dictionary = TrieDictionary.createByName(dictName);
			this.toInt = new Trie<Integer>(dictionary);
		}
	}

	@Override
	public void saveTo(File indexFile) throws IOException {
		SerializationUtils.write(indexFile, this);
	}
}
