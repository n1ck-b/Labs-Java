package labs.dao;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Cache {
    private Map<String, CacheItem> map;

    public Cache() {
        map = new HashMap<>();
    }

    public void addObject(String key, CacheItem item) {
        if (map.size() <= 200) {
            map.put(key, item);
        } else {
            long oldestAccessTime = System.currentTimeMillis();
            String oldestObjKey = "";
            for (String objKey : map.keySet()) {
                CacheItem value = map.get(objKey);
                if (value.getLastAccessTime() <= oldestAccessTime) {
                    oldestAccessTime = value.getLastAccessTime();
                    oldestObjKey = objKey;
                }
            }
            map.remove(oldestObjKey);
            map.put(key, item);
        }
    }

    public Object getObject(String key) {
        CacheItem item = map.get(key);
        if (item == null) {
            return null;
        }
        item.setLastAccessTime(System.currentTimeMillis());
        return item.getObject();
    }

    public Object removeObject(String key) {
        return map.remove(key).getObject();
    }

    public boolean exists(String key) {
        CacheItem item = map.get(key);
        if (item != null) {
            item.setLastAccessTime(System.currentTimeMillis());
        }
        return item != null;
    }

    public void updateObject(String key, CacheItem updatedItem) {
        updatedItem.setLastAccessTime(System.currentTimeMillis());
        map.put(key, updatedItem);
    }
}
