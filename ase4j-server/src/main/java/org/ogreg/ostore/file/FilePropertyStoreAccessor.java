package org.ogreg.ostore.file;

/**
 * SPI interface for accessors which access properties stored in files.
 *
 * @author  Gergely Kiss
 */
interface FilePropertyStoreAccessor {

    /**
     * Sets the property store on this accessor.
     *
     * @param  store
     */
    void setPropertyStore(FilePropertyStore<?> store);
}
