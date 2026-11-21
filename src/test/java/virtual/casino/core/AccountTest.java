package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void newPlayerGetsTheDefaults() {
        Account a = Account.newPlayer("bob");
        assertEquals("secret", a.password());
        assertEquals(100, a.tokens());
        assertEquals(0, a.debt());
    }

    @Test
    void negativeTokensAreDebt() {
        Account a = new Account("bob", "pw", -20);
        assertEquals(0, a.balance());
        assertEquals(20, a.debt());
        a.addTokens(30);
        assertEquals(10, a.balance());
        assertEquals(0, a.debt());
    }

    @Test
    void validNames() {
        assertTrue(Account.isValidName("ananya"));
        assertTrue(Account.isValidName("M0N3YL0VR"));
        assertFalse(Account.isValidName(null));
        assertFalse(Account.isValidName(""));
        assertFalse(Account.isValidName("two words"));
        assertFalse(Account.isValidName("a|b"));
        assertThrows(IllegalArgumentException.class, () -> Account.newPlayer("two words"));
    }
}
