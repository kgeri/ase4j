package org.ogreg.common.utils;

import org.ogreg.common.ConfigurationException;

import java.lang.reflect.Field;


/**
 * Common methods for handling properties and property paths.
 *
 * @author  Gergely Kiss
 */
public abstract class PropertyUtils {

    /**
     * Converts the given property path to a single string (concatenates the path elements with dots).
     *
     * @param   path  The property path elements
     *
     * @return
     */
    public static String pathToString(String... path) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < path.length; i++) {

            if (i > 0) {
                sb.append('.');
            }

            sb.append(path[i]);
        }

        return sb.toString();
    }

    /**
     * Splits the path on the first dot.
     *
     * @param   path  The path to split
     *
     * @return  The { first, remaining } path elements of the path, or { original string, null }, if it cannot be
     *          determined. It is guaranteed that an array of size 2 will be returned, and its 0th element is never
     *          null.
     *
     * @throws  NullPointerException  if path is null
     */
    public static String[] splitFirstPathElement(String path) {
        int idx = path.indexOf('.');

        if ((idx < 0) || (idx >= (path.length() - 1))) {
            return new String[] { path, null };
        } else {
            return new String[] { path.substring(0, idx), path.substring(idx + 1) };
        }
    }

    /**
     * Returns the declared field of the <code>type</code> by its <code>name</code>.
     *
     * <p>As a side effect, the field will be made accessible.</p>
     *
     * @param   type  The type which declares the field
     * @param   name  The name of the declared field
     *
     * @return
     *
     * @throws  ConfigurationException
     */
    public static Field getField(Class<?> type, String name) {

        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);

            return field;
        } catch (SecurityException e) {
            throw new ConfigurationException(e);
        } catch (NoSuchFieldException e) {
            throw new ConfigurationException(e);
        }
    }
}
