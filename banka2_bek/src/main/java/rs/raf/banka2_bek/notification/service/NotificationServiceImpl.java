package rs.raf.banka2_bek.notification.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.banka2_bek.auth.util.UserRole;
import rs.raf.banka2_bek.client.model.Client;
import rs.raf.banka2_bek.client.repository.ClientRepository;
import rs.raf.banka2_bek.employee.model.Employee;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;
import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.event.InAppNotificationEvent;
import rs.raf.banka2_bek.notification.exception.InAppNotificationException;
import rs.raf.banka2_bek.notification.mapper.NotificationObjectMapper;
import rs.raf.banka2_bek.notification.model.Notification;
import rs.raf.banka2_bek.notification.model.NotificationType;
import rs.raf.banka2_bek.notification.repository.NotificationRepository;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @Transactional
    @Override
    public void notify(Long recipientId,
                       String recipientType,
                       NotificationType notificationType,
                       String title,
                       String body,
                       String referenceType,
                       Long referenceId) {

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .recipientType(recipientType)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .read(false)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);

        if (notificationType.isSendsEmail()) {
            queueEmail(recipientId, recipientType, notificationType, title, body, referenceType, referenceId);
        }
    }

    @Override
    public Page<NotificationDto> getMyNotifications(Long recipientId,
                                                    String recipientType,
                                                    boolean onlyUnread,
                                                    int page,
                                                    int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> result = onlyUnread
                ? notificationRepository.findByRecipientIdAndRecipientTypeAndRead(
                        recipientId, recipientType, false, pageable)
                : notificationRepository.findByRecipientIdAndRecipientType(
                        recipientId, recipientType, pageable);
        return result.map(NotificationObjectMapper::toDto);
    }

    @Override
    public Long getUnreadCount(Long recipientId, String recipientType) {
        return notificationRepository.countByRecipientIdAndRecipientTypeAndRead(
                recipientId, recipientType, false);
    }

    @Transactional
    @Override
    public NotificationDto markOneRead(Long notificationId, Long recipientId, String recipientType) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new InAppNotificationException(
                        "Notification with id " + notificationId + " not found"));

        if (!notification.getRecipientId().equals(recipientId)
                || !notification.getRecipientType().equals(recipientType)) {
            throw new AccessDeniedException(
                    "Notification " + notificationId + " does not belong to the current user");
        }

        notification.setRead(true);
        return NotificationObjectMapper.toDto(notificationRepository.save(notification));
    }

    @Transactional
    @Override
    public void markAllRead(Long recipientId, String recipientType) {
        notificationRepository.markAllReadForRecipient(recipientId, recipientType);
    }

    /**
     * Resolves the recipient's contact data and publishes the event that
     * triggers the e-mail. Any failure here is logged and swallowed: the
     * notification is already persisted and an e-mail problem must not roll it
     * back.
     */
    private void queueEmail(Long recipientId,
                            String recipientType,
                            NotificationType notificationType,
                            String title,
                            String body,
                            String referenceType,
                            Long referenceId) {
        try {
            RecipientContact contact = resolveContact(recipientId, recipientType);
            eventPublisher.publishEvent(InAppNotificationEvent.builder()
                    .recipientEmail(contact.email())
                    .firstName(contact.firstName())
                    .lastName(contact.lastName())
                    .gender(contact.gender())
                    .notificationType(notificationType)
                    .title(title)
                    .body(body)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build());
        } catch (Exception e) {
            log.warn("Could not queue notification e-mail for recipientId={}, type={}",
                    recipientId, notificationType, e);
        }
    }

    private RecipientContact resolveContact(Long recipientId, String recipientType) {
        if (UserRole.EMPLOYEE.equals(recipientType)) {
            Employee employee = employeeRepository.findById(recipientId)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Employee with id " + recipientId + " not found"));
            return new RecipientContact(employee.getEmail(), employee.getFirstName(),
                    employee.getLastName(), employee.getGender());
        }
        if (UserRole.CLIENT.equals(recipientType)) {
            Client client = clientRepository.findById(recipientId)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Client with id " + recipientId + " not found"));
            return new RecipientContact(client.getEmail(), client.getFirstName(),
                    client.getLastName(), client.getGender());
        }
        throw new InAppNotificationException(
                "recipientType must be \"CLIENT\" or \"EMPLOYEE\", got: " + recipientType);
    }

    private record RecipientContact(String email, String firstName, String lastName, String gender) {
    }
}
