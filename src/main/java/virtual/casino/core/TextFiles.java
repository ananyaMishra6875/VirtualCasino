package virtual.casino.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Small helpers for the save files. */
class TextFiles {

    private TextFiles() {}

    /** All lines of the file, or none if it doesn't exist yet. */
    static List<String> readLines(Path file) {
        try {
            return Files.exists(file) ? Files.readAllLines(file) : List.of();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Replace the file's contents. The lines go to a temporary file first,
     * which then takes the real file's place, so a crash halfway through
     * can't leave the file half written.
     */
    static void writeLines(Path file, List<String> lines) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Path temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            Files.write(temp, lines);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }
}
