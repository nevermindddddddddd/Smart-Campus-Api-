package uk.ac.westminster.smartcampus.exceptions;

public class LinkedResourceNotFoundException extends RuntimeException {
    private final String fieldName;
    private final String missingId;

    public LinkedResourceNotFoundException(String fieldName, String missingId) {
        super("Referenced " + fieldName + " '" + missingId + "' does not exist.");
        this.fieldName = fieldName;
        this.missingId = missingId;
    }

    public String getFieldName() { return fieldName; }
    public String getMissingId() { return missingId; }
}
