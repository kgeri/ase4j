package org.ogreg.common.utils;

/**
 * Common JVM memory display and management utilities.
 * 
 * @author Gergely Kiss
 */
public class MemoryUtils {

	/**
	 * Returns the currently used memory in bytes.
	 * 
	 * @return
	 */
	public static long usedMem() {
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}
}
