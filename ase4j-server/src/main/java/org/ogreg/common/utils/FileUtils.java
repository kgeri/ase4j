package org.ogreg.common.utils;

import java.io.File;
import java.io.IOException;

/**
 * Common file helper methods.
 * 
 * @author Gergely Kiss
 */
public abstract class FileUtils {

	/**
	 * Attempts to safely rename <code>source</code> to <code>dest</code>.
	 * <p>
	 * Deletes the <code>dest</code> if it already exist.
	 * </p>
	 * 
	 * @param source
	 * @param dest
	 * @throws IOException if the rename operation failed
	 */
	public static synchronized void renameTo(File source, File dest) throws IOException {
		if (dest.exists() && !dest.delete()) {
			throw new IOException("Failed to delete " + dest);
		}

		if (!source.renameTo(dest)) {
			throw new IOException("Failed to rename " + source + " to " + dest);
		}
	}
}
