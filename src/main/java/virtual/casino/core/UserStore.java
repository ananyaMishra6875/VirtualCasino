package virtual.casino.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Saves accounts in a text file (userInfo.txt), one per line:
 * name password tokens debt
 * The methods are synchronized so several players can't mix up the file.
 */
public class UserStore {

    private final Path file;

    public UserStore(Path file) {
        this.file = file;
    }

    /** The saved account with this name, if there is one. */
    public synchronized Optional<Account> find(String name) {
        return Optional.ofNullable(readAll().get(name));
    }

    /** All saved accounts, in file order. */
    public synchronized List<Account> all() {
        return new ArrayList<>(readAll().values());
    }

    /** Save the account, replacing its old line or adding a new one. */
    public synchronized void save(Account account) {
        LinkedHashMap<String, Account> accounts = readAll();
        accounts.put(account.name(), account);
        writeAll(accounts);
    }

    /**
     * Add tokens to several accounts at once (lottery prizes).
     * A name that isn't saved yet gets a new account first.
     */
    public synchronized void addTokens(Map<String, Integer> amounts) {
        if (amounts.isEmpty()) {
            return;
        }
        LinkedHashMap<String, Account> accounts = readAll();
        amounts.forEach((name, amount) ->
            accounts.computeIfAbsent(name, Account::newPlayer).addTokens(amount));
        writeAll(accounts);
    }

    private LinkedHashMap<String, Account> readAll() {
        LinkedHashMap<String, Account> accounts = new LinkedHashMap<>();
        List<String> lines = TextFiles.readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            try {
                //the debt (parts[3]) isn't read back: it comes from the tokens
                Account account = new Account(parts[0], parts[1], Integer.parseInt(parts[2]));
                accounts.put(account.name(), account);
            }
            catch (RuntimeException e) {
                throw new IllegalStateException("Bad line " + (i + 1) + " in " + file + ": " + line, e);
            }
        }
        return accounts;
    }

    private void writeAll(Map<String, Account> accounts) {
        List<String> lines = new ArrayList<>();
        for (Account a : accounts.values()) {
            lines.add(a.name() + " " + a.password() + " " + a.tokens() + " " + a.debt());
        }
        TextFiles.writeLines(file, lines);
    }
}
