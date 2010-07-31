package org.ogreg.ostore;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Simple property field accessor.
 * 
 * @author Gergely Kiss
 */
class FieldPropertyAccessor implements FieldAccessor {
	private final Field field;
	private PropertyStore<Object> store;

	public FieldPropertyAccessor(Field field, PropertyStore<Object> store) {
		this.field = field;
		this.store = store;
	}

	@Override
	public Object load(long identifier, String propertyPath) throws IOException {
		// TODO long ids may be supported later...
		// TODO Field name check
		return store.get((int) identifier);
	}

	@Override
	public void store(long identifier, String propertyPath, Object value) throws IOException {
		// TODO long ids may be supported later...
		// TODO Field name check
		store.update((int) identifier, value);
	}

	@Override
	public String getFieldName() {
		return field.getName();
	}

	@Override
	public Object getFrom(Object source) throws IllegalArgumentException, IllegalAccessException {
		return field.get(source);
	}

	@Override
	public void setTo(Object source, Object value) throws IllegalArgumentException,
			IllegalAccessException {
		field.set(source, value);
	}

	@Override
	public void close() throws IOException {
		store.close();
	}

	@Override
	public void flush() throws IOException {
		store.flush();
	}
}