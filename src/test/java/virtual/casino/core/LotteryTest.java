package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class LotteryTest {

    private static final int[] DRAWN = {1, 30, 4, 23, 26};

    @Test
    void countsMatchesInAnyOrder() {
        assertEquals(5, Lottery.matches(new int[] {26, 23, 4, 30, 1}, DRAWN));
        assertEquals(3, Lottery.matches(new int[] {1, 4, 26, 2, 3}, DRAWN));
        assertEquals(0, Lottery.matches(new int[] {2, 3, 5, 6, 7}, DRAWN));
    }

    @Test
    void prizes() {
        assertEquals(0, Lottery.prize(0));
        assertEquals(0, Lottery.prize(2));
        assertEquals(5, Lottery.prize(3));
        assertEquals(2000, Lottery.prize(4));
        assertEquals(10000, Lottery.prize(5));
    }

    @Test
    void randomNumbersAreFiveDifferentNumbersFrom1To30() {
        Random r = new Random(5);
        for (int i = 0; i < 1000; i++) {
            assertTrue(Lottery.isValidTicket(Lottery.randomNumbers(r)));
        }
    }

    @Test
    void validTickets() {
        assertTrue(Lottery.isValidTicket(new int[] {1, 2, 3, 4, 30}));
        assertFalse(Lottery.isValidTicket(new int[] {1, 2, 3, 4}), "too few");
        assertFalse(Lottery.isValidTicket(new int[] {1, 2, 3, 4, 5, 6}), "too many");
        assertFalse(Lottery.isValidTicket(new int[] {1, 2, 3, 4, 4}), "repeated");
        assertFalse(Lottery.isValidTicket(new int[] {0, 2, 3, 4, 5}), "below 1");
        assertFalse(Lottery.isValidTicket(new int[] {1, 2, 3, 4, 31}), "above 30");
        assertFalse(Lottery.isValidTicket(null));
    }

    @Test
    void numberExists() {
        assertTrue(Lottery.numberExists(4, DRAWN));
        assertFalse(Lottery.numberExists(5, DRAWN));
    }
}
