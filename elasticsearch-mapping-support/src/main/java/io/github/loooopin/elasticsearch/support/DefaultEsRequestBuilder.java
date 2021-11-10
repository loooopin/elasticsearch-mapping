package io.github.loooopin.elasticsearch.support;

import io.github.loooopin.elasticsearch.api.AbstractEsRequestBuilder;
import io.github.loooopin.elasticsearch.support.constants.EsCommonConstants;
import io.github.loooopin.elasticsearch.support.enums.AggregateEnums;
import io.github.loooopin.elasticsearch.support.enums.ComparisonEnums;
import io.github.loooopin.elasticsearch.support.exceptions.EsBuildRequestException;
import io.github.loooopin.elasticsearch.util.CollectionUtils;
import io.github.loooopin.elasticsearch.util.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.lang.reflect.Field;
import java.util.*;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:59
 * Description: 用于构建查询es的入参
 */
public final class DefaultEsRequestBuilder extends AbstractEsRequestBuilder<SearchRequest> {
    //分组条件
    private LinkedHashMap<String, TermsAggregationBuilder> groupBuilders;
    //聚合字段
    private LinkedHashMap<String, AggregationBuilder> aggregationBuilders;
    //排序，只在非聚合查询时，才set值
    private LinkedHashMap<String, SortOrder> orderByFields;

    //最后生成的聚合
    private AggregationBuilder aggregations;
    //查询条件
    private BoolQueryBuilder filter;

    //聚合查询时是否查询其它字段
    private TopHitsAggregationBuilder topHitsAggregationBuilder;
    //普通查询时，查询总数
    private AggregationBuilder totalElementAggregationBuilder;

    public DefaultEsRequestBuilder() {
        this.groupBuilders = new LinkedHashMap();
        this.aggregationBuilders = new LinkedHashMap();
        this.orderByFields = new LinkedHashMap();
    }

    /**
     * 设置查询条件
     *
     * @param request
     * @return
     */
    @Override
    public DefaultEsRequestBuilder setRequest(Object request) {
        this.request = request;

        return this;
    }

    /**
     * 加载类反射相关的数据
     *
     * @param requestClass
     * @param responseClass
     * @return
     */
    @Override
    public DefaultEsRequestBuilder loadContext(Class requestClass, Class responseClass) {
        String index = EsBeanContext.getIndex(requestClass);
        Map<String, Field> requestFieldsMap = EsBeanContext.getFieldsMap(requestClass);
        Map<String, String> requestFieldMappingsMap = EsBeanContext.getFieldMappingsMap(requestClass);
        Map<String, String> responseFieldMappingsMap = EsBeanContext.getFieldMappingsMap(responseClass);
        Map<String, ComparisonEnums> requestComparesMap = EsBeanContext.getComparesMap(requestClass);
        if (CollectionUtils.isEmpty(requestFieldMappingsMap)) {
            throw new EsBuildRequestException(requestClass.getName() + "未配置字段映射关系");
        }
        if (CollectionUtils.isEmpty(responseFieldMappingsMap)) {
            throw new EsBuildRequestException(requestClass.getName() + "未配置返回值字段映射关系");
        }
        if (CollectionUtils.isEmpty(requestComparesMap)) {
            throw new EsBuildRequestException(requestClass.getName() + "未配置字段比较符");
        }
        this.requestFieldsMap = requestFieldsMap;
        this.requestFieldMappingsMap = requestFieldMappingsMap;
        this.responseFieldMappingsMap = responseFieldMappingsMap;
        this.requestComparesMap = requestComparesMap;
        this.index = index;
        this.buildQuery();
        return this;
    }

