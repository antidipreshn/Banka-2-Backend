package rs.raf.banka2_bek.notification.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.banka2_bek.auth.util.UserRole;
import rs.raf.banka2_bek.client.repository.ClientRepository;
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

    /**
     * Single entry point for raising a notification. Persists the in-app
     * notification and, when the type sends e-mail, hands the e-mail off to be
     * delivered after this transaction commits — see InAppNotificationEventListener.
     */
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
    public Page<NotificationDto> getMyNotifications(String principalEmail,
                                                    String recipientType,
                                                    boolean onlyUnread,
                                                    int page,
                                                    int size) {
        Long recipientId = resolveRecipientId(principalEmail, recipientType);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> result = onlyUnread
                ? notificationRepository.findByRecipientIdAndRecipientTypeAndRead(
                        recipientId, recipientType, false, pageable)
                : notificationRepository.findByRecipientIdAndRecipientType(
                        recipientId, recipientType, pageable);
        return result.map(NotificationObjectMapper::toDto);
    }

    @Override
    public Long getUnreadCount(String principalEmail, String recipientType) {
        Long recipientId = resolveRecipientId(principalEmail, recipientType);
        return notificationRepository.countByRecipientIdAndRecipientTypeAndRead(
                recipientId, recipientType, false);
    }

    @Transactional
    @Override
    public NotificationDto markOneRead(Long notificationId, String principalEmail, String recipientType) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new InAppNotificationException(
                        "Notification with id " + notificationId + " not found"));

        Long recipientId = resolveRecipientId(principalEmail, recipientType);
        if (!recipientId.equals(notification.getRecipientId())
                || !recipientType.equals(notification.getRecipientType())) {
            throw new IllegalArgumentException(
                    "Notification " + notificationId + " does not belong to the current user");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationObjectMapper.toDto(saved);
    }

    @Transactional
    @Override
    public void markAllRead(String principalEmail, String recipientType) {
        Long recipientId = resolveRecipientId(principalEmail, recipientType);
        notificationRepository.markAllReadForRecipient(recipientId, recipientType);
    }

    /**
     * Resolves the recipient's e-mail and publishes the event that triggers it.
     * Any failure here is logged and swallowed: the notification is already
     * persisted and an e-mail problem must not roll it back.
     */
    private void queueEmail(Long recipientId,
                            String recipientType,
                            NotificationType notificationType,
                            String title,
                            String body,
                            String referenceType,
                            Long referenceId) {
        try {
            String recipientEmail = resolveEmail(recipientId, recipientType);
            eventPublisher.publishEvent(InAppNotificationEvent.builder()
                    .recipientEmail(recipientEmail)
                    .title(title)
                    .body(body)
                    .notificationType(notificationType)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build());
        } catch (Exception e) {
            log.warn("Could not queue notification e-mail for recipientId={}, type={}",
                    recipientId, notificationType, e);
        }
    }

    private String resolveEmail(Long recipientId, String recipientType) {
        if (UserRole.EMPLOYEE.equals(recipientType)) {
            return employeeRepository.findById(recipientId)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Employee with id " + recipientId + " not found"))
                    .getEmail();
        }
        if (UserRole.CLIENT.equals(recipientType)) {
            return clientRepository.findById(recipientId)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Client with id " + recipientId + " not found"))
                    .getEmail();
        }
        throw new InAppNotificationException(
                "recipientType must be \"CLIENT\" or \"EMPLOYEE\", got: " + recipientType);
    }

    private Long resolveRecipientId(String principalEmail, String recipientType) {
        if (UserRole.EMPLOYEE.equals(recipientType)) {
            return employeeRepository.findByEmail(principalEmail)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Employee with e-mail " + principalEmail + " not found"))
                    .getId();
        }
        if (UserRole.CLIENT.equals(recipientType)) {
            return clientRepository.findByEmail(principalEmail)
                    .orElseThrow(() -> new InAppNotificationException(
                            "Client with e-mail " + principalEmail + " not found"))
                    .getId();
        }
        throw new InAppNotificationException(
                "recipientType must be \"CLIENT\" or \"EMPLOYEE\", got: " + recipientType);
    }
}
