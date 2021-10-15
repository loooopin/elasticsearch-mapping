package io.github.loooopin.support;

import io.github.loooopin.util.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.tophits.ParsedTopHits;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: loooopin
 * Date: 2021/10/14
 * Time: 18:01
 * Description: 解析查询结果，主要是用来递归解析聚合查询的结果
 */
public final class EsResponseResolver {

    private Class _class;
    //groupBy的层级顺序，用于逐层取bucket
    private LinkedList<String> groupFieldChain;
    //是否聚合查询
    private boolean isAggregationQuery;

    private Map<String, String> mappingToJavaFieldsMap;

    private EsResponseResolver(Class _class, LinkedList<String> groupFieldChain, boolean isAggregationQuery, Map<String, String> mappingToJavaFieldsMap) {
        this._class = _class;
        this.groupFieldChain = groupFieldChain;
        this.isAggregationQuery = isAggregationQuery;
        this.mappingToJavaFieldsMap = mappingToJavaFieldsMap;
    }

    /**
     * @param _class             查询结果映射为javaBean
     * @param groupFieldChain    查询时groupBy的层级
     * @param isAggregationQuery 是否是聚合查询结果
     * @return
     */
    public static EsResponseResolver resolver(Class _class, LinkedList<String> groupFieldChain, boolean isAggregationQuery) {
        Map<String, String> mappingToJavaFieldsMap = EsBeanContext.getMappingToJavaFieldsMap(_class);
        if (CollectionUtils.isEmpty(mappingToJavaFieldsMap)) {
            throw new IllegalArgumentException(_class.getName() + "未配置字段映射关系");
        }
        return new EsResponseResolver(_class, groupFieldChain, isAggregationQuery, mappingToJavaFieldsMap);
    }

    /**
     * 开始解析SearchResponse
     *
     * @param searchResponse
     * @return
     */
    public LinkedList resolve(SearchResponse searchResponse) throws IOException {
        if (this.isAggregationQuery) {
            return resolveAggregationResponse(searchResponse);
        }
        return resolveNormalResponse(searchResponse);
    }

    /**
     * 解析普通查询结果
     *
     * @param searchResponse
     * @return
     */
    private LinkedList<Map<String, Object>> resolveNormalResponse(SearchResponse searchResponse) {
        LinkedList<Map<String, Object>> responses = new LinkedList<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            responses.add(sourceAsMap);
        }
        return responses;
    }

    /**
     * 解析聚合查询结果
     *
     * @param searchResponse
     * @return
     */
    private LinkedList<Map<String, Object>> resolveAggregationResponse(SearchResponse searchResponse) throws IOException {
        Aggregations aggregations = searchResponse.getAggregations();
        LinkedList<Map<String, Object>> responses = new LinkedList<>();
        fillValue(aggregations, 0, responses, new HashMap());
        return responses;
    }

    /**
     * 递归生成JSON
     *
     * @param aggregations
     * @param index        本次递归在group链中的位置
     * @param responses    最终结果
     * @param template     上一次递归生成的map
     */
    private void fillValue(Aggregations aggregations, int index, List<Map<String, Object>> responses, HashMap<String, Object> template) throws IOException {
        if (index < this.groupFieldChain.size()) {
            //获得第index层的groupBy的名称
            String name = this.groupFieldChain.get(index);
            index++;
            Map<String, Aggregation> stringAggregationMap = aggregations.asMap();
            Aggregation aggregation = stringAggregationMap.get(name);
            List<? extends Terms.Bucket> buckets = ((ParsedStringTerms) aggregation).getBuckets();
            for (int i = 0; i < buckets.size(); i++) {
                Terms.Bucket bucket = buckets.get(i);

                HashMap<String, Object> cloneTemplate;
                //如果没到最后一个就深克隆，到最后一个就没必要克隆了
                if (i != buckets.size() - 1) {
                    cloneTemplate = deepClone(template);
                } else {
                    cloneTemplate = template;
                }

                String value = bucket.getKeyAsString();
                cloneTemplate.put(name, value);

                Aggregations childAggregations = bucket.getAggregations();
                fillValue(childAggregations, index, responses, cloneTemplate);
            }
            return;
        }
        List<Aggregation> childAggregation = aggregations.asList();
        SearchHit[] hits = null;
        for (Aggregation aggregation : childAggregation) {
            String name = aggregation.getName();
            //如果是ParsedTopHits的话，就证明是聚合查询且查询了非聚合字段
            if (aggregation instanceof ParsedTopHits) {
                ParsedTopHits aggregation1 = (ParsedTopHits) aggregation;
                hits = aggregation1.getHits().getHits();
                continue;
            }
            template.put(name, ((NumericMetricsAggregation.SingleValue) aggregation).value());
        }

        if (hits != null) {
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.forEach((k, v) -> {
                    if(!this.mappingToJavaFieldsMap.containsKey(k)){
                        return;
                    }
                    String javaBeanFieldName = this.mappingToJavaFieldsMap.get(k);
                    //过滤掉已经set过值的字段
                    if (template.containsKey(javaBeanFieldName)) {
                        return;
                    }
                    template.put(javaBeanFieldName, v);
                });
            }
        }
        responses.add(template);
    }

    //TODO 深克隆优化
    private HashMap<String, Object> deepClone(HashMap<String, Object> template) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try {
            objectOutputStream.writeObject(template);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                objectOutputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                //不需要再下一步处理，此处不抛出异常
                e.printStackTrace();
            }
        }
        HashMap<String, Object> result = null;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        try {
            result = (HashMap) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            //不需要再下一步处理，此处不抛出异常
            e.printStackTrace();
        }
        try {
            objectInputStream.close();
            byteArrayInputStream.close();
            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            //不需要再下一步处理，此处不抛出异常
            e.printStackTrace();
        }
        return result;
    }
}
