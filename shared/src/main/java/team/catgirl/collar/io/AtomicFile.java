package team.catgirl.collar.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Atomically write to a file
 */
public final class AtomicFile {
    /**
     * The original file
     */
    public final File file;

    /**
     * @param file to write atomically to
     */
    private AtomicFile(File file) {
        this.file = file;
    }

    /**
     * Writes to a temporary file then moves it to the original file's location
     * @param writeAction consumes the temporary file
     * @throws IOException if error occurs
     */
    public void write(WriteAction writeAction) throws IOException {
        File tempFile = new File(file.getAbsolutePath() + ".tmp");
        if (tempFile.exists()) {
            if (!tempFile.delete()) {
                throw new IllegalStateException("could not delete old temporary file at " + tempFile);
            }
        }
        if (!tempFile.createNewFile()) {
            throw new IllegalStateException("could not create temporary file at " + tempFile);
        }
        try {
            writeAction.write(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (Files.exists(tempFile.toPath())) {
                Files.delete(tempFile.toPath());
            }
        }
    }

    /**
     * Write atomically to the file
     * @param file to write to
     * @param writeAction performing the write
     * @throws IOException if an error occurs
     */
    public static void write(File file, WriteAction writeAction) throws IOException {
        new AtomicFile(file).write(writeAction);
    }

    @FunctionalInterface
    public interface WriteAction {
        /**
         * Perform the write action
         * @param file to write to
         * @throws IOException
         */
        void write(File file) throws IOException;
    }
}
