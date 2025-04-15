package labs.dao;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CacheItem {
    private Object object;
    private String key;

    public CacheItem(Object object, String key) {
        this.object = object;
        this.key = key;
    }
}
