package rs.raf.banka2_bek.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Package-private utility that sends a single HTML email via Spring's
 * {@link JavaMailSender}. Configures the {@link MimeMessageHelper} with
 * UTF-8 encoding, sets From / To / Subject / HTML body, and delegates
 * to {@link JavaMailSender#send}.
 *
 * <p>All {@link jakarta.mail.MessagingException}s are re-thrown wrapped in a
 * {@link RuntimeException} so callers do not need to declare checked exceptions.
 */
final class HtmlMailSender {

    private HtmlMailSender() {
    }

    /**
     * Composes and sends an HTML email.
     *
     * @param mailSender  the configured Spring mail sender bean
     * @param fromAddress the RFC-5321 From address
     * @param toEmail     the recipient address
     * @param subject     the email subject line
     * @param html        the full HTML body; must be valid HTML
     * @throws RuntimeException wrapping any {@link jakarta.mail.MessagingException}
     */
    static void sendHtmlMail(
            JavaMailSender mailSender,
            String fromAddress,
            String toEmail,
            String subject,
            String html
    )
    {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        }
        catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}

