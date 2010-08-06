package org.ogreg.test;

import org.apache.commons.io.FileUtils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * File test support methods.
 *
 * @author  Gergely Kiss
 */
public abstract class FileTestSupport {

    public static void assertBinaryEqual(String expectedPath, String actualPath) {
        File expected = new File("src/test/resources/" + expectedPath);
        File actual = new File(actualPath);

        assertTrue(expected.exists(), "Test file does not exist: " + expected.getAbsolutePath());
        assertEquals(expected.length(), actual.length(), "Index size");
        assertEquals(readBytes(expected), readBytes(actual));
    }

    public static byte[] readBytes(File file) {
        FileInputStream is = null;
        byte[] res = new byte[(int) file.length()];

        try {
            is = new FileInputStream(file);
            is.read(res);

            return res;
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        } finally {

            if (is != null) {

                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static File createTempFile(String name) {
        File tmpFile = new File("target/" + name);

        if (tmpFile.exists() && !tmpFile.delete()) {
            System.err.println("Failed to delete file: " + tmpFile.getAbsolutePath());
        }

        return tmpFile;
    }

    public static File createTempDir(String name) throws IOException {
        File tmpFile = new File("target/" + name);

        FileUtils.deleteQuietly(tmpFile);
        FileUtils.forceMkdir(tmpFile);

        return tmpFile;
    }

    /**
     * Returns the total size of the file or directory (recursively).
     *
     * @param   file
     *
     * @return
     */
    public static long length(File file) {

        if (file.isDirectory()) {
            long len = 0;

            for (File child : file.listFiles()) {
                len += length(child);
            }

            return len;
        } else {
            return file.length();
        }
    }
}
