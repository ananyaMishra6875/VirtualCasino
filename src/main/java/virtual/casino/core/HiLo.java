package virtual.casino.core;

import java.util.List;

/**
 * The rules of Hi-Lo, with no printing or input.
 * A card is shown and the player guesses if the next one is lower or higher.
 * On a tie the player guesses again, and each tie raises the prize:
 * a right guess after n ties wins PRIZE * n * 2 (just PRIZE with no ties).
 * A wrong guess loses PRIZE.
 */
public class HiLo {

    /** Tokens won or lost in a game with no ties. */
    public static final int PRIZE = 15;

    public enum Guess { LOWER, HIGHER }

    public enum Outcome { WIN, LOSE, TIE }

    /** What happened after one guess. tokenChange is 0 on a tie, since the game goes on. */
    public record Turn(int card, int nextCard, Guess guess, Outcome outcome, int ties, int tokenChange) {}

    private final List<Integer> deck;
    private int cardIndex = 0;
    private int ties = 0;
    private boolean over = false;

    /** deck should already be shuffled; cards are 1-13 (A, 2-10, J, Q, K). */
    public HiLo(List<Integer> deck) {
        this.deck = deck;
    }

    public int currentCard() {
        return deck.get(cardIndex);
    }

    public int ties() {
        return ties;
    }

    public boolean isOver() {
        return over;
    }

    /** Make a guess. A null guess (not lower or higher) counts as wrong. */
    public Turn guess(Guess guess) {
        if (over) {
            throw new IllegalStateException("The game is already over");
        }
        int card = deck.get(cardIndex);
        int nextCard = deck.get(cardIndex + 1);
        cardIndex++;

        //if new card is the same as the old one, guess again
        if (nextCard == card) {
            ties++;
            return new Turn(card, nextCard, guess, Outcome.TIE, ties, 0);
        }

        Guess correct = (nextCard < card) ? Guess.LOWER : Guess.HIGHER;
        boolean win = guess == correct;
        over = true;
        int tokenChange;
        if (!win) {
            tokenChange = -PRIZE;
        }
        else if (ties > 0) {
            tokenChange = PRIZE * ties * 2;
        }
        else {
            tokenChange = PRIZE;
        }
        return new Turn(card, nextCard, guess, win ? Outcome.WIN : Outcome.LOSE, ties, tokenChange);
    }

    /** The name shown for a card: A, 2-10, J, Q or K. */
    public static String cardName(int card) {
        return switch (card) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(card);
        };
    }
}
