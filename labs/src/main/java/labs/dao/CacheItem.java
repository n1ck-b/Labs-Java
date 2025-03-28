package labs.dao;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CacheItem {
    private Object object;
    private long lastAccessTime;

    public CacheItem(Object object) {
        this.object = object;
        this.lastAccessTime = System.currentTimeMillis();
    }
}
