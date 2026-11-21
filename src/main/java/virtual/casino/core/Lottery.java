package virtual.casino.core;

import java.util.Random;

/**
 * The rules of the lottery, with no printing, input or files.
 * A ticket is 5 different numbers from 1-30. When the manager draws,
 * each ticket wins based on how many of its numbers were drawn:
 * 3 matches win 5 tokens, 4 win 2000 and 5 win 10000.
 */
public class Lottery {

    /** Tokens a ticket costs. */
    public static final int TICKET_COST = 5;

    /** How many numbers are on a ticket, and the range they come from. */
    public static final int NUMBERS = 5;
    public static final int MIN = 1;
    public static final int MAX = 30;

    public static boolean inRange(int number) {
        return number >= MIN && number <= MAX;
    }

    /** Is number already in numList? */
    public static boolean numberExists(int number, int[] numList) {
        for (int num : numList) {
            if (number == num) {
                return true;
            }
        }
        return false;
    }

    /** True if the ticket has exactly 5 different numbers, all from 1-30. */
    public static boolean isValidTicket(int[] ticket) {
        if (ticket == null || ticket.length != NUMBERS) {
            return false;
        }
        for (int i = 0; i < ticket.length; i++) {
            if (!inRange(ticket[i])) {
                return false;
            }
            for (int j = 0; j < i; j++) {
                if (ticket[i] == ticket[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Randomly picks 5 different numbers from 1-30. */
    public static int[] randomNumbers(Random r) {
        int[] lotteryNums = new int[NUMBERS];
        for (int i = 0; i < NUMBERS; i++) {
            int randNum;
            //repeats until distinct number is found
            do {
                randNum = r.nextInt(MAX - MIN + 1) + MIN;
            } while (numberExists(randNum, lotteryNums));
            lotteryNums[i] = randNum;
        }
        return lotteryNums;
    }

    /** How many of the ticket's numbers were drawn. */
    public static int matches(int[] ticket, int[] drawn) {
        int count = 0;
        for (int num : ticket) {
            if (numberExists(num, drawn)) {
                count++;
            }
        }
        return count;
    }

    /** Tokens won for a number of matches. */
    public static int prize(int matches) {
        return switch (matches) {
            case 3 -> 5;
            case 4 -> 2000;
            case 5 -> 10000;
            default -> 0;
        };
    }
}
