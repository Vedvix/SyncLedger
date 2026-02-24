package com.vedvix.syncledger.notification.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SmsozoneApiResponse {

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("JobId")
    private String jobId;

    @JsonProperty("MessageData")
    private List<MessageData> messageData;

    @Data
    public static class MessageData {
        @JsonProperty("Number")
        private String number;

        @JsonProperty("MessageId")
        private String messageId;
    }

    public boolean isSuccess() {
        return "000".equals(errorCode);
    }
}
