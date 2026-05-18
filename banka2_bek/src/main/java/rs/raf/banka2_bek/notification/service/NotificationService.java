package rs.raf.banka2_bek.notification.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.model.NotificationType;


public interface NotificationService {


    void notify(
            Long recipientId,
            String recipientType,
            NotificationType notificationType,
            String title,
            String body,
            String referenceType,
            Long referenceId
    );


    Page<NotificationDto> getMyNotifications(String principalEmail, String recipientType, boolean onlyUnread, int page, int size);

    Long getUnreadCount(String principalEmail, String recipientType);

    @Transactional
    NotificationDto markOneRead(Long notificationId, String principalEmail, String recipientType);

    @Transactional
    void markAllRead(String principalEmail, String recipientType);
}
