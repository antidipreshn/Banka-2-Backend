package rs.raf.banka2_bek.notification.service;

import org.springframework.data.domain.Page;
import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.model.NotificationType;

public interface NotificationService {

    /**
     * Single entry point for raising a notification: persists the in-app
     * notification and, for types that send e-mail, dispatches one. Other
     * modules (B4/B5/B8) call this directly.
     */
    void notify(
            Long recipientId,
            String recipientType,
            NotificationType notificationType,
            String title,
            String body,
            String referenceType,
            Long referenceId
    );

    Page<NotificationDto> getMyNotifications(Long recipientId, String recipientType, boolean onlyUnread, int page, int size);

    Long getUnreadCount(Long recipientId, String recipientType);

    NotificationDto markOneRead(Long notificationId, Long recipientId, String recipientType);

    void markAllRead(Long recipientId, String recipientType);
}
