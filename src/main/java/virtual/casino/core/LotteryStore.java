package virtual.casino.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Saves lottery tickets in a text file (lottery.txt), one per line:
 * name|[n, n, n, n, n]
 * The methods are synchronized so several players can't mix up the file.
 */
public class LotteryStore {

    /** One ticket: who bought it and their 5 numbers. */
    public record Ticket(String name, int[] numbers) {}

    private final Path file;

    public LotteryStore(Path file) {
        this.file = file;
    }

    public synchronized void add(String name, int[] numbers) {
        if (!Account.isValidName(name) || !Lottery.isValidTicket(numbers)) {
            throw new IllegalArgumentException("Invalid ticket: " + name + " " + Arrays.toString(numbers));
        }
        List<String> lines = new ArrayList<>(TextFiles.readLines(file));
        lines.add(name + "|" + Arrays.toString(numbers));
        TextFiles.writeLines(file, lines);
    }

    /** All tickets, in the order they were bought. */
    public synchronized List<Ticket> all() {
        List<Ticket> tickets = new ArrayList<>();
        List<String> lines = TextFiles.readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                String name = line.split("\\|")[0];
                //gets string of lotto numbers between [ and ]
                String lottoList = line.split("\\[")[1].split("\\]")[0];
                String[] parts = lottoList.split(",");
                int[] numbers = new int[parts.length];
                for (int j = 0; j < parts.length; j++) {
                    numbers[j] = Integer.parseInt(parts[j].trim());
                }
                tickets.add(new Ticket(name, numbers));
            }
            catch (RuntimeException e) {
                throw new IllegalStateException("Bad line " + (i + 1) + " in " + file + ": " + line, e);
            }
        }
        return tickets;
    }

    public synchronized void clear() {
        TextFiles.writeLines(file, List.of());
    }
}
