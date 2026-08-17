package com.respondo.dto.dispatcher;

import com.respondo.enums.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriorityChangeRequest {

    @NotNull(message = "Priority is required")
    private Priority priority;

    @Size(max = 500)
    private String reason;
}
