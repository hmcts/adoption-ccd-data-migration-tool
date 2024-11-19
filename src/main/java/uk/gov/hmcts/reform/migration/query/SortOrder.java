package uk.gov.hmcts.reform.migration.query;

import lombok.Getter;

public enum SortOrder {
    ASC("asc"),
    DESC("desc");

    @Getter
    private final String order;

    SortOrder(String order) {
        this.order = order;
    }
}
