package uk.gov.hmcts.reform.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ElasticSearchQueryTest {

    private static final int QUERY_SIZE = 100;

    @Test
    void shouldReturnQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(true)
            .size(QUERY_SIZE)
            .build();
        String query = elasticSearchQuery.getQuery();

        assertThat(query)
            .isEqualTo("{\n"
                + "  \"query\": {\n"
                + "    \"bool\": {\n"
                + "      \"filter\": {\n"
                + "        \"exists\": {\n"
                + "          \"field\": \"data.court\"\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"size\": 100,\n"
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
                + "  ]\n"
                + "}");
    }

    @Test
    void shouldReturnSearchAfterQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(false)
            .size(QUERY_SIZE)
            .searchAfterValue("1677777777")
            .build();
        String query = elasticSearchQuery.getQuery();

        assertThat(query)
            .isEqualTo("{\n"
                + "  \"query\": {\n"
                + "    \"bool\": {\n"
                + "      \"filter\": {\n"
                + "        \"exists\": {\n"
                + "          \"field\": \"data.court\"\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"size\": 100,\n"
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
                + "  ],\"search_after\": [1677777777]\n"
                + "}");
    }
}
