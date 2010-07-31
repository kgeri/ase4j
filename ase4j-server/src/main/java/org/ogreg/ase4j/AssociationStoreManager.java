package org.ogreg.ase4j;

import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;
import org.ogreg.config.Associationstore;
import org.ogreg.ostore.ObjectStore;

/**
 * Association store configurator service.
 * <p>
 * The configuration XML resources must comply to associationstore.xsd
 * </p>
 * 
 * @author Gergely Kiss
 */
public class AssociationStoreManager extends BaseJaxbManager<Associationstore> {

	public AssociationStoreManager() {
		super(Associationstore.class);
	}

	@Override
	protected void add(Associationstore config) throws ConfigurationException {
		// TODO Auto-generated method stub

	}

	public <F, T> AssociationStore<F, T> createStore(String id) {

		return null;
	}
}
