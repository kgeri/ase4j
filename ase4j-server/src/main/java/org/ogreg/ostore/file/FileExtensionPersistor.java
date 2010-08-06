package org.ogreg.ostore.file;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.common.utils.PropertyUtils;

import org.ogreg.ostore.ObjectStoreManager;
import org.ogreg.ostore.PropertyPersistor;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Property field persistor for extended properties (maps).
 *
 * @author  Gergely Kiss
 */
class FileExtensionPersistor implements PropertyPersistor {
    private final Map<String, PropertyPersistor> persistors =
        new HashMap<String, PropertyPersistor>();

    private final File storageDir;
    private final String propertyName;

    public FileExtensionPersistor(String propertyName, File storageDir) {
        this.propertyName = propertyName;
        this.storageDir = storageDir;
    }

    @Override public Object load(long identifier, String propertyPath) throws IOException {

        if (propertyPath == null) {

            // Full map query
            Map<String, Object> ret = new HashMap<String, Object>();

            for (Entry<String, PropertyPersistor> e : persistors.entrySet()) {
                Object value = e.getValue().load(identifier, null);
                ret.put(e.getKey(), value);
            }

            return ret;
        } else {

            // Object picking
            String[] path = PropertyUtils.splitFirstPathElement(propertyPath);
            PropertyPersistor accessor = persistors.get(path[0]);

            if (accessor == null) {
                return null;
            }

            return accessor.load(identifier, path[1]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void store(long identifier, String propertyPath, Object value) throws IOException {
        Map<String, Object> map = (Map<String, Object>) value;

        if (propertyPath == null) {

            // Full map update
            for (Entry<String, Object> e : map.entrySet()) {
                String ename = e.getKey();
                Object evalue = e.getValue();

                ensureHasPersistor(ename, evalue).store(identifier, null, evalue);
            }
        } else {

            // Object picking
            String[] path = PropertyUtils.splitFirstPathElement(propertyPath);
            ensureHasPersistor(path[0], value).store(identifier, path[1], value);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private PropertyPersistor ensureHasPersistor(String name, Object value) throws IOException {
        PropertyPersistor persistor = persistors.get(name);

        // Creating property store if it didn't exist
        if (persistor == null) {
            Class<? extends Object> etype = value.getClass();
            NioSerializer<?> s = SerializerManager.findSerializerFor(etype);

            FilePropertyStore pstore = new FilePropertyStore();
            pstore.setType(etype);
            pstore.setSerializer(s);
            pstore.open(ObjectStoreManager.getPropertyFile(storageDir, propertyName, name));

            persistor = new ValuePersistor(pstore);
            addPersistor(name, persistor);
        }

        return persistor;
    }

    @Override public void close() throws IOException {

        for (PropertyPersistor accessor : persistors.values()) {
            accessor.close();
        }
    }

    @Override public void flush() throws IOException {

        for (PropertyPersistor accessor : persistors.values()) {
            accessor.flush();
        }
    }

    public PropertyPersistor addPersistor(String propertyName, PropertyPersistor persistor) {
        return persistors.put(propertyName, persistor);
    }

    // Map value persistor
    static class ValuePersistor implements PropertyPersistor {
        private final FilePropertyStore<Object> store;

        public ValuePersistor(FilePropertyStore<Object> pstore) {
            this.store = pstore;
        }

        @Override public Object load(long identifier, String propertyPath) throws IOException {

            // TODO long ids may be supported later...
            // TODO Field name check
            return store.get((int) identifier);
        }

        @Override public void store(long identifier, String propertyPath, Object value)
            throws IOException {

            // TODO long ids may be supported later...
            // TODO Field name check
            store.update((int) identifier, value);
        }

        @Override public void close() throws IOException {
            store.close();
        }

        @Override public void flush() throws IOException {
            store.flush();
        }
    }
}
