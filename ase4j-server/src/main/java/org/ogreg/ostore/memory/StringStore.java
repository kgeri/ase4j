package org.ogreg.ostore.memory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.ostore.ConfigurableObjectStore;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.index.UniqueIndex;
import org.ogreg.util.Trie;
import org.ogreg.util.TrieDictionary;

/**
 * A simple {@link Trie}-based {@link ObjectStore} for storing {@link String}s.
 * 
 * @author Gergely Kiss
 */
public class StringStore implements ConfigurableObjectStore<String>, Serializable {
	private static final long serialVersionUID = -6176261432587230445L;

	private AtomicInteger nextKey;

	/** The dictionary to use for storing strings. */
	private TrieDictionary dictionary;

	/** The {@link Trie} to map Strings to integers. */
	private Trie<Integer> toInt;

	/** The map to map Integers to Strings. */
	private Map<Integer, byte[]> toString;

	/** The file which stores this instance. */
	private transient File storageFile;

	@Override
	public synchronized void init(Class<String> type, File storageDir, Map<String, String> params) {
		// TODO type assert?

		storageFile = ObjectStoreManager.getPropertyFile(storageDir, "strings");

		if (storageFile.exists()) {
			try {
				StringStore store = SerializationUtils.read(storageFile, StringStore.class);
				this.dictionary = store.dictionary;
				this.toInt = store.toInt;
				this.toString = store.toString;
				this.nextKey = store.nextKey;
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		} else {
			String dictName = params.get("dictionary");
			this.dictionary = TrieDictionary.createByName(dictName);
			this.toInt = new Trie<Integer>(dictionary);
			this.toString = new HashMap<Integer, byte[]>();
			this.nextKey = new AtomicInteger(0);
		}
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

	@Override
	public void initProperty(Class<?> type, String propertyName) {
		// Do nothing
	}

	@Override
	public void initIdProperty(Class<?> type, String propertyName, UniqueIndex index) {
		// Do nothing
	}

	@Override
	public void initExtension(Class<?> type, String propertyName) {
		// Do nothing
	}

	@Override
	public synchronized void flush() throws IOException {
		SerializationUtils.write(storageFile, this);
	}
}
