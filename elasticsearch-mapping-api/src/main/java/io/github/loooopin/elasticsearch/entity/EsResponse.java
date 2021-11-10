package io.github.loooopin.elasticsearch.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: loooopin
 * Date: 2021/10/20
 * Time: 20:58
 * Description: 查询结果
 */
public class EsResponse {
    private List content;
    //使用search_after进行分页时，本次分页查询的最后一个sortValues
    private Object[] lastHitSortValues;
    //分页查询时，总数
    private int totalElement;

    public List getContent() {
        return content;
    }

    public void setContent(List content) {
        this.content = content;
    }

    public Object[] getLastHitSortValues() {
        return lastHitSortValues;
    }

    public void setLastHitSortValues(Object[] lastHitSortValues) {
        this.lastHitSortValues = lastHitSortValues;
    }

    public int getTotalElement() {
        return totalElement;
    }

    public void setTotalElement(int totalElement) {
        this.totalElement = totalElement;
    }
}
