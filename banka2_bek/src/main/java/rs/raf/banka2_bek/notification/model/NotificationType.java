package rs.raf.banka2_bek.notification.model;

public enum NotificationType {

    PAYMENT(true),
    TRANSFER(true),
    LIMIT_CHANGE(true),
    CARD_BLOCKED(true),
    CARD_UNBLOCKED(true),
    LOAN_CREATED(true),
    LOAN_APPROVED(true),
    LOAN_REJECTED(true),
    ORDER_PENDING(false),
    ORDER_APPROVED(false),
    ORDER_DECLINED(false),
    ORDER_EXECUTED(false),
    ORDER_PARTIAL_FILL(false),
    ORDER_CANCELLED(false),
    OTC_COUNTER_OFFER(false),
    OTC_ACCEPTED(false),
    OTC_DECLINED(false),
    OTC_CONTRACT_EXPIRING(false),
    ACCOUNT_LOCKED(true),
    GENERAL(false);

    private final boolean sendsEmail;

    NotificationType(boolean sendsEmail) {
        this.sendsEmail = sendsEmail;
    }

    public boolean isSendsEmail() {
        return sendsEmail;
    }
}
