package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;
import static virtual.casino.core.HiLo.Guess.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import virtual.casino.core.HiLo.Outcome;

class HiLoTest {

    @Test
    void rightGuessWinsThePrize() {
        HiLo game = new HiLo(List.of(5, 9));
        assertEquals(5, game.currentCard());
        HiLo.Turn turn = game.guess(HIGHER);
        assertEquals(Outcome.WIN, turn.outcome());
        assertEquals(9, turn.nextCard());
        assertEquals(15, turn.tokenChange());
        assertTrue(game.isOver());
    }

    @Test
    void wrongGuessLosesThePrize() {
        HiLo.Turn turn = new HiLo(List.of(5, 2)).guess(HIGHER);
        assertEquals(Outcome.LOSE, turn.outcome());
        assertEquals(-15, turn.tokenChange());
    }

    @Test
    void tieLetsYouGuessAgain() {
        HiLo game = new HiLo(List.of(5, 5, 9));
        HiLo.Turn tie = game.guess(LOWER);
        assertEquals(Outcome.TIE, tie.outcome());
        assertEquals(0, tie.tokenChange());
        assertFalse(game.isOver());
        assertEquals(5, game.currentCard());
    }

    @Test
    void rightGuessAfterATieDoublesThePrize() {
        HiLo game = new HiLo(List.of(5, 5, 9));
        game.guess(HIGHER);
        HiLo.Turn turn = game.guess(HIGHER);
        assertEquals(Outcome.WIN, turn.outcome());
        assertEquals(30, turn.tokenChange());
    }

    @Test
    void guessAfterATieIsCheckedNotIgnored() {
        // this was the bug: after a tie the guess never mattered
        HiLo game = new HiLo(List.of(5, 5, 9));
        game.guess(HIGHER);
        HiLo.Turn turn = game.guess(LOWER);
        assertEquals(Outcome.LOSE, turn.outcome());
        assertEquals(-15, turn.tokenChange());
    }

    @Test
    void twoTiesWinFourTimesThePrize() {
        HiLo game = new HiLo(List.of(5, 5, 5, 2));
        game.guess(LOWER);
        game.guess(LOWER);
        HiLo.Turn turn = game.guess(LOWER);
        assertEquals(2, turn.ties());
        assertEquals(60, turn.tokenChange());
    }

    @Test
    void invalidGuessLoses() {
        HiLo.Turn turn = new HiLo(List.of(5, 9)).guess(null);
        assertEquals(Outcome.LOSE, turn.outcome());
    }

    @Test
    void cannotGuessAfterTheGameIsOver() {
        HiLo game = new HiLo(List.of(5, 9, 3));
        game.guess(HIGHER);
        assertThrows(IllegalStateException.class, () -> game.guess(LOWER));
    }

    @Test
    void cardNames() {
        assertEquals("A", HiLo.cardName(1));
        assertEquals("7", HiLo.cardName(7));
        assertEquals("J", HiLo.cardName(11));
        assertEquals("Q", HiLo.cardName(12));
        assertEquals("K", HiLo.cardName(13));
    }
}
