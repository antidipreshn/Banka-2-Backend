package rs.raf.banka2_bek.notification.event;

import lombok.Builder;
import lombok.Getter;
import rs.raf.banka2_bek.notification.model.NotificationType;

/**
 * Internal event published by {@code NotificationService.notify(...)} once a
 * notification has been persisted. It carries everything the e-mail channel
 * needs — including the recipient's name and gender — so that mail can be sent
 * after the transaction commits, off the caller's thread, and so e-mail
 * templates can be personalized.
 *
 * <p>This event is an implementation detail of the notification module — other
 * modules raise notifications by calling {@code notify(...)}, never by
 * publishing this event.
 */
@Getter
@Builder
public class InAppNotificationEvent {

    private final String recipientEmail;
    private final String firstName;
    private final String lastName;
    private final String gender;
    private final NotificationType notificationType;
    private final String title;
    private final String body;
    private final String referenceType;
    private final Long referenceId;
}
