package org.ogreg.common.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Support implementation for working with mapped storage.
 * 
 * @author Gergely Kiss
 */
public abstract class MappedFileSupport {
	protected FileChannel channel;
	protected MappedByteBuffer buffer;
	private FileLock lock;

    public void open(File file) throws IOException {

        if (this.channel != null) {
			close();
		}

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
		channel = raf.getChannel();
		lock = channel.lock();
		buffer = map(channel);
	}

    protected abstract MappedByteBuffer map(FileChannel channel) throws IOException;

    public synchronized void close() throws IOException {

		if (buffer != null) {
			NioUtils.unmap(buffer);
			buffer = null;
		}

        if (lock != null) {
			lock.release();
			lock = null;
		}

        if (channel != null) {
			channel.close();
			channel = null;
		}
	}

    public synchronized void flush() throws IOException {
		buffer.force();
		channel.force(true);
	}

    @Override
	protected void finalize() throws Throwable {
		close();
	}
}
