package org.ogreg.util;

/**
 * Common interface for callbacks.
 * <p>
 * Callbacks are useful for both asynchronous method calls, and for processing
 * large amounts of data hidden in internal structures.
 * </p>
 * 
 * @author Gergely Kiss
 * @param <T>
 */
public interface Callback<T> {

	/**
	 * Returns the result to the caller.
	 * 
	 * @param value
	 */
	void callback(T value);
}
