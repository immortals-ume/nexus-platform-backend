package com.immortals.notificationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "notification_capture", schema = "notification")
@Getter
@Setter
public class NotificationCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "notification_id", nullable = false)
    @JdbcTypeCode(SqlTypes.BIGINT)
    private Long notificationId;

    @Column(name = "notification_content", length = 180, nullable = false)
    private String notificationContent;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "notification_sent_status", nullable = false)
    private String notificationSentStatus;

    @Column(name = "expired_timestamp")
    private Instant expiredTimestamp;

    @Column(name = "notification_sent_email")
    private String notificationSentEmail;

    @Column(name = "notification_receive_email")
    private String notificationReceiveEmail;

    @Column(name = "notification_country_code", length = 3)
    private String notificationCountryCode;

    @Column(name = "notification_sent_sms_phone_number", length = 12)
    private String notificationSentSmsPhoneNumber;

    @Column(name = "notification_receiver_sms_phone_number", length = 12)
    private String notificationReceiverSmsPhoneNumber;

    @Column(name = "notification_sent_whatsapp_phone_number", length = 12)
    private String notificationSentWhatsappPhoneNumber;

    @Column(name = "notification_receiver_whatsapp_phone_number", length = 12)
    private String notificationReceiverWhatsappPhoneNumber;

    @Column(name = "notification_created_timestamp", nullable = false)
    private Instant notificationCreatedTimestamp;

    @Column(name = "notification_created_by", nullable = false)
    private String notificationCreatedBy;

    @Column(name = "notification_updated_timestamp")
    private Instant notificationUpdatedTimestamp;

    @Column(name = "notification_updated_by")
    private String notificationUpdatedBy;

    @Column(name = "active_ind", nullable = false)
    private Boolean activeInd;
}
