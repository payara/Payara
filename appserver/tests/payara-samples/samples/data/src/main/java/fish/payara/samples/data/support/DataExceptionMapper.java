package fish.payara.samples.data.support;

import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

/**
 * Maps JPA exceptions to Jakarta Data exceptions for proper TCK compliance.
 */
public final class DataExceptionMapper {
    
    private DataExceptionMapper() {
        // Utility class
    }
    
    /**
     * Maps JPA/persistence exceptions to appropriate Jakarta Data exceptions.
     * 
     * @param ex the original exception
     * @return the mapped Jakarta Data exception or the original exception if no mapping applies
     */
    public static RuntimeException map(RuntimeException ex) {
        if (ex instanceof OptimisticLockException) {
            return new OptimisticLockingFailureException(ex.getMessage(), ex);
        }
        
        if (ex instanceof PersistenceException) {
            // Simple heuristic for "already exists" (PK/unique constraint violations)
            String message = ex.getMessage();
            if (message != null && (message.toLowerCase().contains("unique") || 
                                  message.toLowerCase().contains("duplicate") ||
                                  message.toLowerCase().contains("constraint"))) {
                return new EntityExistsException(ex.getMessage(), ex);
            }
        }
        
        return ex;
    }
}