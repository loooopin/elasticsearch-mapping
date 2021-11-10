package io.github.loooopin.elasticsearch.util;

/**
 * User: loooopin
 * Date: 2021/10/26
 * Time: 20:39
 * Description:
 */
public final class StringUtils {
    public static final boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static final boolean isNotEmpty(String str){
        return !isEmpty(str);
    }
}

