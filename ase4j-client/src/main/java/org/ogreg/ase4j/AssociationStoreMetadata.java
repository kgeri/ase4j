package org.ogreg.ase4j;

import org.ogreg.ostore.ObjectStoreMetadata;


/**
 * Association storage description and schema information.
 *
 * @author  Gergely Kiss
 */
public class AssociationStoreMetadata {
    private ObjectStoreMetadata fromMetadata;
    private ObjectStoreMetadata toMetadata;

    public AssociationStoreMetadata(ObjectStoreMetadata fromMetadata,
        ObjectStoreMetadata toMetadata) {
        this.fromMetadata = fromMetadata;
        this.toMetadata = toMetadata;
    }

    public ObjectStoreMetadata getFromMetadata() {
        return fromMetadata;
    }

    public ObjectStoreMetadata getToMetadata() {
        return toMetadata;
    }
}
