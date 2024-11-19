package uk.gov.hmcts.reform.migration.query;

import lombok.EqualsAndHashCode;

import java.util.Map;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode
public class ExistsQuery implements EsQuery {
    private final String field;

    public ExistsQuery(String field) {
        requireNonNull(field);
        this.field = field;
    }

    public static ExistsQuery of(String field) {
        return new ExistsQuery(field);
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of("exists", Map.of("field", field));
    }
}
