package org.ogreg.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.ogreg.common.nio.NioUtils;

/**
 * Serialization and deserialization helper methods.
 * 
 * @author Gergely Kiss
 */
public abstract class SerializationUtils {

	/**
	 * Reads an object from the specified file.
	 * 
	 * @param in The source file
	 * @param type The expected type of the object
	 * @return The deserialized object
	 * @throws IOException If the reading failed
	 */
	public static final <T> T read(File in, Class<T> type) throws IOException {
		InputStream is = null;

		try {
			is = new FileInputStream(in);
			return read(is, type);
		} finally {
			NioUtils.closeQuietly(is);
		}
	}

	/**
	 * Reads an object from the specified input stream.
	 * 
	 * @param is The source stream (won't be closed)
	 * @param type The expected type of the object
	 * @return The deserialized object
	 * @throws IOException If the reading failed
	 */
	public static final <T> T read(InputStream is, Class<T> type) throws IOException {
		ObjectInputStream ois = null;

		try {
			ois = new ObjectInputStream(is);
			Object obj = ois.readObject();
			return type.cast(obj);
		} catch (ClassNotFoundException e) {
			throw new IOException("Failed to read object", e);
		} catch (ClassCastException e) {
			throw new IOException("Failed to cast object to: " + type.getName(), e);
		}
	}

	/**
	 * Writes an object to the specified file.
	 * <p>
	 * Attempts to serialize the object in the safest way possible. First it
	 * writes the object to a temporary file, and then renames the temporary
	 * file to the destination file. This way if the serialization is
	 * interrupted, the original file will be preserved.
	 * </p>
	 * 
	 * @param out The target file
	 * @param object The object to serialize
	 * @throws IOException If the writing failed
	 */
	public static final <T> void write(File out, T object) throws IOException {
		File tmp = File.createTempFile(out.getName(), ".tmp", out.getParentFile());

		OutputStream os = null;
		try {
			os = new FileOutputStream(tmp);
			write(os, object);
		} finally {
			NioUtils.closeQuietly(os);
		}

		FileUtils.renameTo(tmp, out);
	}

	/**
	 * Writes an object to the specified output stream.
	 * 
	 * @param os The target stream (won't be closed)
	 * @param object The object to serialize
	 * @throws IOException If the writing failed
	 */
	public static final <T> void write(OutputStream os, T object) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(object);
	}
}
