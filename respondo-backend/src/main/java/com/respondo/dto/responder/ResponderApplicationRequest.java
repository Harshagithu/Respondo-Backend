package com.respondo.dto.responder;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponderApplicationRequest {

    @Size(max = 500)
    private String skills;
}
