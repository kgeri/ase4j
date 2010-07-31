package org.ogreg.ostore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogreg.util.Trie;
import org.ogreg.util.TrieDictionary;

/**
 * A simple {@link Trie}-based {@link ObjectStore} for storing {@link String}s.
 * 
 * @author Gergely Kiss
 */
public class StringStore implements ObjectStore<String>, Serializable {
	private static final long serialVersionUID = -6176261432587230445L;

	private AtomicInteger nextKey = new AtomicInteger(0);

	/** The dictionary to use for storing strings. */
	private TrieDictionary dictionary;

	/** The {@link Trie} to map Strings to integers. */
	private Trie<Integer> toInt;

	/** The map to map Integers to Strings. */
	private Map<Integer, byte[]> toString;

	public StringStore(TrieDictionary dictionary) {
		this.dictionary = dictionary;
		this.toInt = new Trie<Integer>(dictionary);
		this.toString = new HashMap<Integer, byte[]>();
	}

	@Override
	public long save(String entity) throws ObjectStoreException {
		Integer key = toInt.get(entity);

		if (key == null) {
			int nk = nextKey.incrementAndGet();
			add(nk, entity);

			return nk;
		}

		return key.intValue();
	}

	@Override
	public long saveOrUpdate(String entity) throws ObjectStoreException {
		return save(entity);
	}

	@Override
	public void add(long identifier, String entity) throws ObjectStoreException {
		byte[] b = dictionary.encode(entity);
		Integer id = Integer.valueOf((int) identifier);

		toInt.set(b, id);
		toString.put(id, b);
	}

	@Override
	public String get(long identifier) throws ObjectStoreException {
		byte[] b = toString.get(Integer.valueOf((int) identifier));

		if (b == null) {
			return null;
		}

		return dictionary.decode(b, 0, b.length);
	}

	@Override
	public Object getField(long identifier, String fieldName) throws ObjectStoreException {
		// TODO Field name check?
		return get(identifier);
	}

	@Override
	public Long uniqueResult(String fieldName, Object value) throws ObjectStoreException {
		// TODO Field name check?
		Integer key = toInt.get((String) value);
		return key == null ? null : Long.valueOf(key.longValue());
	}
}