    @Override
    public SearchRequest build() {
        if (this.index == null) {
            throw new EsBuildRequestException(this.request.getClass().getName() + "未配置索引名称");
        }
        SearchSourceBuilder sourceBuilder = buildSourceBuilder();
        SearchRequest searchRequest = new SearchRequest(this.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    @Override
    public SearchRequest build(int from, int size) {
        if (this.isAggregationQuery) {
            throw new EsBuildRequestException("聚合查询不支持分页");
        }
        if (this.index == null) {
            throw new EsBuildRequestException(this.request.getClass().getName() + "未配置索引名称");
        }
        if (this.totalElementAggregationBuilder == null) {
            this.totalElement(EsCommonConstants.DEFAULT_COUNT_FIELD);
        }
        SearchSourceBuilder sourceBuilder = buildSourceBuilder(from, size);
        SearchRequest searchRequest = new SearchRequest(this.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    /**
     * 分页构建searchSourceBuilder
     * 如果有sortValue且有orderByFields，则使用search_after
     * 如果没有，就使用from+size进行分页
     *
     * @param from
     * @param size
     * @return
     */
    private SearchSourceBuilder buildSourceBuilder(int from, int size) {
        SearchSourceBuilder sourceBuilder = buildSourceBuilder();
        sourceBuilder.size(size);

        //此处用来拼装searchAfter
        //排序查询，且有上一次分页的sortValues时，才searchAfter
        if (!this.orderByFields.isEmpty() && this.sortValues != null) {
            sourceBuilder.searchAfter(this.sortValues);
            return sourceBuilder;
        }

        sourceBuilder.from(from);
        return sourceBuilder;
    }

    /**
     * 不分页构建searchSourceBuilder
     *
     * @return
     */
    private SearchSourceBuilder buildSourceBuilder() {
        buildAggregation();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(this.getFilter());
        if (this.hasAggregations()) {
            sourceBuilder.aggregation(this.getAggregations());
        }
        //如果是聚合查询，就不需要query的命中结果
        if (this.isAggregationQuery) {
            sourceBuilder.size(0);
        } else {
            //非聚合查询时看有没有orderBy
            if (!this.orderByFields.isEmpty()) {
                this.orderByFields.forEach((k, v) -> {
                    sourceBuilder.sort(k, v);
                });
            }
        }
        return sourceBuilder;
    }

    /**
     * 设置聚合条件
     *
     * @param groupFields
     * @return
     */
    @Override
    public DefaultEsRequestBuilder groupBy(String... groupFields) {
        return this.groupBy(MAX_AGGREGATION_SIZE, groupFields);
    }

    @Override
    public DefaultEsRequestBuilder groupBy(Integer size, String... groupFields) {
        for (String groupField : groupFields) {
            String esFieldName = this.requestFieldMappingsMap.get(groupField);
            if (esFieldName == null) {
                continue;
            }
            this.groupBuilders.put(groupField, AggregationBuilders.terms(groupField).field(esFieldName).size(size));
        }
        if (!this.groupBuilders.isEmpty()) {
            this.isAggregationQuery = true;
        }
        return this;
    }

    /**
     * 根据哪些字段排序
     * 只在非聚合查询时才set值
     *
     * @param orderByJavaBeanFieldName
     * @param sortOrder
     * @return
     */
    @Override
    public DefaultEsRequestBuilder orderBy(String orderByJavaBeanFieldName, SortOrder sortOrder) {
        if (this.isAggregationQuery) {
            return this;
        }
        String orderByEsFieldName = this.requestFieldMappingsMap.get(orderByJavaBeanFieldName);
        if (StringUtils.isEmpty(orderByEsFieldName)) {
            return this;
        }
        this.orderByFields.put(orderByEsFieldName, sortOrder);
        return this;
    }

    @Override
    public DefaultEsRequestBuilder orderBy(LinkedHashMap<String, SortOrder> map) {
        if (this.isAggregationQuery) {
            return this;
        }
        map.forEach((k, v) -> {
            String orderByEsFieldName = this.requestFieldMappingsMap.get(k);
            if (StringUtils.isEmpty(orderByEsFieldName)) {
                return;
            }
            this.orderByFields.put(orderByEsFieldName, v);
        });
        return this;
    }


    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量）
     * @param aggregateEnum
     * @return
     */
    @Override
    public DefaultEsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum) {
        this.aggregation(aggregateFieldName, aggregateEnum, aggregateFieldName);
        return this;
    }

    /**
     * 设置聚合字段
     *
     * @param aggregateFieldName 需要聚合的字段（javaBean中的成员变量）
     * @param aggregateEnum
     * @param targetFieldName    聚合后的数据，应放入的字段
     * @return
     */
    @Override
    public DefaultEsRequestBuilder aggregation(String aggregateFieldName, AggregateEnums aggregateEnum, String targetFieldName) {
        String esFieldName = this.responseFieldMappingsMap.get(aggregateFieldName);
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
            case COUNT:
                this.aggregationBuilders.put(targetFieldName, AggregationBuilders.count(targetFieldName).field(esFieldName));
                break;
            case COUNT_DISTINCT:
                this.aggregationBuilders.put(targetFieldName, AggregationBuilders.cardinality(targetFieldName).field(esFieldName));
                break;
            default:
                break;
        }
        return this;
    }

    /**
     * 手动设置查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    @Override
    public DefaultEsRequestBuilder and(QueryBuilder queryBuilder) {
        this.filter.filter(queryBuilder);
        return this;
    }

    /**
     * 手动设置不符合的查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    @Override
    public DefaultEsRequestBuilder not(QueryBuilder queryBuilder) {
        this.filter.mustNot(queryBuilder);
        return this;
    }

    /**
     * 手动设置or的查询条件
     * 必须在filter已经生成后，才能手动设置
     *
     * @param queryBuilder
     * @return
     */
    @Override
    public DefaultEsRequestBuilder or(QueryBuilder queryBuilder) {
        this.filter.should(queryBuilder);
        return this;
    }


    /**
     * tophit
     * 根据排序条件筛选出最符合预期的tophit
     *
     * @return
     */
    @Override
    public DefaultEsRequestBuilder searchOtherFields() {
        if (!this.isAggregationQuery) {
            return this;
        }
        this.topHitsAggregationBuilder = AggregationBuilders.topHits(OTHER_FIELDS).size(1);
        return this;
    }

    @Override
    public DefaultEsRequestBuilder searchOtherFields(String orderByJavaBeanFieldName, SortOrder sortOrder) {
        if (!this.isAggregationQuery) {
            return this;
        }
        this.topHitsAggregationBuilder = AggregationBuilders.topHits(OTHER_FIELDS).size(1);
        String orderByEsFieldName = this.requestFieldMappingsMap.get(orderByJavaBeanFieldName);
        if (StringUtils.isEmpty(orderByEsFieldName)) {
            return this;
        }
        this.topHitsAggregationBuilder.sort(orderByEsFieldName, sortOrder);
        return this;
    }

    @Override
    public DefaultEsRequestBuilder searchOtherFields(Map<String, SortOrder> map) {
        if (!this.isAggregationQuery) {
            return this;
        }
        this.topHitsAggregationBuilder = AggregationBuilders.topHits(OTHER_FIELDS).size(1);
        map.forEach((k, v) -> {
            String orderByEsFieldName = this.requestFieldMappingsMap.get(k);
            if (StringUtils.isEmpty(orderByEsFieldName)) {
                return;
            }
            this.topHitsAggregationBuilder.sort(orderByEsFieldName, v);
        });
        return this;
    }

    @Override
    public DefaultEsRequestBuilder totalElement(String esFieldName) {
        this.totalElementAggregationBuilder = AggregationBuilders.count(EsCommonConstants.RESPONSE_TOTAL_ELEMENT_KEY).field(esFieldName);
        return this;
    }

    /**
     * 生成聚合条件
     */
    @Override
    protected void buildAggregation() {
        if (this.groupBuilders.isEmpty()) {
            if (this.totalElementAggregationBuilder == null) {
                return;
            }
            this.aggregations = this.totalElementAggregationBuilder;
            return;
        }
        ListIterator<Map.Entry<String, TermsAggregationBuilder>> listIterator = new ArrayList(this.groupBuilders.entrySet()).listIterator(this.groupBuilders.size());
        TermsAggregationBuilder pointer = listIterator.previous().getValue();
        if (!this.aggregationBuilders.isEmpty()) {
            for (AggregationBuilder value : this.aggregationBuilders.values()) {
                pointer.subAggregation(value);
            }
            if (this.topHitsAggregationBuilder != null) {
                pointer.subAggregation(this.topHitsAggregationBuilder);
            }
        }
        while (listIterator.hasPrevious()) {
            pointer = listIterator.previous().getValue().subAggregation(pointer);
        }
        this.aggregations = pointer;
    }

    /**
     * 生成查询条件
     */
    @Override
    protected void buildQuery() {
        if (this.filter != null) {
            return;
        }
        this.filter = QueryBuilders.boolQuery();
        if (this.request == null) {
            return;
        }
        Set<String> keys = this.requestFieldsMap.keySet();
        for (String key : keys) {
            Field field = this.requestFieldsMap.get(key);
            Object o = null;
            try {
                o = field.get(this.request);
            } catch (IllegalAccessException e) {
                throw new EsBuildRequestException("获取查询参数值时发生异常", e);
            }
            if (o == null) {
                continue;
            }
            if (o instanceof Date) {
                //es时区问题，+8小时
                Calendar calendarInstance = Calendar.getInstance();
                calendarInstance.setTime((Date) o);
                calendarInstance.add(Calendar.HOUR, 8);
                //把日期类型转为时间戳
                o = calendarInstance.getTime().getTime();
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
        if (!this.requestComparesMap.containsKey(beanFieldName)) {
            return;
        }
        if (!this.requestFieldMappingsMap.containsKey(beanFieldName)) {
            return;
        }
        ComparisonEnums comparisonEnum = this.requestComparesMap.get(beanFieldName);
        String esFieldName = this.requestFieldMappingsMap.get(beanFieldName);
        switch (comparisonEnum) {
            case EQ:
                this.and(QueryBuilders.termQuery(esFieldName, value));
                break;
            case NEQ:
                this.not(QueryBuilders.termQuery(esFieldName, value));
                break;
            case LT:
                this.and(QueryBuilders.rangeQuery(esFieldName).lt(value));
                break;
            case LTE:
                this.and(QueryBuilders.rangeQuery(esFieldName).lte(value));
                break;
            case GT:
                this.and(QueryBuilders.rangeQuery(esFieldName).gt(value));
                break;
            case GTE:
                this.and(QueryBuilders.rangeQuery(esFieldName).gte(value));
                break;
            case LIKE:
                this.and(QueryBuilders.matchQuery(esFieldName, value));
                break;
            case IN:
                if (!(value instanceof List)) {
                    break;
                }
                if (CollectionUtils.isEmpty((List) value)) {
                    break;
                }
                this.and(QueryBuilders.termsQuery(esFieldName, (List) value));
                break;
            default:
                break;
        }

    }

    @Override
    public LinkedList<String> getGroupFieldChain() {
        return new LinkedList(this.groupBuilders.keySet());
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

    public AggregationBuilder getAggregations() {
        return aggregations;
    }

    public BoolQueryBuilder getFilter() {
        return filter;
    }

    public DefaultEsRequestBuilder setFilter(BoolQueryBuilder filter) {
        this.filter = filter;
        return this;
    }

    public void setSortValues(Object[] sortValues) {
        this.sortValues = sortValues;
    }
}
