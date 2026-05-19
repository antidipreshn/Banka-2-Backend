package rs.raf.banka2_bek.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.raf.banka2_bek.notification.event.InAppNotificationEvent;
import rs.raf.banka2_bek.notification.service.MailSenderService;

/**
 * Sends the e-mail that accompanies an in-app notification.
 *
 * <p>Runs after the {@code notify(...)} transaction commits, so no e-mail is
 * ever sent for a notification that was rolled back, and on a separate thread,
 * so a slow SMTP server never blocks the caller. A failure here is logged and
 * swallowed — the in-app notification is already persisted and must not be
 * affected by an e-mail problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationEventListener {

    private final MailSenderService mailSenderService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInAppNotificationEvent(InAppNotificationEvent event) {
        try {
            mailSenderService.sendInAppNotificationMail(event);
        } catch (Exception e) {
            log.warn("Failed to send notification e-mail to {}", event.getRecipientEmail(), e);
        }
    }
}
