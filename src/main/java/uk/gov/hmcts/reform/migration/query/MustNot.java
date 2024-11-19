package uk.gov.hmcts.reform.migration.query;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Builder
public class MustNot implements EsClause {
    private final List<EsClause> clauses;

    @Override
    public Map<String, Object> toMap() {
        return Map.of("must_not", this.clauses.stream().map(EsClause::toMap).collect(Collectors.toList()));
    }

    public static MustNot of(List<EsClause> clauses) {
        return MustNot.builder()
            .clauses(clauses)
            .build();
    }

    public static MustNot of(EsClause clause) {
        return of(List.of(clause));
    }
}
