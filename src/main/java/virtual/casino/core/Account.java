package virtual.casino.core;

/**
 * One player's account: name, password and tokens.
 * Tokens can go below 0; that amount is the player's debt.
 */
public class Account {

    /** What a new player starts with. */
    public static final int STARTING_TOKENS = 100;
    public static final String DEFAULT_PASSWORD = "secret";

    private final String name;
    private String password;
    private int tokens;

    public Account(String name, String password, int tokens) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid username: " + name);
        }
        this.name = name;
        this.password = password;
        this.tokens = tokens;
    }

    /** A player who isn't saved yet: default password and starting tokens. */
    public static Account newPlayer(String name) {
        return new Account(name, DEFAULT_PASSWORD, STARTING_TOKENS);
    }

    /**
     * Names are saved in files separated by spaces and "|",
     * so they can't be empty or contain either.
     */
    public static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && !name.matches(".*[\\s|].*");
    }

    public String name() {
        return name;
    }

    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int tokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public void addTokens(int amount) {
        tokens += amount;
    }

    /** The tokens shown to the player: never below 0. */
    public int balance() {
        return Math.max(0, tokens);
    }

    /** How far below 0 the tokens are, or 0 if not in debt. */
    public int debt() {
        return tokens < 0 ? -tokens : 0;
    }
}
