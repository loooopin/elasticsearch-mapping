package io.github.loooopin.elasticsearch.api;

import io.github.loooopin.elasticsearch.entity.EsResponse;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public abstract class AbstractEsResponseResolver<Response> {

    protected Class _class;
    //groupBy的层级顺序，用于逐层取bucket
    protected LinkedList<String> groupFieldChain;
    //是否聚合查询
    protected boolean isAggregationQuery;

    protected Map<String, String> mappingToJavaFieldsMap;

    /**
     * 加载类反射相关的数据
     *
     * @return
     */
    public abstract AbstractEsResponseResolver loadContext();

    /**
     * 开始解析SearchResponse
     *
     * @param response
     * @return
     */
    public abstract EsResponse resolve(Response response) throws IOException;

    /**
     * 解析普通查询结果
     *
     * @param response
     * @return
     */
    protected abstract EsResponse resolveNormalResponse(Response response);

    /**
     * 解析聚合查询结果
     *
     * @param response
     * @return
     */
    protected abstract EsResponse resolveAggregationResponse(Response response) throws IOException;

    /**
     * 总数
     *
     * @param response
     * @return
     */
    protected abstract int getTotalElement(Response response);

}
