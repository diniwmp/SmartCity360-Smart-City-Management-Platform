// ---- exception/PolicyParseException.java
package com.urbantech.smartcity360.exception;

/** Rule එක (DSL text) grammar එකට නොගැලපෙන විට: වැරැද්ද සහ එය තිබෙන තැන. */
public class PolicyParseException extends Exception {

    private final int position;     // 1-based character position; 0 = නොදනී

    public PolicyParseException(String message) {
        super(message);
        this.position = 0;
    }

    public PolicyParseException(String message, int zeroBasedPosition) {
        super(message + " (at character " + (zeroBasedPosition + 1) + ")");
        this.position = zeroBasedPosition + 1;
    }

    public int getPosition() {
        return position;
    }
}