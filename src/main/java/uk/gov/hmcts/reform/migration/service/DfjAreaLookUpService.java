package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.model.DfjAreaCourtMapping;
import uk.gov.hmcts.reform.domain.util.ResourceReader;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class DfjAreaLookUpService {

    private final ObjectMapper objectMapper;
    private List<DfjAreaCourtMapping> dfjCourtMapping;

    public DfjAreaLookUpService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadDfjMappings();
    }

    private void loadDfjMappings() {
        try {
            final String jsonContent = ResourceReader.readString("static_data/dfjAreaCourtMapping.json");
            dfjCourtMapping = objectMapper.readValue(jsonContent, new TypeReference<List<DfjAreaCourtMapping>>() {});
        } catch (IOException e) {
            log.error("Unable to parse dfjAreaCourtMapping.json file.", e);
        }
    }

    public DfjAreaCourtMapping getDfjArea(String courtCode) {
        return dfjCourtMapping.stream()
            .filter(dfjCourtMap -> dfjCourtMap.getCourtCode().equals(courtCode))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("No dfjArea found for court code: " + courtCode));
    }
}
