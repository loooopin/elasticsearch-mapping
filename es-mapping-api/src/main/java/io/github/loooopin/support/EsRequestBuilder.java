package io.github.loooopin.support;

import io.github.loooopin.support.enums.AggregateEnums;
import io.github.loooopin.support.enums.ComparisonEnums;
import io.github.loooopin.util.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import java.lang.reflect.Field;
import java.util.*;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:59
 * Description: 用于构建查询es的入参
 */
public final class EsRequestBuilder {
    //聚合查询时，如果要命中非聚合的字段，则别名固定使用otherFields
    private static final String OTHER_FIELDS="otherFields";
    //查询参数
    private Object request;
    //索引
    private String index;
    //分组条件
    private LinkedHashMap<String, TermsAggregationBuilder> groupBuilders;
    //聚合字段
    private LinkedHashMap<String, AggregationBuilder> aggregationBuilders;

    //最后生成的聚合
    private TermsAggregationBuilder aggregations;
    //最后生成的查询条件
    private BoolQueryBuilder filter;

    private Map<String, Field> fieldsMap;
    private Map<String, String> fieldMappingsMap;
    private Map<String, ComparisonEnums> comparesMap;

    //是否是聚合查询
    private boolean isAggregationQuery;
    //聚合查询时是否查询其它字段
    private boolean searchOtherField;

    private EsRequestBuilder() {
        this.groupBuilders = new LinkedHashMap();
        this.aggregationBuilders = new LinkedHashMap();
    }

    /**
     * 生成builder
     * 设置查询条件
     *
     * @param request
     * @return
     */
    public static EsRequestBuilder builder(Object request) {
        return new EsRequestBuilder().setRequest(request);
    }

    /**
     * 生成builder
     * 不设置查询条件，仅设置类反射相关的数据
     *
     * @param _class
     * @return
     */
    public static EsRequestBuilder builder(Class _class) {
        return new EsRequestBuilder().loadContext(_class);
    }

    /**
     * 设置查询条件
     *
     * @param request
     * @return
     */
    public EsRequestBuilder setRequest(Object request) {
        Class _class = request.getClass();
        this.request = request;
        return this.loadContext(_class);
    }

    /**
     * 加载类反射相关的数据
     *
     * @param _class
     * @return
     */
    public EsRequestBuilder loadContext(Class _class) {
        String index = EsBeanContext.getIndex(_class);
        Map<String, Field> fieldsMap = EsBeanContext.getFieldsMap(_class);
        Map<String, String> fieldMappingsMap = EsBeanContext.getFieldMappingsMap(_class);
        Map<String, ComparisonEnums> comparesMap = EsBeanContext.getComparesMap(_class);
//        if (index == null) {
//            throw new IllegalArgumentException(_class.getName() + "未配置索引名称");
//        }
        if (CollectionUtils.isEmpty(fieldMappingsMap)) {
            throw new IllegalArgumentException(_class.getName() + "未配置字段映射关系");
        }
        if (CollectionUtils.isEmpty(comparesMap)) {
            throw new IllegalArgumentException(_class.getName() + "未配置字段比较符");
        }
        this.fieldsMap = fieldsMap;
        this.fieldMappingsMap = fieldMappingsMap;
        this.comparesMap = comparesMap;
        this.index = index;
        return this;
    }

    public void build() throws IllegalAccessException {
        if (this.index == null) {
            throw new IllegalArgumentException(this.request.getClass().getName() + "未配置索引名称");
        }
        buildQuery();
        buildAggregation();
    }

