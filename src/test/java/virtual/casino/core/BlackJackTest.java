package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import virtual.casino.core.BlackJack.Outcome;

class BlackJackTest {

    // decks are dealt in order: player, dealer, player, dealer, then draws

    @Test
    void dealsTwoCardsEachTakingTurns() {
        BlackJack game = new BlackJack(List.of(10, 9, 7, 8, 5));
        assertEquals(List.of(10, 7), game.playerHand());
        assertEquals(List.of(9, 8), game.dealerHand());
        assertFalse(game.isOver());
    }

    @Test
    void higherTotalWins() {
        BlackJack game = new BlackJack(List.of(10, 10, 9, 7));
        game.stand();
        assertEquals(Outcome.WIN, game.result().outcome());
        assertEquals(20, game.result().tokenChange());
    }

    @Test
    void lowerTotalLoses() {
        BlackJack game = new BlackJack(List.of(10, 10, 7, 9));
        game.stand();
        assertEquals(Outcome.LOSE, game.result().outcome());
        assertEquals(-20, game.result().tokenChange());
    }

    @Test
    void sameTotalIsAPush() {
        BlackJack game = new BlackJack(List.of(10, 9, 7, 8));
        game.stand();
        assertEquals(Outcome.PUSH, game.result().outcome());
        assertEquals(0, game.result().tokenChange());
    }

    @Test
    void goingOver21IsABust() {
        BlackJack game = new BlackJack(List.of(10, 9, 6, 8, 10));
        game.hit();
        assertTrue(game.isOver());
        assertEquals(Outcome.BUST, game.result().outcome());
        assertEquals(-20, game.result().tokenChange());
    }

    @Test
    void dealerDoesNotDrawAfterPlayerBusts() {
        BlackJack game = new BlackJack(List.of(10, 5, 6, 6, 10, 10));
        game.hit();
        assertEquals(List.of(5, 6), game.dealerHand());
    }

    @Test
    void dealerDrawsUntil17() {
        BlackJack game = new BlackJack(List.of(10, 9, 8, 2, 5, 4));
        game.stand();
        assertEquals(List.of(9, 2, 5, 4), game.dealerHand());
        assertEquals(20, game.result().dealerTotal());
        assertEquals(Outcome.LOSE, game.result().outcome());
    }

    @Test
    void dealerBustMeansYouWin() {
        BlackJack game = new BlackJack(List.of(10, 9, 8, 6, 10));
        game.stand();
        assertEquals(25, game.result().dealerTotal());
        assertEquals(Outcome.WIN, game.result().outcome());
    }

    @Test
    void reaching21EndsYourTurn() {
        BlackJack game = new BlackJack(List.of(5, 10, 6, 8, 10));
        game.hit();
        assertTrue(game.isOver());
        assertEquals(21, game.result().playerTotal());
    }

    @Test
    void dealtBlackjackEndsYourTurnStraightAway() {
        BlackJack game = new BlackJack(List.of(1, 9, 10, 8));
        assertTrue(game.isOver());
        assertEquals(21, game.result().playerTotal());
        assertEquals(Outcome.WIN, game.result().outcome());
    }

    @Test
    void cannotPlayAfterTheGameIsOver() {
        BlackJack game = new BlackJack(List.of(10, 9, 7, 8));
        game.stand();
        assertThrows(IllegalStateException.class, game::hit);
        assertThrows(IllegalStateException.class, game::stand);
    }

    @Test
    void aceCountsAs11WhenItFits() {
        assertEquals(21, BlackJack.handTotal(List.of(1, 10)));
        assertEquals(17, BlackJack.handTotal(List.of(1, 6)));
        assertEquals(12, BlackJack.handTotal(List.of(1, 1)));
        assertEquals(21, BlackJack.handTotal(List.of(1, 1, 9)));
    }

    @Test
    void aceCountsAs1WhenItWouldBust() {
        assertEquals(16, BlackJack.handTotal(List.of(1, 5, 10)));
        assertEquals(17, BlackJack.handTotal(List.of(10, 7)));
    }
}
