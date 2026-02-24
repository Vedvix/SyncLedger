package com.vedvix.syncledger.notification.enums;

public enum NotificationPriority {
    LOW(3), MEDIUM(2), HIGH(1), CRITICAL(0);

    private final int level;

    NotificationPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}