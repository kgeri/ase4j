package org.ogreg.ostore;

import java.util.Map;


/**
 * SPI interface for {@link ObjectStore} property accessors.
 *
 * <p>Property accessors are used to handle <b>all the properties</b> of the stored Java entity, and for creting new
 * entities.</p>
 *
 * @author  Gergely Kiss
 */
public interface EntityAccessor {

    /**
     * Returns the property value from the specified object.
     *
     * @param   source
     * @param   propertyName
     *
     * @return
     *
     * @throws  IllegalAccessException  if the field value cannot be read
     */
    Object getFrom(Object source, String propertyName) throws IllegalAccessException;

    /**
     * Sets the property value on the specified object.
     *
     * @param   source
     * @param   propertyName
     * @param   value
     *
     * @throws  IllegalAccessException  if the field value cannot be set
     */
    void setTo(Object source, String propertyName, Object value) throws IllegalAccessException;

    /**
     * Returns a new instance of the stored type.
     *
     * @return  An instance of the stored type
     *
     * @throws  InstantiationException  if instantiation failed
     */
    Object newInstance() throws InstantiationException;

    /**
     * Sets the accessible properties for this instance.
     *
     * @param  properties
     */
    void setProperties(Map<String, Class<?>> properties);

    /**
     * Returns the type name accessed by this instance.
     *
     * @return
     */
    String getTypeName();
}
