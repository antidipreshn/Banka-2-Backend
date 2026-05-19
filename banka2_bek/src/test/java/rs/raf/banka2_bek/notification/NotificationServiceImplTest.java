package rs.raf.banka2_bek.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import rs.raf.banka2_bek.client.model.Client;
import rs.raf.banka2_bek.client.repository.ClientRepository;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;
import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.event.InAppNotificationEvent;
import rs.raf.banka2_bek.notification.exception.InAppNotificationException;
import rs.raf.banka2_bek.notification.model.Notification;
import rs.raf.banka2_bek.notification.model.NotificationType;
import rs.raf.banka2_bek.notification.repository.NotificationRepository;
import rs.raf.banka2_bek.notification.service.NotificationServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long CLIENT_ID = 5L;
    private static final Long EMPLOYEE_ID = 8L;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;
    @Captor
    private ArgumentCaptor<InAppNotificationEvent> eventCaptor;

    @Test
    void notify_persistsNotificationAndDelegatesToEmail() {
        Client client = mock(Client.class);
        when(client.getEmail()).thenReturn("marko@test.rs");
        when(client.getFirstName()).thenReturn("Marko");
        when(client.getLastName()).thenReturn("Markovic");
        when(client.getGender()).thenReturn("M");
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        notificationService.notify(CLIENT_ID, "CLIENT", NotificationType.PAYMENT,
                "Placanje", "Vase placanje je izvrseno", "PAYMENT", 99L);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(CLIENT_ID, saved.getRecipientId());
        assertEquals("CLIENT", saved.getRecipientType());
        assertEquals(NotificationType.PAYMENT, saved.getNotificationType());
        assertEquals("Placanje", saved.getTitle());
        assertEquals("Vase placanje je izvrseno", saved.getBody());
        assertFalse(saved.isRead());
        assertEquals("PAYMENT", saved.getReferenceType());
        assertEquals(99L, saved.getReferenceId().longValue());

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InAppNotificationEvent event = eventCaptor.getValue();
        assertEquals("marko@test.rs", event.getRecipientEmail());
        assertEquals("Marko", event.getFirstName());
        assertEquals("Markovic", event.getLastName());
        assertEquals("M", event.getGender());
        assertEquals("Placanje", event.getTitle());
        assertEquals("Vase placanje je izvrseno", event.getBody());
        assertEquals(NotificationType.PAYMENT, event.getNotificationType());
    }

    @Test
    void notify_emailFailureDoesNotRollbackPersistence() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        notificationService.notify(CLIENT_ID, "CLIENT", NotificationType.PAYMENT,
                "Placanje", "telo", null, null);

        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getMyNotifications_returnsAllWhenOnlyUnreadFalse() {
        Page<Notification> page = new PageImpl<>(List.of(
                notification(false), notification(true), notification(false)));
        when(notificationRepository.findByRecipientIdAndRecipientType(
                eq(CLIENT_ID), eq("CLIENT"), any(Pageable.class))).thenReturn(page);

        Page<NotificationDto> result =
                notificationService.getMyNotifications(CLIENT_ID, "CLIENT", false, 0, 20);

        assertEquals(3, result.getContent().size());
    }

    @Test
    void getMyNotifications_returnsOnlyUnreadWhenFlagTrue() {
        Page<Notification> page = new PageImpl<>(List.of(notification(false)));
        when(notificationRepository.findByRecipientIdAndRecipientTypeAndRead(
                eq(CLIENT_ID), eq("CLIENT"), eq(false), any(Pageable.class))).thenReturn(page);

        Page<NotificationDto> result =
                notificationService.getMyNotifications(CLIENT_ID, "CLIENT", true, 0, 20);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByRecipientIdAndRecipientTypeAndRead(EMPLOYEE_ID, "EMPLOYEE", false))
                .thenReturn(7L);

        Long count = notificationService.getUnreadCount(EMPLOYEE_ID, "EMPLOYEE");

        assertEquals(7L, count.longValue());
    }

    @Test
    void markOneRead_updatesReadFlagAndReturnsDto() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification(false)));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto dto = notificationService.markOneRead(10L, CLIENT_ID, "CLIENT");

        verify(notificationRepository).save(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().isRead());
        assertTrue(dto.isRead());
    }

    @Test
    void markOneRead_throwsWhenNotificationBelongsToOtherRecipient() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification(false)));

        assertThrows(AccessDeniedException.class,
                () -> notificationService.markOneRead(10L, 999L, "CLIENT"));
    }

    @Test
    void markOneRead_throwsWhenNotificationNotFound() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(InAppNotificationException.class,
                () -> notificationService.markOneRead(404L, CLIENT_ID, "CLIENT"));
    }

    @Test
    void markAllRead_delegatesToRepository() {
        notificationService.markAllRead(EMPLOYEE_ID, "EMPLOYEE");

        verify(notificationRepository).markAllReadForRecipient(EMPLOYEE_ID, "EMPLOYEE");
    }

    private Notification notification(boolean read) {
        return Notification.builder()
                .id(1L)
                .recipientId(CLIENT_ID)
                .recipientType("CLIENT")
                .notificationType(NotificationType.GENERAL)
                .title("Naslov")
                .body("Telo")
                .read(read)
                .build();
    }
}
