package uk.gov.hmcts.reform.migration.query;

public interface EsClause<T> {
    T toMap();
}
