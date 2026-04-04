package com.vedvix.syncledger.service.erp;

/**
 * Generic result of an ERP sync operation. Replaces SageIntacctService.SageResponse as the
 * common return type for all connector implementations.
 *
 * @author vedvix
 */
public record ErpSyncResult(
        boolean success,
        String remoteRecordId,
        int httpStatusCode,
        String errorCode,
        String errorMessage,
        String requestPayload,
        String responsePayload
) {
    public static ErpSyncResult ok(String remoteRecordId, int httpStatus, String request, String response) {
        return new ErpSyncResult(true, remoteRecordId, httpStatus, null, null, request, response);
    }

    public static ErpSyncResult fail(String remoteRecordId, int httpStatus, String code, String msg,
                                     String request, String response) {
        return new ErpSyncResult(false, remoteRecordId, httpStatus, code, msg, request, response);
    }
}
