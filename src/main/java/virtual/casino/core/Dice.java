package virtual.casino.core;

import java.util.Random;

/**
 * The rules of the dice game (CRAPS), with no printing or input.
 * The player guesses a total from 4-12, then two dice are rolled.
 * If the first roll is a 2 or 3, the dice are rolled once more.
 * A correct guess wins the prize and a wrong one loses it.
 */
public class Dice {

    /** Tokens won or lost in one game. */
    public static final int PRIZE = 10;

    private final Random r;

    public Dice(Random r) {
        this.r = r;
    }

    /** One roll of two dice. */
    public record Roll(int die1, int die2) {
        public int total() {
            return die1 + die2;
        }
    }

    /**
     * What happened in one game.
     * reroll is null when the first roll was not a 2 or 3.
     */
    public record Result(int guess, Roll firstRoll, Roll reroll, boolean won, int tokenChange) {
        /** The roll that decided the game. */
        public Roll finalRoll() {
            return reroll != null ? reroll : firstRoll;
        }
    }

    public Roll roll() {
        return new Roll(r.nextInt(6) + 1, r.nextInt(6) + 1);
    }

    public Result play(int guess) {
        Roll firstRoll = roll();
        Roll reroll = null;
        if (firstRoll.total() == 2 || firstRoll.total() == 3) {
            reroll = roll();
        }
        int finalTotal = (reroll != null ? reroll : firstRoll).total();
        boolean won = guess == finalTotal;
        return new Result(guess, firstRoll, reroll, won, won ? PRIZE : -PRIZE);
    }
}
