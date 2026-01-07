package io.confluent.schemaregistry.pg.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Standard error response format matching Confluent Schema Registry.
 * All errors return this format with an error_code and message.
 */
@Value
@Builder
public class ErrorResponse {
    @JsonProperty("error_code")
    int errorCode;

    String message;

    public static ErrorResponse of(int errorCode, String message) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
