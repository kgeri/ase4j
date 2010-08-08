package org.ogreg.common.utils;

import java.io.File;
import java.io.IOException;


/**
 * Common file helper methods.
 *
 * @author  Gergely Kiss
 */
public abstract class FileUtils {

    /**
     * Attempts to safely rename <code>source</code> to <code>dest</code>.
     *
     * <p>Deletes the <code>dest</code> if it already exist.</p>
     *
     * @param   source
     * @param   dest
     *
     * @throws  IOException  if the rename operation failed
     */
    public static synchronized void renameTo(File source, File dest) throws IOException {

        if (dest.exists() && !dest.delete()) {
            throw new IOException("Failed to delete " + dest);
        }

        if (!source.renameTo(dest)) {
            throw new IOException("Failed to rename " + source + " to " + dest);
        }
    }

    /**
     * Creates the directory structure of the specified <code>dir</code>.
     *
     * @param   dir
     *
     * @throws  IOException  if the path is not a directory or we couldn't create it
     */
    public static void mkdirs(File dir) throws IOException {

        if (dir.exists()) {

            if (dir.isDirectory()) {
                return;
            } else {
                throw new IOException("Path exists, but is not a directory: " +
                    dir.getAbsolutePath());
            }
        } else if (!dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
        }
    }
}
