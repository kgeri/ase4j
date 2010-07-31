package org.ogreg.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.ogreg.common.nio.NioUtils;

/**
 * JAXB configurator base class.
 * 
 * @param <C> The type of the configuration
 * @author Gergely Kiss
 */
public abstract class BaseJaxbManager<C> {
	private final Class<C> type;

	public BaseJaxbManager(Class<C> type) {
		this.type = type;
	}

	/**
	 * Adds the given configuration file to the current configuration.
	 * 
	 * @param configurationFile The source file to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(File configurationFile) throws ConfigurationException {
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(configurationFile);
			add(fis);
		} catch (FileNotFoundException e) {
			throw new ConfigurationException(e);
		} finally {
			NioUtils.closeQuietly(fis);
		}
	}

	/**
	 * Adds the given Object Store configuration resource to the current
	 * configuration.
	 * 
	 * @param configurationResource The resource to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(String configurationResource) throws ConfigurationException {
		FileInputStream fis = null;

		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			InputStream is = null;

			if (contextClassLoader != null) {
				is = contextClassLoader.getResourceAsStream(configurationResource);
			}
			if (is == null) {
				is = getClass().getClassLoader().getResourceAsStream(configurationResource);
			}

			if (is == null) {
				throw new ConfigurationException("Resource was not found: " + configurationResource);
			}

			add(is);
		} finally {
			NioUtils.closeQuietly(fis);
		}
	}

	/**
	 * Adds the given Object Store configuration to the current configuration.
	 * 
	 * @param configurationStream The source stream to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(InputStream configurationStream) throws ConfigurationException {
		try {
			JAXBContext jc = JAXBContext.newInstance(type.getPackage().getName());
			Unmarshaller um = jc.createUnmarshaller();

			@SuppressWarnings("unchecked")
			C storage = (C) um.unmarshal(configurationStream);

			add(storage);
		} catch (JAXBException e) {
			throw new ConfigurationException(e);
		}
	}

	/**
	 * The implementation must specify the action taken when a configuration
	 * root of type <code>C</code> was successfully read.
	 * 
	 * @param config
	 * @throws ObjectStoreException
	 */
	public abstract void add(C config) throws ConfigurationException;

	protected Class<?> forName(String className) throws ConfigurationException {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e);
		}
	}
}