package rs.raf.banka2_bek.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.banka2_bek.auth.util.UserContext;
import rs.raf.banka2_bek.auth.util.UserResolver;
import rs.raf.banka2_bek.notification.dto.NotificationDto;
import rs.raf.banka2_bek.notification.service.NotificationService;

import java.util.Map;

/**
 * REST endpoints over the authenticated user's own in-app notifications. The
 * principal is resolved via {@link UserResolver}; ownership of an individual
 * notification is enforced by {@link NotificationService}.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean onlyUnread) {
        UserContext user = userResolver.resolveCurrent();
        Page<NotificationDto> notifications = notificationService.getMyNotifications(
                user.userId(), user.userRole(), Boolean.TRUE.equals(onlyUnread), page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UserContext user = userResolver.resolveCurrent();
        Long count = notificationService.getUnreadCount(user.userId(), user.userRole());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markOneRead(@PathVariable Long id) {
        UserContext user = userResolver.resolveCurrent();
        return ResponseEntity.ok(notificationService.markOneRead(id, user.userId(), user.userRole()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        UserContext user = userResolver.resolveCurrent();
        notificationService.markAllRead(user.userId(), user.userRole());
        return ResponseEntity.noContent().build();
    }
}
