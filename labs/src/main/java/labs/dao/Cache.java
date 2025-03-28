package labs.dao;

import labs.model.Day;
import labs.model.Meal;
import labs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Cache {
//    private Map<Integer, Product> productMap;
//    private Map<Integer, Meal> mealMap;
//    private Map<Integer, Day> dayMap;
//
//    public Cache() {
//        productMap = new HashMap<>();
//        mealMap = new HashMap<>();
//        dayMap = new HashMap<>();
//    }

    private Map<String, CacheItem> map;
//    private long sizeInBytes = 0;

    public Cache() {
        map = new HashMap<>();
    }

//    public void addObject(String key, CacheItem item, int sizeInBytes) {
//        if (this.sizeInBytes <= 1048576) {
//            map.put(key, item);
//            this.sizeInBytes += sizeInBytes;
//            log.info("size = " + this.sizeInBytes);
//        } else {
//            long oldestAccessTime = System.currentTimeMillis();
//            String oldestObjKey = "";
//            for (String objKey : map.keySet()) {
//                CacheItem value = map.get(objKey);
//                if (value.getLastAccessTime() <= oldestAccessTime) {
//                    oldestAccessTime = value.getLastAccessTime();
//                    oldestObjKey = objKey;
//                }
//            }
//            map.remove(oldestObjKey);
//            map.put(key, item);
//        }
//    }

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

//    public void addObject(String key, CacheItem item) {
//        if (this.sizeInBytes <= 1048576) {
//            long amountOfMemoryBefore = Runtime.getRuntime().freeMemory();
//            map.put(key, item);
//            sizeInBytes += amountOfMemoryBefore - Runtime.getRuntime().freeMemory();
//            log.info("size = " + sizeInBytes);
//        } else {
//            long oldestAccessTime = System.currentTimeMillis();
//            String oldestObjKey = "";
//            for (String objKey : map.keySet()) {
//                CacheItem value = map.get(objKey);
//                if (value.getLastAccessTime() <= oldestAccessTime) {
//                    oldestAccessTime = value.getLastAccessTime();
//                    oldestObjKey = objKey;
//                }
//            }
//            map.remove(oldestObjKey);
//            map.put(key, item);
//        }
//    }

    public Object getObject(String key) {
        CacheItem item = map.get(key);
        if (item == null) {
            return null;
        }
        item.setLastAccessTime(System.currentTimeMillis());
        return item.getObject();
    }

//    public Object removeObject(String key, int sizeOfRemovedObject) {
//        sizeInBytes -= sizeOfRemovedObject;
//        return map.remove(key).getObject();
//    }
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
