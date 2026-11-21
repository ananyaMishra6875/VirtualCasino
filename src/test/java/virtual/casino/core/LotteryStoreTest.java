package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LotteryStoreTest {

    @TempDir
    Path dir;

    Path file;
    LotteryStore tickets;

    @BeforeEach
    void setUp() {
        file = dir.resolve("lottery.txt");
        tickets = new LotteryStore(file);
    }

    @Test
    void missingFileMeansNoTickets() {
        assertTrue(tickets.all().isEmpty());
    }

    @Test
    void savesInTheOldFormat() throws Exception {
        tickets.add("ananya", new int[] {1, 30, 4, 23, 26});
        tickets.add("bob", new int[] {2, 3, 5, 7, 11});
        assertEquals(List.of("ananya|[1, 30, 4, 23, 26]", "bob|[2, 3, 5, 7, 11]"), Files.readAllLines(file));
    }

    @Test
    void readsTheOldFormat() throws Exception {
        Files.write(file, List.of("ananya|[1, 30, 4, 23, 26]"));
        List<LotteryStore.Ticket> all = tickets.all();
        assertEquals(1, all.size());
        assertEquals("ananya", all.get(0).name());
        assertArrayEquals(new int[] {1, 30, 4, 23, 26}, all.get(0).numbers());
    }

    @Test
    void clearRemovesEveryTicket() {
        tickets.add("ananya", new int[] {1, 2, 3, 4, 5});
        tickets.clear();
        assertTrue(tickets.all().isEmpty());
    }

    @Test
    void invalidTicketsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> tickets.add("ananya", new int[] {0, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> tickets.add("two words", new int[] {1, 2, 3, 4, 5}));
    }
}
