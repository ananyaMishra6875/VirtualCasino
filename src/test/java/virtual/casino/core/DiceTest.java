package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DiceTest {

    /** A Random that rolls the dice values we give it, in order. */
    private static Random rolls(int... dieValues) {
        return new Random() {
            int i = 0;

            @Override
            public int nextInt(int bound) {
                return dieValues[i++] - 1;
            }
        };
    }

    @Test
    void correctGuessWinsThePrize() {
        Dice.Result result = new Dice(rolls(3, 4)).play(7);
        assertTrue(result.won());
        assertEquals(10, result.tokenChange());
        assertNull(result.reroll());
        assertEquals(7, result.finalRoll().total());
    }

    @Test
    void wrongGuessLosesThePrize() {
        Dice.Result result = new Dice(rolls(6, 6)).play(7);
        assertFalse(result.won());
        assertEquals(-10, result.tokenChange());
    }

    @Test
    void firstRollOfTwoOrThreeIsRolledAgain() {
        Dice.Result result = new Dice(rolls(1, 2, 3, 4)).play(7);
        assertEquals(3, result.firstRoll().total());
        assertNotNull(result.reroll());
        assertEquals(7, result.finalRoll().total());
        assertTrue(result.won());
    }

    @Test
    void onlyTheRerollCounts() {
        // the first roll matches the guess, but a 3 is always rolled again
        Dice.Result result = new Dice(rolls(1, 2, 5, 5)).play(3);
        assertFalse(result.won());
        assertEquals(-10, result.tokenChange());
    }

    @Test
    void diceAlwaysLandOneToSix() {
        Dice dice = new Dice(new Random(42));
        for (int i = 0; i < 1000; i++) {
            Dice.Roll roll = dice.roll();
            assertTrue(roll.die1() >= 1 && roll.die1() <= 6);
            assertTrue(roll.die2() >= 1 && roll.die2() <= 6);
        }
    }
}
