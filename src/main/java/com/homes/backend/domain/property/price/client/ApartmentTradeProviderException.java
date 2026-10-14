package com.homes.backend.domain.property.price.client;

public class ApartmentTradeProviderException extends RuntimeException {
    public ApartmentTradeProviderException(String message) {
        super(message);
    }

    public ApartmentTradeProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
