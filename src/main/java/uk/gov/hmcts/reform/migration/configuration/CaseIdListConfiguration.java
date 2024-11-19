package uk.gov.hmcts.reform.migration.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.domain.util.ConfigParser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class CaseIdListConfiguration {

    private final Map<String, List<String>> mapping;

    public CaseIdListConfiguration(@Value("${case-migration.case_id_list.mapping:}") String config) {
        if (isBlank(config)) {
            mapping = Collections.emptyMap();
        } else {
            mapping = ConfigParser.parseConfig(config);
        }
    }

    public List<String> getCaseIds(String migrationId) {
        return this.mapping.getOrDefault(migrationId, List.of());
    }
}
