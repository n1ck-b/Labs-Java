package labs.dao;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionCache {
    private Map<String, Object> map;
    private static final int MAX_ENTRIES = 200;

    public SessionCache() {
        map = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                if (size() > MAX_ENTRIES) {
                    log.info(eldest.getValue().getClass().getSimpleName() + " with key = '" +
                            eldest.getKey() + "' was removed from cache");
                }
                return size() > MAX_ENTRIES;
            }
        };
    }

    public CacheItem addObject(String key, Object item) {
        map.put(key, item);
        return new CacheItem(item, key);
    }

    public Object getObject(String key) {
        return map.get(key);
    }

    public CacheItem removeObject(String key) {
        Object object = map.remove(key);
        return new CacheItem(object, key);
    }

    public boolean exists(String key) {
        return map.get(key) != null;
    }
}