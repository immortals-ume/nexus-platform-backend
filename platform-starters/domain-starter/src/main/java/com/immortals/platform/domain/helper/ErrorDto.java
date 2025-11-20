package com.immortals.platform.domain.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ErrorDto {

    private HttpStatusCode status;
    private List<String> error;
    private String message;
    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private List<FieldValidationError> validationErrors;

    public ErrorDto() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorDto(HttpStatusCode status, String message, List<String> error, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }


    @Setter
    @Getter
    public static class FieldValidationError {
        private String field;
        private String message;

        public FieldValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

    }
}
