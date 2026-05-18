package rs.raf.banka2_bek.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.auth.model.PasswordResetRequestedEvent;
import rs.raf.banka2_bek.notification.service.MailSenderService;

@Component
@RequiredArgsConstructor
public class PasswordResetRequestedListener {

    private final MailSenderService mailSenderService;


    @Async
    @EventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        mailSenderService.sendPasswordResetMail(event.getEmail(), event.getToken());
    }
}

