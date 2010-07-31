package org.ogreg.common;

/**
 * Signals an error in the configuration.
 * 
 * @author Gergely Kiss
 */
public class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 1752209986550885794L;

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(Throwable cause) {
		super(cause);
	}

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
