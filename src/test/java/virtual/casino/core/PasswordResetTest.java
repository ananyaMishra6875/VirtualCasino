package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PasswordResetTest {

    @Test
    void passwordIsAlwaysSixCharacters() {
        // this was the bug: a digit of 10 made the password 7 characters
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            String password = PasswordReset.generate(r);
            assertEquals(6, password.length(), password);
        }
    }

    @Test
    void firstAndLastAreLettersAndTheRestAreLettersOrDigits() {
        Random r = new Random(2);
        for (int i = 0; i < 10_000; i++) {
            String password = PasswordReset.generate(r);
            assertTrue(Character.isLetter(password.charAt(0)), password);
            assertTrue(Character.isLetter(password.charAt(5)), password);
            assertTrue(password.matches("[a-zA-Z0-9]{6}"), password);
        }
    }

    @Test
    void digitsCanBeZeroToNine() {
        Random r = new Random(3);
        String all = "";
        for (int i = 0; i < 10_000; i++) {
            all += PasswordReset.generate(r);
        }
        for (char digit = '0'; digit <= '9'; digit++) {
            assertTrue(all.indexOf(digit) >= 0, "never saw " + digit);
        }
    }
}
