package org.ogreg.ostore.file;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.common.utils.SerializationUtils;

import org.ogreg.ostore.BaseObjectStore;
import org.ogreg.ostore.EntityAccessor;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.ObjectStoreMetadata;
import org.ogreg.ostore.PropertyPersistor;
import org.ogreg.ostore.index.UniqueIndex;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A generic, file-based implementation of the {@link ObjectStore} interface.
 * <p>
 * The objects are stored with their properties, in different files. Please see
 * {@link ObjectStore} for further documentation.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class FileObjectStoreImpl<T> extends BaseObjectStore<T> {

	/** The storage dir of this Object Store. */
	private File storageDir;

	/** The "sequence generator". */
	private AtomicInteger nextKey;

	/** Storage metadata. */
	private ObjectStoreMetadata metadata;

	@Override
	protected long getNextId() {
		return nextKey.incrementAndGet();
	}

	@Override
	protected void updateMaxId(long identifier) {
		// Does not guarantee that every identifier is always assigned, but
		// is threadsafe
		int diff = (int) (identifier - nextKey.get());
		if (diff > 0) {
			nextKey.addAndGet(diff);
		}
	}

	@Override
	public void init(EntityAccessor accessor, File storageDir, Map<String, String> params) {
		super.init(accessor, storageDir, params);
		this.storageDir = storageDir;

		try {
			nextKey = SerializationUtils.read(ObjectStoreManager.getSequenceFile(storageDir),
					AtomicInteger.class);
		} catch (IOException e) {
			nextKey = new AtomicInteger();
		}
	}

	@Override
	public void flush() throws IOException {
		SerializationUtils.write(ObjectStoreManager.getSequenceFile(storageDir), nextKey);

		super.flush();
	}

	@Override
	protected void flushUniqueIndex(String propertyName, UniqueIndex index) throws IOException {
		index.saveTo(ObjectStoreManager.getIndexFile(storageDir, propertyName));
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected FilePropertyPersistor createPersistor(Class<?> propertyType, String propertyName) {

		try {

			// TODO Custom config for serializers (through property type)
			NioSerializer<?> s = SerializerManager.findSerializerFor(propertyType);

			FilePropertyStore pstore = new FilePropertyStore();
			pstore.setType(propertyType);
			pstore.setSerializer(s);
			pstore.open(ObjectStoreManager.getPropertyFile(storageDir, propertyName));

			// TODO Indices for properties

			return new FilePropertyPersistor(pstore);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected PropertyPersistor createExtensionPersistor(String propertyName) {
		final Pattern extPattern = ObjectStoreManager.getExtensionFileNamePattern(propertyName);

		File[] extStoreFiles = storageDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return extPattern.matcher(name).matches();
			}
		});

		FileExtensionPersistor persistor = new FileExtensionPersistor(propertyName, storageDir);

		if ((extStoreFiles != null) && (extStoreFiles.length > 0)) {

			try {

				for (File extStore : extStoreFiles) {
					Matcher m = extPattern.matcher(extStore.getName());

					if (m.matches()) {
						String ename = m.group(1);

						// Typeless PropertyStore init (opening the store
						// initializes the type)
						FilePropertyStore pstore = new FilePropertyStore();
						pstore.open(extStore);
						pstore.setSerializer(SerializerManager.findSerializerFor(pstore.getType()));

						persistor.addPersistor(ename, new FileExtensionPersistor.ValuePersistor(
								pstore));
					}
				}
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		}

		return persistor;
	}

	@Override
	public ObjectStoreMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void setMetadata(ObjectStoreMetadata metadata) {
		this.metadata = metadata;
	}
}
