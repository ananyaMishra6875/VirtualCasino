package virtual.casino.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules of BlackJack, with no printing or input.
 * The player and dealer get two cards each. The player hits or stands,
 * and their turn ends on standing or reaching 21 or more. Then, unless the
 * player went bust, the dealer draws until they have 17 or more.
 * An ace counts as 11 when that doesn't go over 21, otherwise as 1.
 */
public class BlackJack {

    /** Tokens won or lost in one game. */
    public static final int PRIZE = 20;

    public enum Outcome { BUST, WIN, LOSE, PUSH }

    /** How the game ended. */
    public record Result(Outcome outcome, int tokenChange, int playerTotal, int dealerTotal) {}

    private final List<Integer> deck;
    private final List<Integer> playerHand = new ArrayList<>();
    private final List<Integer> dealerHand = new ArrayList<>();
    private Result result = null;

    /** deck should already be shuffled; aces are 1 and face cards are 10. */
    public BlackJack(List<Integer> deck) {
        this.deck = new ArrayList<>(deck);
        //start off by dealing 2 cards to the player and dealer, taking turns
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) {
                playerHand.add(draw());
            }
            else {
                dealerHand.add(draw());
            }
        }
        //a 21 straight away ends the player's turn
        if (handTotal(playerHand) >= 21) {
            finish();
        }
    }

    public List<Integer> playerHand() {
        return Collections.unmodifiableList(playerHand);
    }

    public List<Integer> dealerHand() {
        return Collections.unmodifiableList(dealerHand);
    }

    public boolean isOver() {
        return result != null;
    }

    /** How the game ended, or null while it is still going. */
    public Result result() {
        return result;
    }

    /** Take another card. Reaching 21 or more ends the player's turn. */
    public void hit() {
        checkNotOver();
        playerHand.add(draw());
        if (handTotal(playerHand) >= 21) {
            finish();
        }
    }

    /** End the player's turn. */
    public void stand() {
        checkNotOver();
        finish();
    }

    private void finish() {
        int player = handTotal(playerHand);
        //dealer's turn, only if the player didn't bust
        if (player <= 21) {
            while (handTotal(dealerHand) < 17) {
                dealerHand.add(draw());
            }
        }
        int dealer = handTotal(dealerHand);

        //win/lose tokens based on cards
        Outcome outcome;
        if (player > 21) {
            outcome = Outcome.BUST;
        }
        else if (player > dealer || dealer > 21) {
            outcome = Outcome.WIN;
        }
        else if (player < dealer) {
            outcome = Outcome.LOSE;
        }
        else {
            outcome = Outcome.PUSH;
        }
        int tokenChange = switch (outcome) {
            case WIN -> PRIZE;
            case BUST, LOSE -> -PRIZE;
            case PUSH -> 0;
        };
        result = new Result(outcome, tokenChange, player, dealer);
    }

    private int draw() {
        return deck.remove(0);
    }

    private void checkNotOver() {
        if (isOver()) {
            throw new IllegalStateException("The game is already over");
        }
    }

    /** The total of a hand, counting one ace as 11 if that doesn't go over 21. */
    public static int handTotal(List<Integer> hand) {
        int sum = 0;
        boolean hasAce = false;
        for (int card : hand) {
            sum += card;
            if (card == 1) {
                hasAce = true;
            }
        }
        if (hasAce && sum + 10 <= 21) {
            sum += 10;
        }
        return sum;
    }
}
