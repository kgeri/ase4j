package org.ogreg.ostore.memory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.ostore.ConfigurableObjectStore;
import org.ogreg.ostore.EntityAccessor;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.ObjectStoreMetadata;
import org.ogreg.ostore.index.UniqueIndex;
import org.ogreg.util.Callback;
import org.ogreg.util.Trie;
import org.ogreg.util.TrieDictionary;
import org.ogreg.util.TrieSerializer;
import org.ogreg.util.TrieSerializer.TrieSerializerListener;

/**
 * A simple {@link Trie}-based {@link ObjectStore} for storing {@link String}s.
 * 
 * @author Gergely Kiss
 */
public class StringStore implements ConfigurableObjectStore<String>, Closeable, Serializable,
		StringStoreMBean {
	private static final long serialVersionUID = 4411880039656329960L;

	private AtomicInteger nextKey;

	/** The dictionary to use for storing properties. */
	private TrieDictionary dictionary;

	/** The {@link Trie} to map Strings to integers. */
	private Trie<Integer> toInt;

	/** The map to map Integers to Strings. */
	private Map<Integer, byte[]> toString;

	// Helper fields

	/** The file which stores this instance. */
	private transient File storageFile;

	/** The file channel which stores this instance. */
	private transient FileChannel storageChannel;

	/** Storage metadata. */
	private transient ObjectStoreMetadata metadata;

	private transient TrieSerializer<Integer> serializer = new TrieSerializer<Integer>(
			Integer.class);

	@Override
	public synchronized void init(EntityAccessor accessor, File storageDir,
			Map<String, String> params) {
		try {
			storageFile = ObjectStoreManager.getPropertyFile(storageDir, "strings");
			boolean existed = storageFile.exists();

			storageChannel = new RandomAccessFile(storageFile, "rw").getChannel();

			if (existed) {
				this.nextKey = new AtomicInteger(NioUtils.readInt(storageChannel));
				this.toString = new HashMap<Integer, byte[]>();

				Trie<Integer> trie = serializer.deserialize(storageChannel,
						new TrieSerializerListener<Integer>() {
							@Override
							public void onEntryRead(byte[] key, Integer value) {
								toString.put(value, key);
							}
						});

				this.dictionary = trie.getDictionary();
				this.toInt = trie;
			} else {
				String dictName = params.get("dictionary");
				this.dictionary = TrieDictionary.createByName(dictName);
				this.toInt = new Trie<Integer>(dictionary);
				this.toString = new HashMap<Integer, byte[]>();
				this.nextKey = new AtomicInteger(0);

				NioUtils.writeInt(storageChannel, nextKey.intValue());
				serializer.serialize(toInt, storageChannel);
			}
		} catch (IOException e) {
			throw new ConfigurationException(e);
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
	public synchronized void add(long identifier, String entity) throws ObjectStoreException {
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

		try {
			// Appending to file channel immediately
			storageChannel.position(storageChannel.size());
			serializer.write(b, id, storageChannel);
		} catch (IOException e) {
			throw new ObjectStoreException(e);
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
		storageChannel.force(false);
	}

	@Override
	public synchronized void close() throws IOException {
		storageChannel.close();
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

	@Override
	public long getObjectCount() {
		return toString.size();
	}

	@Override
	public void dump(String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			throw new IOException("Target path already exists: '" + path
					+ "'. Dump aborted for security reasons.");
		}

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(file));
			final BufferedWriter w = bw;

			toInt.getWords(new Callback<String>() {
				@Override
				public void callback(String value) {
					try {
						w.append(value).append('\n');
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

			bw.flush();
		} catch (IllegalStateException e) {
			if (e.getCause() instanceof IOException) {
				throw ((IOException) e.getCause());
			}
			throw e;
		} finally {
			NioUtils.closeQuietly(bw);
		}
	}
}
