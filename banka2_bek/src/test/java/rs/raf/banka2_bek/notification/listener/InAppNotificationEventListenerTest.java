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

    private InAppNotificationEvent event() {
        return InAppNotificationEvent.builder()
                .recipientEmail("user@test.rs")
                .firstName("Marko")
                .lastName("Markovic")
                .gender("M")
                .notificationType(NotificationType.GENERAL)
                .title("Naslov")
                .body("Telo poruke")
                .build();
    }

    @Test
    void onInAppNotificationEvent_sendsMailForEvent() {
        InAppNotificationEvent event = event();

        listener.onInAppNotificationEvent(event);

        verify(mailSenderService).sendInAppNotificationMail(event);
    }

    @Test
    void onInAppNotificationEvent_swallowsMailFailure() {
        InAppNotificationEvent event = event();
        doThrow(new RuntimeException("SMTP down"))
                .when(mailSenderService).sendInAppNotificationMail(event);

        assertDoesNotThrow(() -> listener.onInAppNotificationEvent(event));
    }
}
