package com.rememberber.mootool.nextfx.domain.regex;

public final class RegexException extends RuntimeException {

    private final String messageKey;
    private final String details;

    public RegexException(String messageKey, String details) {
        super(details == null || details.isBlank() ? messageKey : details);
        this.messageKey = messageKey;
        this.details = details == null ? "" : details;
    }

    public String messageKey() {
        return messageKey;
    }

    public String details() {
        return details;
    }
}
