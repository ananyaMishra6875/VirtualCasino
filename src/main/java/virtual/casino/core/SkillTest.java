package virtual.casino.core;

import java.util.Random;

/**
 * The skill testing question needed to cash out, with no printing or input.
 * The question is a + b * c with each number from 1-15. The player gets
 * 3 tries, and each wrong answer brings a new question.
 */
public class SkillTest {

    public static final int TRIES = 3;

    /** One question: a + b * c. */
    public record Question(int a, int b, int c) {
        public int answer() {
            return a + b * c;
        }

        /** The question as shown to the player, like "2 + 3 * 4". */
        public String text() {
            return a + " + " + b + " * " + c;
        }
    }

    private final Random r;
    private Question question;
    private int triesLeft = TRIES;
    private boolean passed = false;

    public SkillTest(Random r) {
        this.r = r;
        this.question = newQuestion();
    }

    public Question question() {
        return question;
    }

    public int triesLeft() {
        return triesLeft;
    }

    public boolean passed() {
        return passed;
    }

    public boolean failed() {
        return triesLeft == 0;
    }

    /** Answer the current question. Returns true if it was right. */
    public boolean answer(int userAnswer) {
        if (passed || failed()) {
            throw new IllegalStateException("The skill test is already finished");
        }
        if (userAnswer == question.answer()) {
            passed = true;
            return true;
        }
        triesLeft--;
        if (!failed()) {
            question = newQuestion();
        }
        return false;
    }

    private Question newQuestion() {
        return new Question(r.nextInt(15) + 1, r.nextInt(15) + 1, r.nextInt(15) + 1);
    }
}
