package org.simpleflatmapper.map.mapper;

import org.simpleflatmapper.map.FieldKey;
import org.simpleflatmapper.map.FieldMapper;
import org.simpleflatmapper.map.SetRowMapper;
import org.simpleflatmapper.map.SourceFieldMapper;
import org.simpleflatmapper.map.MapperConfig;
import org.simpleflatmapper.map.property.FieldMapperColumnDefinition;
import org.simpleflatmapper.map.context.MappingContextFactoryBuilder;
import org.simpleflatmapper.reflect.meta.ClassMeta;
import org.simpleflatmapper.util.Enumerable;
import org.simpleflatmapper.util.UnaryFactory;

/**
 * @param <T> the targeted type of the mapper
 */
public final class SetRowMapperBuilder<ROW, SET, T, K extends FieldKey<K>, E extends Exception>  {

    private final ConstantSourceMapperBuilder<ROW, T, K> constantSourceMapperBuilder;

    protected final MapperConfig<K, FieldMapperColumnDefinition<K>> mapperConfig;
    protected final MappingContextFactoryBuilder<? super ROW, K> mappingContextFactoryBuilder;
    private final UnaryFactory<SET, Enumerable<ROW>> enumerableFactory;

    /**
     * @param classMeta                  the meta for the target class.
     * @param parentBuilder              the parent builder, null if none.
     * @param mapperConfig               the mapperConfig.
     * @param mapperSource               the Mapper source.
     * @param keyFactory
     * @param enumerableFactory
     */
    public SetRowMapperBuilder(
            final ClassMeta<T> classMeta,
            MappingContextFactoryBuilder<? super ROW, K> parentBuilder,
            MapperConfig<K, FieldMapperColumnDefinition<K>> mapperConfig,
            MapperSource<? super ROW, K> mapperSource,
            KeyFactory<K> keyFactory,
            UnaryFactory<SET, Enumerable<ROW>> enumerableFactory) {
        this.enumerableFactory = enumerableFactory;


        this.constantSourceMapperBuilder =
                new ConstantSourceMapperBuilder<ROW, T, K>(
                        mapperSource,
                        classMeta,
                        mapperConfig,
                        parentBuilder,
                        keyFactory);
        this.mapperConfig = mapperConfig;
        this.mappingContextFactoryBuilder = parentBuilder;
    }

    /**
     * @return a new newInstance of the jdbcMapper based on the current state of the builder.
     */
    public final SetRowMapper<ROW, SET, T, E> mapper() {
        SourceFieldMapper<ROW, T> mapper = sourceFieldMapper();

        SetRowMapper<ROW, SET, T, E> m;
        
        if (constantSourceMapperBuilder.hasJoin()) {
            m = new JoinMapper<ROW, SET, T, E>(mapper, mapperConfig.consumerErrorHandler(), mappingContextFactoryBuilder.newFactory(), enumerableFactory);
        } else {
            m = new StaticSetRowMapper<ROW, SET, T, E>(mapper, mapperConfig.consumerErrorHandler(), mappingContextFactoryBuilder.newFactory(), enumerableFactory);
        }
        
        return m;
        
        
    }
    
    public final SourceFieldMapper<ROW, T> sourceFieldMapper() {
        return constantSourceMapperBuilder.mapper();
    }
    
    public boolean hasJoin() {
        return constantSourceMapperBuilder.hasJoin();
    }


    public final void addMapper(FieldMapper<ROW, T> mapper) {
        constantSourceMapperBuilder.addMapper(mapper);
    }


    public final void addMapping(K key, FieldMapperColumnDefinition<K> columnDefinition) {
        constantSourceMapperBuilder.addMapping(key, columnDefinition);
    }


    public MapperConfig<K, FieldMapperColumnDefinition<K>>  mapperConfig() {
        return mapperConfig;
    }

    public MappingContextFactoryBuilder<? super ROW, K> getMappingContextFactoryBuilder() {
        return mappingContextFactoryBuilder;
    }
}