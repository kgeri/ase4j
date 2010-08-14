package org.ogreg.ostore.memory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.common.utils.FileUtils;
import org.ogreg.ostore.ConfigurableObjectStore;
import org.ogreg.ostore.EntityAccessor;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.ObjectStoreMetadata;
import org.ogreg.ostore.index.UniqueIndex;
import org.ogreg.util.Trie;
import org.ogreg.util.TrieDictionary;
import org.ogreg.util.TrieSerializer;
import org.ogreg.util.TrieSerializer.TrieSerializerListener;

/**
 * A simple {@link Trie}-based {@link ObjectStore} for storing {@link String}s.
 * 
 * @author Gergely Kiss
 */
public class StringStore implements ConfigurableObjectStore<String>, Serializable {
	private static final long serialVersionUID = -6176261432587230445L;

	private AtomicInteger nextKey;

	/** The dictionary to use for storing properties. */
	private TrieDictionary dictionary;

	/** The {@link Trie} to map Strings to integers. */
	private Trie<Integer> toInt;

	/** The map to map Integers to Strings. */
	private Map<Integer, byte[]> toString;

	/** The file which stores this instance. */
	private transient File storageFile;

	/** Storage metadata. */
	private transient ObjectStoreMetadata metadata;

	@Override
	public synchronized void init(EntityAccessor accessor, File storageDir,
			Map<String, String> params) {
		storageFile = ObjectStoreManager.getPropertyFile(storageDir, "strings");

		if (storageFile.exists()) {
			try {
				load();
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

		// Does not guarantee that every identifier is always assigned, but is
		// threadsafe
		int diff = (int) (identifier - nextKey.get());
		if (diff >= 0) {
			nextKey.addAndGet(diff + 1);
		}
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

		return (key == null) ? null : Long.valueOf(key.longValue());
	}

	@Override
	public synchronized void flush() throws IOException {
		save();
	}

	@Override
	public void setIdPropertyName(String propertyName) {
		// Do nothing
	}

	@Override
	public void addProperty(Class<?> propertyType, String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addExtension(String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addIndex(String fieldName, UniqueIndex idx) {
		// Do nothing
	}

	@Override
	public ObjectStoreMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void setMetadata(ObjectStoreMetadata metadata) {
		this.metadata = metadata;
	}

	private void load() throws IOException {
		FileChannel channel = null;

		try {
			RandomAccessFile raf = new RandomAccessFile(storageFile, "r");
			channel = raf.getChannel();

			this.nextKey = NioUtils.deserializeFrom(channel, AtomicInteger.class);
			this.toString = new HashMap<Integer, byte[]>();

			TrieSerializer<Integer> serializer = new TrieSerializer<Integer>(Integer.class);
			Trie<Integer> trie = serializer.deserialize(channel,
					new TrieSerializerListener<Integer>() {
						@Override
						public void onEntryRead(byte[] key, Integer value) {
							toString.put(value, key);
						}
					});

			this.dictionary = trie.getDictionary();
			this.toInt = trie;
		} finally {
			NioUtils.closeQuietly(channel);
		}
	}

	private void save() throws IOException {
		FileChannel channel = null;

		// Safe serialization with renameTo
		File tmp = File.createTempFile(storageFile.getName(), ".tmp", storageFile.getParentFile());

		try {
			RandomAccessFile raf = new RandomAccessFile(tmp, "rw");
			channel = raf.getChannel();

			NioUtils.serializeTo(channel, nextKey);

			TrieSerializer<Integer> serializer = new TrieSerializer<Integer>(Integer.class);
			serializer.serialize(toInt, channel);

			FileUtils.renameTo(tmp, storageFile);
		} finally {
			NioUtils.closeQuietly(channel);
			tmp.delete();
		}
	}
}
