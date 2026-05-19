package rs.raf.banka2_bek.notification.mapper;

import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.model.Notification;

public final class NotificationObjectMapper {

    private NotificationObjectMapper() {
    }

    public static NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getNotificationType().name())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .build();
    }
}