    /**
     * 设置聚合条件
     *
     * @param groupFields
     * @return
     */
    public EsRequestBuilder groupBy(String... groupFields) {
        for (String groupField : groupFields) {
            String esFieldName = this.fieldMappingsMap.get(groupField);
            if (esFieldName == null) {
                continue;
            }
            this.groupBuilders.put(groupField, AggregationBuilders.terms(groupField).field(esFieldName));
        }
        return this;
    }

    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量名）
     * @param aggregateEnum
     * @return
     */
    public EsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum) {
        this.aggregation(aggregateFieldName, aggregateEnum, aggregateFieldName);
        return this;
    }

    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量名）
     * @param aggregateEnum
     * @param targetFieldName    聚合后的数据的别名，用于与结果类做映射
     * @return
     */
    public EsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum, String targetFieldName) {
        String esFieldName = this.fieldMappingsMap.get(aggregateFieldName);
        if (esFieldName == null) {
            return this;
        }
        switch (aggregateEnum) {
            case SUM:
                this.aggregationBuilders.put(targetFieldName, AggregationBuilders.sum(targetFieldName).field(esFieldName));
                break;
            case MAX:
                this.aggregationBuilders.put(targetFieldName, AggregationBuilders.max(targetFieldName).field(esFieldName));
                break;
            case MIN:
                this.aggregationBuilders.put(targetFieldName, AggregationBuilders.min(targetFieldName).field(esFieldName));
                break;
            default:
                break;
        }
        return this;
    }

    /**
     * 生成聚合条件
     */
    private void buildAggregation() {
        if (this.groupBuilders.isEmpty()) {
            return;
        }
        this.isAggregationQuery = true;
        ListIterator<Map.Entry<String, TermsAggregationBuilder>> listIterator = new ArrayList(this.groupBuilders.entrySet()).listIterator(this.groupBuilders.size());
        TermsAggregationBuilder pointer = listIterator.previous().getValue();
        if (!this.aggregationBuilders.isEmpty()) {
            for (AggregationBuilder value : this.aggregationBuilders.values()) {
                pointer.subAggregation(value);
            }
            if (searchOtherField) {
                //TODO sort,用于筛选出第一条命中的数据
                pointer.subAggregation(AggregationBuilders.topHits(OTHER_FIELDS).size(1));
            }
        }
        while (listIterator.hasPrevious()) {
            pointer = listIterator.previous().getValue().subAggregation(pointer);
        }
        this.aggregations = pointer;
    }

    /**
     * 生成查询条件
     *
     * @throws IllegalAccessException
     */
    private void buildQuery() throws IllegalAccessException {
        if (this.filter != null) {
            return;
        }
        this.filter = QueryBuilders.boolQuery();
        Set<String> keys = this.fieldsMap.keySet();
        for (String key : keys) {
            Field field = this.fieldsMap.get(key);
            Object o = field.get(this.request);
            if (o == null) {
                continue;
            }
            addSearchCondition(key, o);
        }
    }

    /**
     * 添加查询条件
     *
     * @param beanFieldName
     * @param value
     */
    private void addSearchCondition(String beanFieldName, Object value) {
        if (!this.comparesMap.containsKey(beanFieldName)) {
            return;
        }
        if (!this.fieldMappingsMap.containsKey(beanFieldName)) {
            return;
        }
        ComparisonEnums comparisonEnum = this.comparesMap.get(beanFieldName);
        String esFieldName = this.fieldMappingsMap.get(beanFieldName);
        switch (comparisonEnum) {
            case EQ:
                this.filter.must(QueryBuilders.matchQuery(esFieldName, value));
                break;
            case LT:
                this.filter.must(QueryBuilders.rangeQuery(esFieldName).lt(value));
                break;
            case LTE:
                this.filter.must(QueryBuilders.rangeQuery(esFieldName).lte(value));
                break;
            case GT:
                this.filter.must(QueryBuilders.rangeQuery(esFieldName).gt(value));
                break;
            case GTE:
                this.filter.must(QueryBuilders.rangeQuery(esFieldName).gte(value));
                break;
            case IN:
                this.filter.must(QueryBuilders.termsQuery(esFieldName, (List) value));
                break;
            default:
                break;
        }

    }

    public String getIndex() {
        return index;
    }

    public LinkedHashMap<String, TermsAggregationBuilder> getGroupBuilders() {
        return groupBuilders;
    }

    public LinkedHashMap<String, AggregationBuilder> getAggregationBuilders() {
        return aggregationBuilders;
    }

    public boolean hasAggregations() {
        return aggregations != null;
    }

    public TermsAggregationBuilder getAggregations() {
        return aggregations;
    }

    public BoolQueryBuilder getFilter() {
        return filter;
    }

    public boolean isAggregationQuery() {
        return isAggregationQuery;
    }

    public boolean isSearchOtherField() {
        return searchOtherField;
    }

    //单实体可能对应多个索引，此时类上并不会有EsIndex注解，故每次查询时手动settter
    public EsRequestBuilder setIndex(String index) {
        this.index = index;
        return this;
    }

    public EsRequestBuilder setFilter(BoolQueryBuilder filter) {
        this.filter = filter;
        return this;
    }

    public EsRequestBuilder setSearchOtherField(boolean searchOtherField) {
        this.searchOtherField = searchOtherField;
        return this;
    }
}
