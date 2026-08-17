package com.respondo.dto.dispatcher;

import com.respondo.enums.ResponderAvailability;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableResponderResponse {
    private Long responderId;
    private Long userId;
    private String fullName;
    private String teamName;
    private ResponderAvailability availability;
}
