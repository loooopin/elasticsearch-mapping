package io.github.loooopin.elasticsearch.api;

import io.github.loooopin.elasticsearch.support.enums.AggregateEnums;
import io.github.loooopin.elasticsearch.support.enums.ComparisonEnums;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: loooopin
 * Date: 2021/10/29
 * Time: 19:03
 * Description: 构建es查询入参，抽象类
 */
public abstract class AbstractEsRequestBuilder<Request> {
    //聚合查询时，如果要命中非聚合的字段，则别名固定使用otherFields
    protected static final String OTHER_FIELDS = "otherFields";
    protected static final Integer MAX_AGGREGATION_SIZE = 10000;
    //入参
    protected Object request;

    //索引
    protected String index;

    //是否是聚合查询
    protected boolean isAggregationQuery;
    protected Map<String, Field> requestFieldsMap;
    protected Map<String, String> requestFieldMappingsMap;
    protected Map<String, ComparisonEnums> requestComparesMap;

    protected Map<String, String> responseFieldMappingsMap;

    //普通查询时使用searchAfter，该值为上一次分页查询的最后一位的sortValue
    protected Object[] sortValues;

    /**
     * 设置查询条件
     *
     * @param request
     * @return
     */
    public abstract AbstractEsRequestBuilder setRequest(Object request);

    /**
     * 加载类反射相关的数据
     *
     * @param requestClass
     * @param responseClass
     * @return
     */
    public abstract AbstractEsRequestBuilder loadContext(Class requestClass, Class responseClass);

    /**
     * 设置聚合条件
     *
     * @param groupFields
     * @return
     */
    public abstract AbstractEsRequestBuilder groupBy(String... groupFields);

    public abstract AbstractEsRequestBuilder groupBy(Integer size, String... groupFields);

    public abstract AbstractEsRequestBuilder orderBy(String orderByJavaBeanFieldName, SortOrder sortOrder);

    public abstract AbstractEsRequestBuilder orderBy(LinkedHashMap<String, SortOrder> map);

    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量）
     * @param aggregateEnum
     * @return
     */
    public abstract AbstractEsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum);

    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量）
     * @param aggregateEnum
     * @param targetFieldName    聚合后的数据，应放入的字段
     * @return
     */
    public abstract AbstractEsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum, String targetFieldName);


    /**
     * 手动设置查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    public abstract AbstractEsRequestBuilder and(QueryBuilder queryBuilder);

    /**
     * 手动设置不符合的查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    public abstract AbstractEsRequestBuilder not(QueryBuilder queryBuilder);

    /**
     * 手动设置or的查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    public abstract AbstractEsRequestBuilder or(QueryBuilder queryBuilder);


    /**
     * tophit
     * 根据排序条件筛选出最符合预期的tophit
     *
     * @return
     */
    public abstract AbstractEsRequestBuilder searchOtherFields();

    public abstract AbstractEsRequestBuilder searchOtherFields(String orderByJavaBeanFieldName, SortOrder sortOrder);

    public abstract AbstractEsRequestBuilder searchOtherFields(Map<String, SortOrder> map);

    /**
     * 根据指定字段来计算value_count
     *
     * @param esFieldName
     * @return
     */
    public abstract AbstractEsRequestBuilder totalElement(String esFieldName);

    /**
     * 生成聚合条件
     */
    protected abstract void buildAggregation();

    /**
     * 生成查询条件
     */
    protected abstract void buildQuery();

    /**
     * 构建查询对象
     *
     * @return
     */
    public abstract Request build();

    public abstract Request build(int from, int size);

    public boolean isAggregationQuery() {
        return this.isAggregationQuery;
    }

    /**
     * 获得groupBy的层级顺序，用于逐层解析聚合结果
     *
     * @return
     */
    public abstract LinkedList<String> getGroupFieldChain();

    public String getIndex() {
        return this.index;
    }

    public AbstractEsRequestBuilder setIndex(String index) {
        this.index = index;
        return this;
    }

    public Class getRequestClass() {
        return this.request.getClass();
    }
}
