package rs.raf.banka2_bek.notification.event;

import lombok.Builder;
import lombok.Getter;
import rs.raf.banka2_bek.notification.model.NotificationType;

/**
 * Internal event published by {@code NotificationService.notify(...)} once a
 * notification has been persisted. It carries everything the e-mail channel
 * needs, so that mail can be sent after the transaction commits and off the
 * caller's thread.
 *
 * <p>This event is an implementation detail of the notification module — other
 * modules raise notifications by calling {@code notify(...)}, never by
 * publishing this event.
 */
@Getter
@Builder
public class InAppNotificationEvent {

    private final String recipientEmail;
    private final String title;
    private final String body;
    private final NotificationType notificationType;
    private final String referenceType;
    private final Long referenceId;
}
