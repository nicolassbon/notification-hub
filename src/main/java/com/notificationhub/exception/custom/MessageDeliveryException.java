package com.notificationhub.exception.custom;

public class MessageDeliveryException extends RuntimeException {
    public MessageDeliveryException(String message) {
        super(message);
    }
}
