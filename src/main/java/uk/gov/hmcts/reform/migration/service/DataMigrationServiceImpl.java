package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.adoption.model.common.Element;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.query.BooleanQuery;
import uk.gov.hmcts.reform.migration.query.EsQuery;
import uk.gov.hmcts.reform.migration.query.ExistsQuery;
import uk.gov.hmcts.reform.migration.query.Filter;
import uk.gov.hmcts.reform.migration.query.MatchQuery;
import uk.gov.hmcts.reform.migration.query.Must;
import uk.gov.hmcts.reform.migration.query.MustNot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataMigrationServiceImpl implements DataMigrationService<Map<String, Object>> {

    public static final String COURT = "court";
    private final Map<String, Function<CaseDetails, Map<String, Object>>> migrations = Map.of(
        "ADOP-log", this::triggerOnlyMigration,
        "ADOP-2555", this::triggerTtlMigration,
        "ADOP-2555-suspend", this::triggerSuspendMigrationTtl
        );

    private final Map<String, EsQuery> queries = Map.of(
        "ADOP-log", this.casesInState("Draft")
    );

    private EsQuery casesInState(String state) {
        final MatchQuery matchState = MatchQuery.of("state", state);

        return BooleanQuery.builder()
            .must(Must.builder()
                .clauses(List.of(matchState))
                .build())
            .build();
    }

    @Override
    public void validateMigrationId(String migrationId) {
        if (!migrations.containsKey(migrationId)) {
            throw new NoSuchElementException("No migration mapped to " + migrationId);
        }
    }

    @Override
    public EsQuery getQuery(String migrationId) {
        if (!queries.containsKey(migrationId)) {
            throw new NoSuchElementException("No migration mapped to " + migrationId);
        }
        log.info(queries.get(migrationId).toQueryContext(100, 0).toString());
        return queries.get(migrationId);
    }


    @Override
    public Predicate<CaseDetails> accepts() {
        return caseDetails -> true;
    }

    @Override
    public Map<String, Object> migrate(CaseDetails caseDetails, String migrationId) {
        requireNonNull(migrationId, "Migration ID must not be null");
        if (!migrations.containsKey(migrationId)) {
            throw new NoSuchElementException("No migration mapped to " + migrationId);
        }

        // Perform Migration
        return migrations.get(migrationId).apply(caseDetails);
    }

    private EsQuery topLevelFieldExistsQuery(String field) {
        return BooleanQuery.builder()
            .filter(Filter.builder()
                .clauses(List.of(ExistsQuery.of("data." + field)))
                .build())
            .build();
    }

    private EsQuery topLevelFieldDoesNotExistQuery(String field) {
        return BooleanQuery.builder()
            .filter(Filter.builder()
                .clauses(List.of(BooleanQuery.builder()
                    .mustNot(MustNot.of(ExistsQuery.of("data." + field)))
                    .build()))
                .build())
            .build();
    }

    private Map<String, Object> triggerOnlyMigration(CaseDetails caseDetails) {
        // do nothing
        return new HashMap<>();
    }

    public Map<String, Object> triggerTtlMigration(CaseDetails caseDetails) {
        HashMap<String, Object> ttlMap = new HashMap<>();
        ttlMap.put("OverrideTTL", null);
        ttlMap.put("Suspended", "No");

        ObjectMapper objectMapper = new ObjectMapper();

        switch (caseDetails.getState()) {
            case "Draft":
                ttlMap.put("SystemTTL", caseDetails.getCreatedDate().toLocalDate().plusDays(90)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                break;
            case "AwaitingPayment":
                List<Element<Map<String,Object>>> applicationPayments = objectMapper.convertValue(
                    caseDetails.getData().getOrDefault("applicationPayments", null),
                    new TypeReference<List<Element<Map<String,Object>>>>() {}
                );

                if (isNull(applicationPayments)) {
                    throw new AssertionError(format("Migration 2555, case with id: %s "
                        + "has no applicationPayments in case data as expected", caseDetails.getId()));
                }

                List<LocalDate> paymentDates = new ArrayList<>();
                for (Element<Map<String,Object>> payment : applicationPayments) {
                    paymentDates.add(LocalDateTime.parse(payment.getValue().get("created").toString()).toLocalDate());
                }

                Collections.sort(paymentDates);
                LocalDate oldestApplicationCreatedDate = paymentDates.get(0);

                ttlMap.put("SystemTTL", oldestApplicationCreatedDate.plusDays(36524)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                break;
            case "Submitted":
                String dateSubmittedString = objectMapper.convertValue(
                    caseDetails.getData().getOrDefault("dateSubmitted", null),
                    new TypeReference<String>() {}
                );

                if (isNull(dateSubmittedString)) {
                    throw new AssertionError(format("Migration 2555, case with id: %s "
                        + "has no dateSubmitted in case data as expected", caseDetails.getId()));
                }

                LocalDate dateSubmitted = LocalDate.parse(dateSubmittedString);

                ttlMap.put("SystemTTL", dateSubmitted.plusDays(36524)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                break;
            case "LaSubmitted":
                ttlMap.put("SystemTTL", caseDetails.getLastModified().toLocalDate().plusDays(36524)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                break;
            default:
                throw new AssertionError(format("Migration 2555, case with id: %s "
                    + "not in valid state for TTL migration", caseDetails.getId()));
        }

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("TTL", ttlMap);
        return updates;
    }

    public Map<String, Object> triggerSuspendMigrationTtl(CaseDetails caseDetails) {
        HashMap<String, Object> updates = new HashMap<>();
        HashMap<String, Object> ttlMap = new HashMap<>();

        if (caseDetails.getData().containsKey("TTL")) {
            ttlMap.put("OverrideTTL", caseDetails.getData().getOrDefault("OverrideTTL", null));
            ttlMap.put("Suspended", "Yes");
            ttlMap.put("SystemTTL", caseDetails.getData().getOrDefault("SystemTTL", null));
        } else {
            ttlMap.put("OverrideTTL", null);
            ttlMap.put("Suspended", "Yes");
            ttlMap.put("SystemTTL", null);
        }

        updates.put("TTL", ttlMap);
        return updates;
    }

    public Map<String, Object> triggerRemoveMigrationTtl(CaseDetails caseDetails) {
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, Object> updates = objectMapper.convertValue(caseDetails.getData(),
            new TypeReference<HashMap<String, Object>>() {});

        if (caseDetails.getData().containsKey("TTL")) {
            updates.remove("TTL");
        }

        return updates;
    }
}
