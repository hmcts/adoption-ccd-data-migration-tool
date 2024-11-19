package uk.gov.hmcts.reform.migration.query;

import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public class Filter implements EsClause<Map<String, Object>> {
    private final List<EsClause> clauses;
    private TermQuery termQuery;
    private TermsQuery termsQuery;
    private RangeQuery rangeQuery;

    @Override
    public Map<String, Object> toMap() {
        return Map.of("filter", clauses.stream().map(EsClause::toMap).collect(Collectors.toList()));
    }
}
