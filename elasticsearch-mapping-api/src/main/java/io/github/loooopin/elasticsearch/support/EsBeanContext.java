package io.github.loooopin.elasticsearch.support;

import io.github.loooopin.elasticsearch.support.annotations.EsComparison;
import io.github.loooopin.elasticsearch.support.annotations.EsField;
import io.github.loooopin.elasticsearch.support.annotations.EsIndex;
import io.github.loooopin.elasticsearch.support.enums.ComparisonEnums;
import io.github.loooopin.elasticsearch.support.exceptions.EsContextInitException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:56
 * Description: 用于缓存实体中的注解和成员变量，省去每次都要反射获取
 */
public final class EsBeanContext {
    private static final String PROPERTIES_FOLDER = "spring/properties/";
    private static final String VALUE_IN_PROPERTIES_PREFIX = "${";
    private static final String HAS_NOT_INDEX_NAME = "NONE_INDEX";
    //索引名
    private static Map<Class, String> indexMap;
    //查询条件map
    private static Map<Class, Map<String, ComparisonEnums>> comparesMaps;
    //类中包含的所有字段
    private static Map<Class, Map<String, Field>> fieldsMaps;
    //成员变量与索引字段映射关系
    private static Map<Class, Map<String, String>> fieldMappingsMaps;
    //索引字段与成员变量映射关系
    private static Map<Class, Map<String, String>> mappingToJavaFieldsMaps;

    private static Map<String, Properties> propertiesMap;

    static {
        indexMap = new HashMap();
        comparesMaps = new HashMap();
        fieldsMaps = new HashMap();
        fieldMappingsMaps = new HashMap();
        mappingToJavaFieldsMaps = new HashMap();
        propertiesMap = new HashMap();
    }

    /**
     * 手动设置java实体字段与es列的映射关系
     * 手动设置比较符
     *
     * @param _class            java实体类
     * @param javaBeanFieldName java实体字段名
     * @param esFieldName       es索引的列名
     * @param comparisonEnums   比较符号
     */
    public static void addContext(Class _class, String javaBeanFieldName, String esFieldName, ComparisonEnums comparisonEnums) {
        if (_class == null || javaBeanFieldName == null) {
            return;
        }
        Field field = getSuperClassField(_class, javaBeanFieldName);
        //如果在_class及所有基类中找不到这个field就没有必要再继续执行了
        if (field == null) {
            return;
        }
        getFieldsMap(_class).put(javaBeanFieldName, field);
        //设置实体字段与es索引列的映射
        if (esFieldName != null) {
            getFieldMappingsMap(_class).put(javaBeanFieldName, esFieldName);
            getMappingToJavaFieldsMap(_class).put(esFieldName, javaBeanFieldName);
        }
        //设置比较符
        if (comparisonEnums != null) {
            getComparesMap(_class).put(javaBeanFieldName, comparisonEnums);
        }
    }

    private static Field getSuperClassField(Class _class, String javaBeanFieldName) {
        try {
            Field field = _class.getDeclaredField(javaBeanFieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            Class superclass = _class.getSuperclass();
            if (superclass == Object.class) {
                return null;
            }
            return getSuperClassField(superclass, javaBeanFieldName);
        }
    }

    public static String getIndex(Class _class) {
        if (!indexMap.containsKey(_class)) {
            indexMap.put(_class, assembleIndex(_class));
        }
        return indexMap.get(_class);
    }

    private static String assembleIndex(Class _class) {
        EsIndex esIndex = EsIndex.class.cast(_class.getDeclaredAnnotation(EsIndex.class));
        if (esIndex == null) {
            return null;
        }
        if (!esIndex.value().startsWith(VALUE_IN_PROPERTIES_PREFIX)) {
            return esIndex.value();
        }
        try {
            Properties properties = getProperties(esIndex.refProps());
            return properties.getProperty(esIndex.value().substring(2, esIndex.value().length() - 1), HAS_NOT_INDEX_NAME);
        } catch (IOException e) {
            throw new EsContextInitException(MessageFormat.format("读取配置文件【spring/properties/{0}】时出错", esIndex.refProps()), e);
        }
    }

    /**
     * 根据class获得查询条件
     * 如果没有就根据注解生成
     *
     * @param _class
     * @return
     */
    public static Map<String, ComparisonEnums> getComparesMap(Class _class) {
        if (!comparesMaps.containsKey(_class)) {
            comparesMaps.put(_class, assembleComparesMap(_class));
        }
        return comparesMaps.get(_class);
    }

    /**
     * 根据注解生成指定class的查询条件
     *
     * @param _class
     * @return
     */
    private static Map<String, ComparisonEnums> assembleComparesMap(Class _class) {
        Field[] fields = _class.getDeclaredFields();
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

    public static Map<String, Field> getFieldsMap(Class _class) {
        if (!fieldsMaps.containsKey(_class)) {
            fieldsMaps.put(_class, assembleFieldsMap(_class));
        }
        return fieldsMaps.get(_class);
    }

    private static Map<String, Field> assembleFieldsMap(Class _class) {
        Field[] fields = _class.getDeclaredFields();
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
     * @param _class
     * @return
     */
    public static Map<String, String> getFieldMappingsMap(Class _class) {
        if (!fieldMappingsMaps.containsKey(_class)) {
            fieldMappingsMaps.put(_class, assembleFieldMappingsMap(_class));
        }
        return fieldMappingsMaps.get(_class);
    }

    /**
     * 根据注解生成指定class中成员变量与表字段的对应关系
     *
     * @param _class
     * @return
     */
    private static Map<String, String> assembleFieldMappingsMap(Class _class) {
        Field[] fields = _class.getDeclaredFields();
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

    public static Map<String, String> getMappingToJavaFieldsMap(Class _class) {
        if (!mappingToJavaFieldsMaps.containsKey(_class)) {
            mappingToJavaFieldsMaps.put(_class, assembleMappingToJavaFieldsMap(_class));
        }
        return mappingToJavaFieldsMaps.get(_class);
    }

    private static Map<String, String> assembleMappingToJavaFieldsMap(Class _class) {
        Field[] fields = _class.getDeclaredFields();
        Map<String, String> fieldMappingsMap = new HashMap();
        for (Field field : fields) {
            EsField esField = field.getAnnotation(EsField.class);
            if (esField == null) {
                continue;
            }
            if (esField.onlyCompare()) {
                continue;
            }
            String value = esField.value();
            fieldMappingsMap.put(value, field.getName());
        }
        return fieldMappingsMap;
    }

    private static Properties getProperties(String propertiesName) throws IOException {
        if (propertiesMap.containsKey(propertiesName)) {
            return propertiesMap.get(propertiesName);
        }
        Properties prop = new Properties();
        String fileName = PROPERTIES_FOLDER + propertiesName;
        InputStream inputStream = EsBeanContext.class.getClassLoader().getResourceAsStream(fileName);
        prop.load(inputStream);
        propertiesMap.put(propertiesName, prop);
        return prop;
    }
}
