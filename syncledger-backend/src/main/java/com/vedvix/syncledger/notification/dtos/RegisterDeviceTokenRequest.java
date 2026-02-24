package com.vedvix.syncledger.notification.dtos;

import com.vedvix.syncledger.notification.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to register a device token for push notifications")
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "FCM token is required")
    @Schema(
            description = "Firebase Cloud Messaging token of the device",
            example = "fcm_ABC123xyzTOKEN",
            required = true
    )
    private String fcmToken;

    @NotNull(message = "Device platform is required")
    @Schema(
            description = "Platform of the device",
            example = "ANDROID",
            required = true,
            type = "string",
            allowableValues = {"ANDROID", "IOS", "WEB"}
    )
    private DevicePlatform platform;

    @Schema(
            description = "Unique device identifier (optional, for device-specific notifications)",
            example = "device_1234567890"
    )
    private String deviceId;

    @Schema(
            description = "Model of the device (optional)",
            example = "Samsung Galaxy S23"
    )
    private String deviceModel;

    @Schema(
            description = "Operating system of the device (optional)",
            example = "Android 13"
    )
    private String deviceOs;

    @Schema(
            description = "App version installed on the device (optional)",
            example = "1.0.3"
    )
    private String appVersion;
}
