package exceptions;


import java.io.Serializable;

/**
 * This class represents any Exception thrown by a storage cloud driver
 */
public class StorageCloudException extends Exception implements Serializable {

    public static final String UNAVAIL_FUNC = "Not supported by driver.";
    public static final String INVALID_SESSION = "Incorrect session properties.";
    public static final String UNKNOWN_EXCP = "Unknown Exception Thrown. Please report to authors.";
    public static final String LOGIN_ERROR = "Not logged in. Please login.";
    
    public StorageCloudException(String message) {
        super(message);
    }

}
