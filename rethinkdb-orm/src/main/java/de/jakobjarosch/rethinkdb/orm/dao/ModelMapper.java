package de.jakobjarosch.rethinkdb.orm.dao;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlGeo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class ModelMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelMapper.class);

    private final ObjectMapper mapper;

    public ModelMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Maps a object to a RethinkDB ready map.
     *
     * @param obj Object which should be converted.
     * @return A RethinkDB ready map which contains only primitive and ReqlGeo data types.
     */
    public Map<?, ?> map(Object obj) {
        Map<?, ?> map = mapper.convertValue(obj, Map.class);
        map.forEach((k, v) -> reconstructGeoObjects(map, k, v));
        return map;
    }

    /**
     * Converts a RethinkDB response into a given Model.
     *
     * @param map   RethinkDB response as a map.
     * @param clazz Class which should be converted into.
     * @return The converted RethinkDB response.
     */
    public <T> T map(Map<?, ?> map, Class<T> clazz) {
        return mapper.convertValue(map, clazz);
    }

    /**
     * The method reconstructs all previous {@link ReqlGeo} instances back from their serialized representation.
     * Those instances are required, otherwise RethinkDB would not store coordinates in the correct data type.
     * <br>
     * The whole reconstruction is required because jackson is not able the preserve some types when converting
     * to a generic Map.
     *
     * @param parentMap The map which contains the given key and value
     * @param key       The key of the given value
     * @param value     The value which might be a map and contain the {@link ReqlGeo} class identifier.
     */
    @SuppressWarnings("unchecked")
    private void reconstructGeoObjects(Map<?, ?> parentMap, Object key, Object value) {
        // We only dig deeper if it's a map, otherwise we are done.
        if (value instanceof Map) {
            Map<?, ?> childs = (Map<?, ?>) value;
            if (childs.containsKey(ReqlGeo.MAPPING_CLASS_KEY)) {
                // The childs contain the required key used to identify ReqlGeo data types.
                // The map will be replaced with a new instance of the ReqlGeo sub type.
                try {
                    Class<?> geoClassName = Class.forName(String.valueOf(childs.get(ReqlGeo.MAPPING_CLASS_KEY)));
                    final Object geoEntry = map(childs, geoClassName);
                    ((Map) parentMap).put(key, geoEntry);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Failed to deserialize ReqlGeo class.", e);
                }
            } else {
                // This is no ReqlGeo specific child, try to dig deeper with a recursive call.
                childs.forEach((k, v) -> reconstructGeoObjects(childs, k, v));
            }
        }
    }
}
