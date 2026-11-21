package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SkillTestTest {

    /** A Random that gives the question numbers we want, in order. */
    private static Random numbers(int... values) {
        return new Random() {
            int i = 0;

            @Override
            public int nextInt(int bound) {
                return values[i++] - 1;
            }
        };
    }

    @Test
    void answerIsAPlusBTimesC() {
        SkillTest.Question q = new SkillTest.Question(2, 3, 4);
        assertEquals(14, q.answer());
        assertEquals("2 + 3 * 4", q.text());
    }

    @Test
    void rightAnswerPasses() {
        SkillTest test = new SkillTest(numbers(2, 3, 4));
        assertTrue(test.answer(14));
        assertTrue(test.passed());
        assertFalse(test.failed());
    }

    @Test
    void wrongAnswerGivesANewQuestion() {
        SkillTest test = new SkillTest(numbers(2, 3, 4, 5, 5, 5));
        assertFalse(test.answer(20));
        assertEquals(new SkillTest.Question(5, 5, 5), test.question());
        assertEquals(2, test.triesLeft());
        assertFalse(test.passed());
        assertFalse(test.failed());
    }

    @Test
    void rightOnTheLastTryStillPasses() {
        SkillTest test = new SkillTest(numbers(1, 1, 1, 1, 1, 1, 2, 3, 4));
        test.answer(0);
        test.answer(0);
        assertTrue(test.answer(14));
        assertTrue(test.passed());
    }

    @Test
    void threeWrongAnswersFail() {
        SkillTest test = new SkillTest(numbers(1, 1, 1, 1, 1, 1, 1, 1, 1));
        test.answer(0);
        test.answer(0);
        test.answer(0);
        assertTrue(test.failed());
        assertFalse(test.passed());
        assertEquals(0, test.triesLeft());
    }

    @Test
    void cannotAnswerAfterFinishing() {
        SkillTest test = new SkillTest(numbers(2, 3, 4));
        test.answer(14);
        assertThrows(IllegalStateException.class, () -> test.answer(14));
    }

    @Test
    void numbersAreOneToFifteen() {
        Random r = new Random(4);
        for (int i = 0; i < 1000; i++) {
            SkillTest.Question q = new SkillTest(r).question();
            for (int n : new int[] {q.a(), q.b(), q.c()}) {
                assertTrue(n >= 1 && n <= 15);
            }
        }
    }
}
