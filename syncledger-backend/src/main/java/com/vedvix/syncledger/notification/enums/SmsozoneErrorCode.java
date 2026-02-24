package com.vedvix.syncledger.notification.enums;

import lombok.Getter;

@Getter
public enum SmsozoneErrorCode {

    DONE("000", false),
    LOGIN_DETAILS_BLANK("001", false),
    SENDER_BLANK("003", false),
    MESSAGE_TEXT_BLANK("004", false),
    MESSAGE_DATA_BLANK("005", false),
    GENERIC_ERROR("006", true),
    INVALID_CREDENTIALS("007", false),
    ACCOUNT_NOT_ACTIVE("008", false),
    ACCOUNT_LOCKED("009", false),
    API_RESTRICTION("010", false),
    IP_RESTRICTION("011", false),
    INVALID_MESSAGE_LENGTH("012", false),
    INVALID_MOBILE("013", false),
    ACCOUNT_LOCKED_SPAM("014", false),
    INVALID_SENDER_ID("015", false),
    INVALID_GROUP_ID("017", false),
    GROUP_NOT_SUPPORTED("018", false),
    INVALID_SCHEDULE_DATE("019", false),
    MESSAGE_OR_NUMBER_BLANK("020", false),
    INSUFFICIENT_CREDITS("021", false),
    INVALID_JOB_ID("022", false),
    PARAMETER_MISSING("023", false),
    TEMPLATE_MISMATCH("024", false),
    FIELD_BLANK("025", false),
    INVALID_DATE_RANGE("026", false),
    INVALID_OPTIN("027", false),
    UNKNOWN("999", true);

    private final String code;
    private final boolean retryable;

    SmsozoneErrorCode(String code, boolean retryable) {
        this.code = code;
        this.retryable = retryable;
    }

    public static SmsozoneErrorCode from(String code) {
        for (SmsozoneErrorCode c : values()) {
            if (c.code.equals(code)) return c;
        }
        return UNKNOWN;
    }
}
