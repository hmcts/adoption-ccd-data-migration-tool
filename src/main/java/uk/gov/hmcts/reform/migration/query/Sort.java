package uk.gov.hmcts.reform.migration.query;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Builder
public class Sort implements EsClause<List<Object>> {
    private final List<EsClause> clauses;

    @Override
    public List<Object> toMap() {
        return this.clauses.stream().map(EsClause::toMap).collect(Collectors.toList());
    }
}
