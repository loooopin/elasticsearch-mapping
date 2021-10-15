package io.github.loooopin.support;

import io.github.loooopin.support.annotations.EsComparison;
import io.github.loooopin.support.annotations.EsField;
import io.github.loooopin.support.annotations.EsIndex;
import io.github.loooopin.support.enums.ComparisonEnums;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:56
 * Description: 用于缓存实体中的注解和成员变量，省去每次都要反射获取
 */
public final class EsBeanContext {
    //实体对应的索引名
    private static Map<Class, String> indexMap;
    //查询条件map
    private static Map<Class, Map<String, ComparisonEnums>> comparesMaps;
    //类中包含的所有字段
    private static Map<Class, Map<String, Field>> fieldsMaps;
    //成员变量与索引字段映射关系
    private static Map<Class, Map<String, String>> fieldMappingsMaps;
    //索引字段与成员变量映射关系
    private static Map<Class, Map<String, String>> mappingToJavaFieldsMaps;

    static {
        indexMap = new HashMap();
        comparesMaps = new HashMap();
        fieldsMaps = new HashMap();
        fieldMappingsMaps = new HashMap();
        mappingToJavaFieldsMaps = new HashMap();
    }

    public static String getIndex(Class clazz) {
        if (!indexMap.containsKey(clazz)) {
            indexMap.put(clazz, assembleIndex(clazz));
        }
        return indexMap.get(clazz);
    }

    private static String assembleIndex(Class clazz) {
        EsIndex esIndex = EsIndex.class.cast(clazz.getDeclaredAnnotation(EsIndex.class));
        if (esIndex == null) {
            return null;
        }
        return esIndex.value();
    }

    /**
     * 根据class获得查询条件
     * 如果没有就根据注解生成
     *
     * @param clazz
     * @return
     */
    public static Map<String, ComparisonEnums> getComparesMap(Class clazz) {
        if (!comparesMaps.containsKey(clazz)) {
            comparesMaps.put(clazz, assembleComparesMap(clazz));
        }
        return comparesMaps.get(clazz);
    }

    /**
     * 根据注解生成指定class的查询条件
     *
     * @param clazz
     * @return
     */
    private static Map<String, ComparisonEnums> assembleComparesMap(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, ComparisonEnums> comparesMap = new HashMap();
        for (Field field : fields) {
            EsComparison esComparison = field.getAnnotation(EsComparison.class);
            if (esComparison == null) {
                continue;
            }
            comparesMap.put(field.getName(), esComparison.value());
        }
        return comparesMap;
    }

    public static Map<String, Field> getFieldsMap(Class clazz) {
        if (!fieldsMaps.containsKey(clazz)) {
            fieldsMaps.put(clazz, assembleFieldsMap(clazz));
        }
        return fieldsMaps.get(clazz);
    }

    private static Map<String, Field> assembleFieldsMap(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, Field> fieldsMap = new HashMap();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldsMap.put(field.getName(), field);
        }
        return fieldsMap;
    }

    /**
     * 根据class获得成员变量与表字段的对应关系
     * 如果没有就根据注解生成
     *
     * @param clazz
     * @return
     */
    public static Map<String, String> getFieldMappingsMap(Class clazz) {
        if (!fieldMappingsMaps.containsKey(clazz)) {
            fieldMappingsMaps.put(clazz, assembleFieldMappingsMap(clazz));
        }
        return fieldMappingsMaps.get(clazz);
    }

    /**
     * 根据注解生成指定class中成员变量与表字段的对应关系
     *
     * @param clazz
     * @return
     */
    private static Map<String, String> assembleFieldMappingsMap(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, String> fieldMappingsMap = new HashMap();
        for (Field field : fields) {
            EsField esField = field.getAnnotation(EsField.class);
            if (esField == null) {
                continue;
            }
            String value = esField.value();
            fieldMappingsMap.put(field.getName(), value);
        }
        return fieldMappingsMap;
    }

    public static Map<String, String> getMappingToJavaFieldsMap(Class clazz) {
        if (!mappingToJavaFieldsMaps.containsKey(clazz)) {
            mappingToJavaFieldsMaps.put(clazz, assembleMappingToJavaFieldsMap(clazz));
        }
        return mappingToJavaFieldsMaps.get(clazz);
    }

    private static Map<String, String> assembleMappingToJavaFieldsMap(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, String> fieldMappingsMap = new HashMap();
        for (Field field : fields) {
            EsField esField = field.getAnnotation(EsField.class);
            if (esField == null) {
                continue;
            }
            if(esField.onlyCompare()){
                continue;
            }
            String value = esField.value();
            fieldMappingsMap.put(value, field.getName());
        }
        return fieldMappingsMap;
    }
}
