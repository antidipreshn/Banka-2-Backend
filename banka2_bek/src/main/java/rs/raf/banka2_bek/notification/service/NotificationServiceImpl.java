package rs.raf.banka2_bek.notification.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.model.NotificationType;

// ============================================================
// TODO [B1 - Notifikacioni sistem | Nosilac: Mina Kovacevic, Tadija]
//
// Servis koji upravlja in-app notifikacijama i prosledjuje ih
// na postojeci email kanal (MailSenderService).
// Injektovati: NotificationRepository i MailSenderService.
//
// IMPLEMENTIRATI (sve metode):
//
//   1. notify(Long recipientId, String recipientType,
//             NotificationType type, String title, String body,
//             String referenceType, Long referenceId) : void
//      — kreira i cuva Notification entitet u bazi
//        (recipientId, recipientType, type, title, body,
//         read = false, createdAt = LocalDateTime.now(),
//         referenceType, referenceId)
//      — ZATIM poziva email kanal (MailSenderService)
//        da posalje email primaocu ako je to relevantno za dati type;
//        email kanal se ne sme menjati — samo pozvati odgovarajucu
//        postojecu metodu (npr. sendPaymentConfirmationMail, itd.)
//      — anotirati sa @Transactional
//
//   2. getMyNotifications(String principalEmail, String recipientType,
//                         Boolean onlyUnread, int page, int size)
//      : Page<NotificationDto>
//      — vraca paginiranu listu notifikacija za primaoca
//      — ako je onlyUnread = true, filtrira samo neprocitane
//      — resolovati recipientId iz principalEmail tako sto se
//        upita ClientRepository ili EmployeeRepository na osnovu
//        recipientType ("CLIENT" ili "EMPLOYEE")
//
//   3. getUnreadCount(String principalEmail, String recipientType)
//      : long
//      — vraca broj neprocitanih notifikacija pozivom
//        NotificationRepository.countByRecipientIdAndRecipientTypeAndRead
//
//   4. markOneRead(Long notificationId, String principalEmail,
//                  String recipientType) : NotificationDto
//      — pronaci notifikaciju po ID-u
//      — proveriti da notifikacija zaista pripada tom primaocu
//        (baciti IllegalArgumentException ako ne)
//      — setovati read = true i sacuvati
//      — vratiti azurirani NotificationDto
//      — anotirati sa @Transactional
//
//   5. markAllRead(String principalEmail, String recipientType) : void
//      — pozvati NotificationRepository.markAllReadForRecipient
//      — anotirati sa @Transactional
//
//   6. (privatna) toDto(Notification n) : NotificationDto
//      — mapira Notification entitet u NotificationDto
//
// Konvencija: pratiti paket `savings` kao sablon.
// Spec: Zadaci_Backend.pdf, zadatak B1.
// ============================================================
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notify(Long recipientId, String recipientType, NotificationType notificationType, String title, String body, String referenceType, Long referenceId) {

    }

    @Override
    public Page<NotificationDto> getMyNotifications(String principalEmail, String recipientType, boolean onlyUnread, int page, int size) {
        return null;
    }

    @Override
    public Long getUnreadCount(String principalEmail, String recipientType) {
        return 0L;
    }

    @Transactional
    @Override
    public NotificationDto markOneRead(Long notificationId, String principalEmail, String recipientType) {
        return null;
    }

    @Transactional
    @Override
    public void markAllRead(String principalEmail, String recipientType) {

    }

}
