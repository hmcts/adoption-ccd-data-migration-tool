package uk.gov.hmcts.reform.migration.query;

import lombok.Builder;

@Builder
public class ElasticSearchQuery {

    private static final String START_QUERY = "{\n"
        + "  \"query\": {\n"
        + "    \"bool\": {\n"
        + "      \"filter\": {\n"
        + "        \"exists\": {\n"
        + "          \"field\": \"data.court\"\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  },\n"
        + "  \"size\": %s,\n"
        + "  \"_source\": [\n"
        + "    \"reference\",\n"
        + "    \"jurisdiction\"\n"
        + "  ],\n"
        + "  \"sort\": [\n"
        + "    {\n"
        + "      \"reference.keyword\": {\n"
        + "        \"order\": \"asc\"\n"
        + "      }\n"
        + "    }\n"
        + "  ]";

    private static final String END_QUERY = "\n}";

    private static final String SEARCH_AFTER = "\"search_after\": [%s]";

    private String searchAfterValue;
    private int size;
    private boolean initialSearch;

    public String getQuery() {
        if (initialSearch) {
            return getInitialQuery();
        } else {
            return getSubsequentQuery();
        }
    }

    private String getInitialQuery() {
        return String.join("",
            String.format(START_QUERY, size),
            END_QUERY);
    }

    private String getSubsequentQuery() {
        return String.join("",
            String.format(START_QUERY, size),
            ",",
            String.format(SEARCH_AFTER, searchAfterValue),
            END_QUERY);
    }
}
