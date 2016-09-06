package org.simpleflatmapper.map.mapper;

import org.simpleflatmapper.map.FieldKey;
import org.simpleflatmapper.map.MapperBuilderErrorHandler;
import org.simpleflatmapper.map.MapperBuildingException;
import org.simpleflatmapper.reflect.meta.ClassMeta;
import org.simpleflatmapper.reflect.meta.DirectClassMeta;
import org.simpleflatmapper.reflect.meta.PropertyFinder;
import org.simpleflatmapper.reflect.meta.PropertyMeta;
import org.simpleflatmapper.map.PropertyNameMatcherFactory;
import org.simpleflatmapper.util.ForEachCallBack;
import org.simpleflatmapper.util.Predicate;
import org.simpleflatmapper.util.TypeHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class PropertyMappingsBuilder<T, K extends FieldKey<K>, D extends ColumnDefinition<K, D>> {

	protected final PropertyFinder<T> propertyFinder;
	
	protected final List<PropertyMapping<T, ?, K, D>> properties = new ArrayList<PropertyMapping<T, ?, K, D>>();

	protected final PropertyNameMatcherFactory propertyNameMatcherFactory;

	private final MapperBuilderErrorHandler mapperBuilderErrorHandler;
	private final ClassMeta<T> classMeta;

	protected boolean modifiable = true;

	private final Predicate<PropertyMeta<?, ?>> isValidMeta;

	public PropertyMappingsBuilder(final ClassMeta<T> classMeta,
								   final PropertyNameMatcherFactory propertyNameMatcherFactory,
								   final MapperBuilderErrorHandler mapperBuilderErrorHandler,
								   final Predicate<PropertyMeta<?, ?>> isValidMeta)  throws MapperBuildingException {
		this.mapperBuilderErrorHandler = mapperBuilderErrorHandler;
		this.isValidMeta = isValidMeta;
		this.propertyFinder = classMeta.newPropertyFinder();
		this.propertyNameMatcherFactory = propertyNameMatcherFactory;
		this.classMeta = classMeta;
	}

	public PropertyMappingsBuilder(final ClassMeta<T> classMeta,
								   final PropertyNameMatcherFactory propertyNameMatcherFactory,
								   final MapperBuilderErrorHandler mapperBuilderErrorHandler,
								   final Predicate<PropertyMeta<?, ?>> isValidMeta,
								   final PropertyFinder<T> propertyFinder)  throws MapperBuildingException {
		this.mapperBuilderErrorHandler = mapperBuilderErrorHandler;
		this.isValidMeta = isValidMeta;
		this.propertyFinder = propertyFinder != null ? propertyFinder : classMeta.newPropertyFinder();
		this.propertyNameMatcherFactory = propertyNameMatcherFactory;
		this.classMeta = classMeta;
	}

	
    public <P> PropertyMeta<T, P> addProperty(final K key, final D columnDefinition) {
		
		if (!modifiable) throw new IllegalStateException("Builder not modifiable");

        if (columnDefinition.ignore()) {
            properties.add(null);
            return null;
        }

		final PropertyMeta<T, P> prop = addPropertyIfPresent(key, columnDefinition);

		if (prop == null) {
			mapperBuilderErrorHandler.propertyNotFound(classMeta.getType(), key.getName());
            return null;
		} else {
            return prop;
		}
	}

	@SuppressWarnings("unchecked")
	public <P> PropertyMeta<T, P> addPropertyIfPresent(final K key, final D columnDefinition) {
		final PropertyMeta<T, P> prop =
				(PropertyMeta<T, P>) propertyFinder.findProperty(propertyNameMatcherFactory.newInstance(key));

		if (prop == null || !isValidMeta.test(prop)) {
			properties.add(null);
			return null;
		} else {
			addProperty(key, columnDefinition, prop);
			return prop;
		}
	}

	public <P> void addProperty(final K key, final D columnDefinition, final PropertyMeta<T, P> prop) {
		if (columnDefinition.hasCustomSource()) {
            Type type = prop.getPropertyType();

            if (!checkTypeCompatibility(key, columnDefinition.getCustomSourceReturnType(), type)) {
				properties.add(null);
				return;
			}
		}
		properties.add(new PropertyMapping<T, P, K, D>(prop, key, columnDefinition));
	}

	private boolean checkTypeCompatibility(K key, Type customSourceReturnType, Type propertyMetaType) {
		if (customSourceReturnType == null) {
			// cannot determine type
			return true;
		} else if(!areCompatible(propertyMetaType, customSourceReturnType)) {
			mapperBuilderErrorHandler.customFieldError(key, "Incompatible customReader type " + customSourceReturnType +  " expected " + propertyMetaType);
			return false;
		}
		return true;
	}

	private boolean areCompatible(Type propertyMetaType, Type customSourceReturnType) {
		Class<?> propertyMetaClass = TypeHelper.toBoxedClass(TypeHelper.toClass(propertyMetaType));
		Class<?> customSourceReturnClass = TypeHelper.toBoxedClass(TypeHelper.toClass(customSourceReturnType));
		return propertyMetaClass.isAssignableFrom(customSourceReturnClass);
	}


	public List<K> getKeys() {
		modifiable = false;
		
		List<K>  keys = new ArrayList<K>(properties.size());
        for (PropertyMapping<T, ?, K, D> propMapping : properties) {
            if (propMapping != null) {
                keys.add(propMapping.getColumnKey());
            } else {
                keys.add(null);
            }

        }
		return keys;
	}

	public void forEachConstructorProperties(ForEachCallBack<PropertyMapping<T, ?, K, D>> handler)  {
		modifiable = false;

        for (PropertyMapping<T, ?, K, D> property : properties) {
            if (property != null) {
                PropertyMeta<T, ?> propertyMeta = property.getPropertyMeta();
                if (propertyMeta != null && propertyMeta.isConstructorProperty()) {
                    handler.handle(property);
                }
            }
        }
	}
	
	public <H extends ForEachCallBack<PropertyMapping<T, ?, K, D>>> H forEachProperties(H handler)  {
		return forEachProperties(handler, -1 );
	}
	
	public <F extends ForEachCallBack<PropertyMapping<T, ?, K, D>>> F forEachProperties(F handler, int start)  {
		return forEachProperties(handler, start, -1 );
	}
	
	public <F extends ForEachCallBack<PropertyMapping<T, ?, K, D>>> F forEachProperties(F handler, int start, int end)  {
		modifiable = false;
        for (PropertyMapping<T, ?, K, D> prop : properties) {
            if (prop != null
                    && (prop.getColumnKey().getIndex() >= start || start == -1)
                    && (prop.getColumnKey().getIndex() < end || end == -1)) {
                handler.handle(prop);
            }
        }
		return handler;
	}


	public PropertyFinder<T> getPropertyFinder() {
		modifiable = false;
		return propertyFinder;
	}


	public int size() {
		return properties.size();
	}


    public boolean isDirectProperty() {
        return  (properties.size() == 1 && properties.get(0) != null && properties.get(0).getPropertyMeta() instanceof DirectClassMeta.DirectPropertyMeta);
    }

	public int maxIndex() {
		int i = -1;
		for (PropertyMapping<T, ?, K, D> prop : properties) {
			if (prop != null) {
				i = Math.max(i, prop.getColumnKey().getIndex());
			}
		}
		return i;
	}

	public boolean hasKey(Predicate<? super K> predicate) {
		for (PropertyMapping<T, ?, K, D> propMapping : properties) {
			if (propMapping != null && predicate.test(propMapping.getColumnKey())) {
				return true;
			}
		}

		return false;
	}
}
