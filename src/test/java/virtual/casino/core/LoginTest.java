package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoginTest {

    @TempDir
    Path dir;

    UserStore users;

    @BeforeEach
    void setUp() {
        users = new UserStore(dir.resolve("userInfo.txt"));
        users.save(new Account("ananya", "myPass", 250));
    }

    @Test
    void managerNeedsNoPassword() {
        assertTrue(Login.isManager("M0N3YL0VR"));
        assertFalse(Login.isManager("ananya"));
        assertFalse(Login.isManager(null));
    }

    @Test
    void savedPlayerLogsInWithTheirPassword() {
        Login login = new Login(users, "ananya");
        assertFalse(login.isNewPlayer());
        assertTrue(login.tryPassword("myPass"));
        assertEquals(250, login.account().tokens());
    }

    @Test
    void newPlayerUsesTheDefaultPassword() {
        Login login = new Login(users, "bob");
        assertTrue(login.isNewPlayer());
        assertFalse(login.tryPassword("myPass"));
        assertTrue(login.tryPassword("secret"));
        assertEquals(100, login.account().tokens());
    }

    @Test
    void rightOnTheThirdTryStillWorks() {
        Login login = new Login(users, "ananya");
        login.tryPassword("no");
        login.tryPassword("nope");
        assertTrue(login.tryPassword("myPass"));
        assertTrue(login.passed());
    }

    @Test
    void threeWrongTriesFail() {
        Login login = new Login(users, "ananya");
        login.tryPassword("a");
        login.tryPassword("b");
        login.tryPassword(null);
        assertTrue(login.failed());
        assertThrows(IllegalStateException.class, login::account);
        assertThrows(IllegalStateException.class, () -> login.tryPassword("myPass"));
    }

    @Test
    void invalidNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Login(users, "two words"));
        assertThrows(IllegalArgumentException.class, () -> new Login(users, null));
    }
}
