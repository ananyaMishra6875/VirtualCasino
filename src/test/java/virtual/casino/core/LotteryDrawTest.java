package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LotteryDrawTest {

    private static final int[] DRAWN = {1, 2, 3, 4, 5};

    @TempDir
    Path dir;

    UserStore users;
    LotteryStore tickets;

    @BeforeEach
    void setUp() {
        users = new UserStore(dir.resolve("userInfo.txt"));
        tickets = new LotteryStore(dir.resolve("lottery.txt"));
    }

    @Test
    void paysEachTicketAndClearsThem() {
        users.save(new Account("ananya", "pw", 10));
        users.save(new Account("bob", "pw", 10));
        tickets.add("ananya", new int[] {1, 2, 3, 4, 30});  // 4 matches
        tickets.add("bob", new int[] {1, 2, 3, 29, 30});    // 3 matches
        tickets.add("bob", new int[] {26, 27, 28, 29, 30}); // 0 matches

        List<LotteryDraw.TicketResult> results = LotteryDraw.payOut(DRAWN, tickets, users);

        assertEquals(3, results.size());
        assertEquals(4, results.get(0).matches());
        assertEquals(2000, results.get(0).prize());
        assertEquals(0, results.get(2).prize());
        assertEquals(2010, users.find("ananya").orElseThrow().tokens());
        assertEquals(15, users.find("bob").orElseThrow().tokens());
        assertTrue(tickets.all().isEmpty());
    }

    @Test
    void winnerNotInTheUserFileDoesNotWipeIt() {
        // this was a bug: the draw wiped userInfo.txt in this case
        users.save(new Account("ananya", "pw", 10));
        tickets.add("ghost", new int[] {1, 2, 3, 4, 5});

        LotteryDraw.payOut(DRAWN, tickets, users);

        assertEquals(10, users.find("ananya").orElseThrow().tokens());
        assertEquals(10100, users.find("ghost").orElseThrow().tokens());
    }

    @Test
    void noTicketsMeansNoChanges() {
        users.save(new Account("ananya", "pw", 10));
        assertTrue(LotteryDraw.payOut(DRAWN, tickets, users).isEmpty());
        assertEquals(10, users.find("ananya").orElseThrow().tokens());
    }
}
