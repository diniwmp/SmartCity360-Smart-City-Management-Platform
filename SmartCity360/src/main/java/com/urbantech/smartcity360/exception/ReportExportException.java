// ---- exception/ReportExportException.java
package com.urbantech.smartcity360.exception;

/** Report එක PDF/CSV බවට හැරවීම අසාර්ථක වූ විට. */
public class ReportExportException extends RuntimeException {
    public ReportExportException(String message, Throwable cause) {
        super(message, cause);
    }
}