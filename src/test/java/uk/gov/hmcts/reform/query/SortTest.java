package uk.gov.hmcts.reform.query;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.migration.query.Sort;
import uk.gov.hmcts.reform.migration.query.SortOrder;
import uk.gov.hmcts.reform.migration.query.SortQuery;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SortTest {

    @Test
    void shouldReturnSortClause() {
        Sort sort = Sort.builder()
            .clauses(List.of(
                    SortQuery.of("data.dateSubmitted", SortOrder.DESC)
            ))
            .build();
        List<Object> objects = sort.toMap();
        JSONObject jsonObject = new JSONObject(Map.of("sort", objects));
        JSONObject expected = new JSONObject(Map.of("sort",
                List.of(Map.of("data.dateSubmitted", Map.of("order","desc")))));
        assertThat(jsonObject).usingRecursiveComparison().isEqualTo(expected);
    }
}
