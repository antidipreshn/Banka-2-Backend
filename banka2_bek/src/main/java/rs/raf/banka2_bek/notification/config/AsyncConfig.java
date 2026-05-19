package rs.raf.banka2_bek.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * [B1] Enables Spring's {@code @Async} support, which allows
 * {@link rs.raf.banka2_bek.notification.listener.InAppNotificationEventListener}
 * to send emails off the caller's thread after the notification transaction commits.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

}
