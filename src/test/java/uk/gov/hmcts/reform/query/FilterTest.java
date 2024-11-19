package uk.gov.hmcts.reform.query;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.migration.query.Filter;
import uk.gov.hmcts.reform.migration.query.RangeQuery;
import uk.gov.hmcts.reform.migration.query.TermQuery;
import uk.gov.hmcts.reform.migration.query.TermsQuery;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterTest {

    public static final List<String> REQUIRED_STATES = List.of(
        "submitted","gatekeeping","prepare_for_hearing","final_hearing"
    );

    @Test
    void shouldReturnSortClause() {
        TermsQuery termsQuery = TermsQuery.of("state", REQUIRED_STATES);
        TermQuery termQuery = TermQuery.of("data.court.code", "344");
        RangeQuery rangeQuery = RangeQuery.builder()
            .field("data.dateSubmitted")
            .lessThan("29-Sept-2022")
            .build();

        Filter filter = Filter.builder()
            .clauses(List.of(
                termQuery, termsQuery, rangeQuery
            ))
            .build();

        JSONObject expected = new JSONObject(
            Map.of("filter",
                List.of(
                    Map.of("term", Map.of("data.court.code","344")),
                    Map.of("terms", Map.of("state",
                        List.of("submitted","gatekeeping","prepare_for_hearing","final_hearing"))),
                    Map.of("range", Map.of("data.dateSubmitted", Map.of("lt","29-Sept-2022"))))));

        JSONObject actual = new JSONObject(filter.toMap());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
