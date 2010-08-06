package org.ogreg.ostore.file;

import org.ogreg.ostore.PropertyPersistor;

import java.io.IOException;


/**
 * File-based property persistor.
 *
 * @author  Gergely Kiss
 */
class FilePropertyPersistor implements PropertyPersistor {
    private final FilePropertyStore<Object> store;

    public FilePropertyPersistor(FilePropertyStore<Object> store) {
        this.store = store;
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
