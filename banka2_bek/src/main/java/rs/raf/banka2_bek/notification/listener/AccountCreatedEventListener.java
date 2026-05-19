package rs.raf.banka2_bek.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.account.event.AccountCreatedEvent;
import rs.raf.banka2_bek.notification.service.MailSenderService;

@Component
@RequiredArgsConstructor
public class AccountCreatedEventListener {

    private final MailSenderService mailSenderService;


    @Async
    @EventListener
    public void onClientAccountCreatedEvent(AccountCreatedEvent event) {
        
        mailSenderService.sendAccountCreatedConfirmationMail(
                event.getEmail(),
                event.getFirstName(),
                event.getAccountNumber(),
                event.getAccountType()
        );
    }

}