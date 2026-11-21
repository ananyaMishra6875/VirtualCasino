package virtual.casino.core;

import java.util.Optional;

/**
 * Logging in, with no printing or input.
 * A saved player needs their password; a new player uses the default
 * password ("secret") and starts with 100 tokens. Either way they get
 * 3 tries. The casino manager logs in by name alone.
 */
public class Login {

    public static final String MANAGER = "M0N3YL0VR";

    public static final int TRIES = 3;

    private final Account account;
    private final boolean newPlayer;
    private int triesLeft = TRIES;
    private boolean passed = false;

    public Login(UserStore users, String name) {
        if (!Account.isValidName(name)) {
            throw new IllegalArgumentException("Invalid username: " + name);
        }
        Optional<Account> saved = users.find(name);
        newPlayer = saved.isEmpty();
        account = saved.orElseGet(() -> Account.newPlayer(name));
    }

    /** The manager doesn't need a password. */
    public static boolean isManager(String name) {
        return MANAGER.equals(name);
    }

    /** True if this name isn't saved yet. */
    public boolean isNewPlayer() {
        return newPlayer;
    }

    /** Try a password. Returns true if it was right. */
    public boolean tryPassword(String password) {
        if (passed || failed()) {
            throw new IllegalStateException("The login is already finished");
        }
        if (account.password().equals(password)) {
            passed = true;
            return true;
        }
        triesLeft--;
        return false;
    }

    public boolean passed() {
        return passed;
    }

    public boolean failed() {
        return triesLeft == 0;
    }

    public int triesLeft() {
        return triesLeft;
    }

    /** The logged in player's account. Only once the password was right. */
    public Account account() {
        if (!passed) {
            throw new IllegalStateException("Not logged in");
        }
        return account;
    }
}
