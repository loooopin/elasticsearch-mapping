package io.github.loooopin.elasticsearch.util;

import java.util.Collection;
import java.util.Map;

/**
 * User: huxiaodong24
 * Date: 2021/10/14
 * Time: 20:03
 * Description:
 */
public class CollectionUtils {
    public static final boolean isEmpty(Collection collection){
        return collection==null||collection.size()==0;
    }

    public static final boolean isNotEmpty(Collection collection){
        return !isEmpty(collection);
    }

    public static final boolean isEmpty(Map map){
        return map==null||map.size()==0;
    }

    public static final boolean isNotEmpty(Map map){
        return !isEmpty(map);
    }
}
