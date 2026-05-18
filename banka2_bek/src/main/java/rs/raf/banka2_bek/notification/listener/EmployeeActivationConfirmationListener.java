package rs.raf.banka2_bek.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.employee.event.EmployeeActivationConfirmationEvent;
import rs.raf.banka2_bek.notification.service.MailSenderService;

@Component
@RequiredArgsConstructor
public class EmployeeActivationConfirmationListener {

    private final MailSenderService mailSenderService;


    @Async
    @EventListener
    public void onEmployeeActivationConfirmationEvent(EmployeeActivationConfirmationEvent event) {
        mailSenderService.sendActivationConfirmationMail(
                event.getEmail(),
                event.getFirstName()
        );
    }
}
