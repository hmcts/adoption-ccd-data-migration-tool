package uk.gov.hmcts.reform.migration.service;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataMigrationServiceImpl implements DataMigrationService<Map<String, Object>> {

    public static final String COURT = "court";
    private final Map<String, Function<CaseDetails, Map<String, Object>>> migrations = Map.of(
        "ADOP-log", this::triggerOnlyMigration,
        "ADOP-2555", this::triggerTtlMigration
        );

    private final Map<String, EsQuery> queries = Map.of(
        "ADOP-1234", this.closedCases()
    );

    private EsQuery closedCases() {
        final MatchQuery closedState = MatchQuery.of("state", "CLOSED");

        return BooleanQuery.builder()
            .must(Must.builder()
                .clauses(List.of(closedState))
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
        ttlMap.put("Suspend", "NO");

        switch (caseDetails.getState()) {
            case "Draft":
                ttlMap.put("SystemTTL", caseDetails.getCreatedDate().toLocalDate().plusDays(90));
                break;
            case "AwaitingPayment":
                @SuppressWarnings("unchecked")
                Map<String, Object> application = (Map<String, Object>) caseDetails.getData().get("application");
                @SuppressWarnings("unchecked")
                List<Element<Map<String,Object>>> applicationPayments =
                    (List<Element<Map<String,Object>>>) application.get("applicationPayments");
                List<LocalDateTime> paymentDates = new ArrayList<>();

                for (Element<Map<String,Object>> payment : applicationPayments) {
                    paymentDates.add((LocalDateTime) payment.getValue().get("created"));
                }

                Collections.sort(paymentDates);
                LocalDateTime oldestApplicationCreatedDate = paymentDates.get(0);

                ttlMap.put("SystemTTL", oldestApplicationCreatedDate.toLocalDate().plusDays(36524));
                break;
            case "Submitted":
                @SuppressWarnings("unchecked")
                Map<String, Object> applicationData = (Map<String, Object>) caseDetails.getData().get("application");
                LocalDate dateSubmitted = (LocalDate) applicationData.get("dateSubmitted");

                ttlMap.put("SystemTTL", dateSubmitted.plusDays(36524));
                break;
            case "LaSubmitted":
                ttlMap.put("SystemTTL", caseDetails.getLastModified().toLocalDate().plusDays(36524));
                break;
            default:
                throw new AssertionError(format("Migration 2555, case with id: %s "
                    + "not in valid state for TTL migration", caseDetails.getId()));
        }

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("TTL", ttlMap);
        return updates;
    }
}
