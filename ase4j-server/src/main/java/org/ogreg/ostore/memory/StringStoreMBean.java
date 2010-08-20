package org.ogreg.ostore.memory;

import java.io.IOException;

import org.ogreg.ostore.ObjectStoreMBean;

/**
 * JMX interface for the {@link StringStore}.
 * 
 * @author Gergely Kiss
 */
public interface StringStoreMBean extends ObjectStoreMBean {

	/**
	 * Dumps the contents of this store to the specified <code>path</code>, or a
	 * temporary file if it was null.
	 * <p>
	 * The dump file will be in plain text format, UTF-8 encoded, containing the
	 * stored strings separated by newlines (\n). Please note that the dump
	 * process is not synchronized - it may be possible that entries are added
	 * while writing the dump file.
	 * </p>
	 * 
	 * @param path The path of the output file - must <b>not</b> exist
	 * @throws IOException on dump failure
	 */
	void dump(String path) throws IOException;
}
