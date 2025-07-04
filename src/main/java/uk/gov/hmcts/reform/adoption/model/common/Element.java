package uk.gov.hmcts.reform.adoption.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Element<T> {
    private String id;

    @NotNull
    @Valid
    private T value;

    public static <T> Element<T> newElement(T value) {
        return Element.<T>builder().id(UUID.randomUUID().toString()).value(value).build();
    }
}
