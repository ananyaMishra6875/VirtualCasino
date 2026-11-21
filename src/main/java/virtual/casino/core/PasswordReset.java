package virtual.casino.core;

import java.util.Random;

/**
 * Makes a new random password, with no printing or input.
 * It is 6 characters: the first and last are letters, and the middle four
 * are letters or digits.
 */
public class PasswordReset {

    /** Tokens a password reset costs. */
    public static final int COST = 5;

    public static final int LENGTH = 6;

    public static String generate(Random r) {
        String password = "";
        //generate a random letter for first character
        password = addChar(r, password, 2);
        //generate a random character for the second, third, fourth, fifth letter
        for (int i = 0; i < LENGTH - 2; i++) {
            password = addChar(r, password, 3);
        }
        //generate a random letter for last character
        password = addChar(r, password, 2);
        return password;
    }

    /** num 2 adds a letter, num 3 adds a letter or a digit. */
    private static String addChar(Random r, String password, int num) {
        int passwordChar = r.nextInt(num);
        switch (passwordChar) {
            //randomizes lowercase, uppercase, and numbers based on randomized case
            case 0 -> password = password + (char) (r.nextInt(26) + 'a');
            case 1 -> password = password + (char) (r.nextInt(26) + 'A');
            //a single digit 0-9 (this used to be 1-10, so a 10 made the password too long)
            case 2 -> password = password + r.nextInt(10);
        }
        return password;
    }
}
