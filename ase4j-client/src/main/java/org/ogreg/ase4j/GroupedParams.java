package org.ogreg.ase4j;

import org.ogreg.ase4j.AssociationStore.Operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Grouped association parameters for storing and querying.
 * <p>
 * Used to specify which association stores need to be accessed and with what
 * multipliers.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class GroupedParams extends Params {
	private static final long serialVersionUID = 1158867616552676901L;

	/** The multipliers used when accessing the stores, keyed by the store ids. */
	private Map<String, Float> multipliers = new HashMap<String, Float>();

	public GroupedParams(String id, float multiplier) {
		this(id, multiplier, Operation.SUM);
	}

	public GroupedParams(String id, float multiplier, Operation op) {
		super(op);
		multipliers.put(id, multiplier);
	}

	/**
	 * Sets the <code>multiplier</code> for <code>storeId</code>.
	 * 
	 * @param storeId
	 * @param multiplier
	 * @return This instance for method chaining
	 */
	public GroupedParams set(String storeId, float multiplier) {
		this.multipliers.put(storeId, Float.valueOf(multiplier));

		return this;
	}

	/**
	 * Returns the group identifiers to access.
	 * 
	 * @return
	 */
	public Set<String> getGroups() {
		return multipliers.keySet();
	}

	/**
	 * Returns the group multipliers keyed by their group ids.
	 * 
	 * @return
	 */
	public Map<String, Float> getMultipliers() {
		return multipliers;
	}

	public static GroupedParams ensureNotNull(Params params) {

		if (params == null) {
			throw new IllegalArgumentException(
					"Operations on grouped AssociationStores must specify a GroupedParams parameter which must not be null",
					null);
		}

		try {
			return (GroupedParams) params;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"Operations on grouped AssociationStores must specify a GroupedParams parameter",
					e);
		}
	}
}
