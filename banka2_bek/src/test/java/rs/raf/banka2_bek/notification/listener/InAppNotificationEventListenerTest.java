package rs.raf.banka2_bek.notification.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.notification.event.InAppNotificationEvent;
import rs.raf.banka2_bek.notification.model.NotificationType;
import rs.raf.banka2_bek.notification.service.MailSenderService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InAppNotificationEventListenerTest {

    @Mock
    private MailSenderService mailSenderService;

    @InjectMocks
    private InAppNotificationEventListener listener;

    @Test
    void onInAppNotificationEvent_sendsMailWithEventData() {
        InAppNotificationEvent event = InAppNotificationEvent.builder()
                .recipientEmail("user@test.rs")
                .title("Naslov")
                .body("Telo poruke")
                .notificationType(NotificationType.GENERAL)
                .build();

        listener.onInAppNotificationEvent(event);

        verify(mailSenderService).sendInAppNotificationMail("user@test.rs", "Naslov", "Telo poruke");
    }

    @Test
    void onInAppNotificationEvent_swallowsMailFailure() {
        InAppNotificationEvent event = InAppNotificationEvent.builder()
                .recipientEmail("user@test.rs")
                .title("Naslov")
                .body("Telo poruke")
                .notificationType(NotificationType.GENERAL)
                .build();
        doThrow(new RuntimeException("SMTP down"))
                .when(mailSenderService)
                .sendInAppNotificationMail("user@test.rs", "Naslov", "Telo poruke");

        assertDoesNotThrow(() -> listener.onInAppNotificationEvent(event));
    }
}
