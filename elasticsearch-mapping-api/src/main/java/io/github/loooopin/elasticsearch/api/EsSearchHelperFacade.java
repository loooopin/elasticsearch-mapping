package io.github.loooopin.elasticsearch.api;

import io.github.loooopin.elasticsearch.entity.EsResponse;

import java.io.Closeable;
import java.io.IOException;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:20
 * Description: 查询入口类
 */

public abstract class EsSearchHelperFacade<RestClient extends Closeable, RequestBuilder extends AbstractEsRequestBuilder, ResponseResolver extends AbstractEsResponseResolver> {
    protected RestClient restClient;

    public EsSearchHelperFacade setRestClient(RestClient restClient) {
        this.restClient = restClient;
        return this;
    }

    /**
     * 不分页且设置返回值类
     *
     * @param requestBuilder
     * @param responseClass
     * @return
     * @throws IOException
     */
    public abstract EsResponse search(RequestBuilder requestBuilder, Class responseClass) throws IOException;


    /**
     * 分页且设置返回值类
     *
     * @param requestBuilder
     * @param responseClass
     * @return
     * @throws IOException
     */
    public abstract EsResponse search(RequestBuilder requestBuilder, Class responseClass, int from, int size) throws IOException;

    /**
     * 不分页且使用查询参数的类作为返回值
     *
     * @param requestBuilder
     * @return
     * @throws IOException
     */
    public abstract EsResponse search(RequestBuilder requestBuilder) throws IOException;


    /**
     * 分页且使用查询参数的类作为返回值
     *
     * @param requestBuilder
     * @return
     * @throws IOException
     */
    public abstract EsResponse search(RequestBuilder requestBuilder, int from, int size) throws IOException;

    /**
     * 生成builder
     * 设置查询条件
     *
     * @param request
     * @return
     */
    public abstract RequestBuilder requestBuilder(Object request) throws IllegalAccessException;

    /**
     * 生成builder
     * 不设置查询条件，仅设置类反射相关的数据
     *
     * @param _class
     * @return
     */
    public abstract RequestBuilder requestBuilder(Class _class) throws IllegalAccessException;

    /**
     * 如果是聚合查询，则根据requestBuilder中的分组字段进行逐层解析
     *
     * @param requestBuilder
     * @param responseClass
     * @return
     */
    public abstract ResponseResolver responseResolver(RequestBuilder requestBuilder, Class responseClass);

}
