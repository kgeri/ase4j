package org.ogreg.common.utils;

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX registry convenience methods.
 * 
 * @author Gergely Kiss
 */
public abstract class MBeanUtils {
	private static final Logger log = LoggerFactory.getLogger(MBeanUtils.class);

	private static final String MBEAN_DOMAIN = "org.ogreg";

	/**
	 * Registers the given <code>mbean</code> on the domain
	 * {@link #MBEAN_DOMAIN} and <code>name</code>.
	 * 
	 * @param mbean
	 * @param name
	 */
	public static void register(Object mbean, String name) {
		try {
			ObjectName on = new ObjectName(MBEAN_DOMAIN + ":type=" + name);
			ManagementFactory.getPlatformMBeanServer().registerMBean(mbean, on);
			log.info("MBean {} registered successfully", on);
		} catch (Exception e) {
			log.error("Failed to register {} as an MBean called: {} ({})", new Object[] {
					mbean.getClass().getName(), name, e.getLocalizedMessage() });
			log.debug("Failure trace", e);
		}
	}
}
