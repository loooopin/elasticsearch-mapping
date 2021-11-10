package io.github.loooopin.elasticsearch.support.enums;


import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;

/**
 * User: loooopin
 * Date: 2021/10/13
 * Time: 18:28
 * Description:
 */
public enum AggregateEnums {
    SUM(SumAggregationBuilder.NAME),
    MAX(MaxAggregationBuilder.NAME),
    MIN(MinAggregationBuilder.NAME),
    COUNT(ValueCountAggregationBuilder.NAME),
    COUNT_DISTINCT(CardinalityAggregationBuilder.NAME),
    ;
    private String type;

    public String getType() {
        return type;
    }

    public AggregateEnums typeOf(String type) {
        AggregateEnums[] values = AggregateEnums.values();
        for (AggregateEnums aggregateEnum : values) {
            if (aggregateEnum.getType().equals(type)) {
                return aggregateEnum;
            }
        }
        return null;
    }

    AggregateEnums(String type) {
        this.type = type;
    }
}
