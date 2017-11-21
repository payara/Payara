// Portions Copyright [2016] [Payara Foundation]

package org.glassfish.security.common;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;


/**
 * Provides common functionality for protecting files.
 *
 * @author Phillip Ross
 */
public class FileProtectionUtility {


    /**
     * Set permissions on the specified file equivalent to file mode 0600.
     *
     * NOTE: This method will only set permissions on files that exist on filesystems which support POSIX file
     * permissions.  Manipulation of permissions is silently skipped for filesystems that do not support POSIX file
     * permissions (such as Windows NTFS).
     *
     * @param file The file to set permissions on
     * @throws IOException if an I/O error occurs
     */
    public static void chmod0600(File file) throws IOException {
        Path filePath = file.toPath();
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(filePath, PosixFileAttributeView.class);
        if (posixFileAttributeView != null) {
            Files.setPosixFilePermissions(filePath,
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            );
        }
    }


}
