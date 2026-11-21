package virtual.casino.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserStoreTest {

    @TempDir
    Path dir;

    Path file;
    UserStore users;

    @BeforeEach
    void setUp() {
        file = dir.resolve("userInfo.txt");
        users = new UserStore(file);
    }

    @Test
    void missingFileMeansNoAccounts() {
        assertTrue(users.find("ananya").isEmpty());
        assertTrue(users.all().isEmpty());
    }

    @Test
    void readsTheOldFileFormatIncludingNullDebt() throws Exception {
        Files.write(file, List.of("ananya secret -20 0", "charlie secret 100 null"));
        Account ananya = users.find("ananya").orElseThrow();
        assertEquals("secret", ananya.password());
        assertEquals(-20, ananya.tokens());
        assertEquals(20, ananya.debt());
        assertEquals(100, users.find("charlie").orElseThrow().tokens());
    }

    @Test
    void savingANewPlayerWritesANumberForDebt() throws Exception {
        // this was a bug: a new player's debt was saved as "null"
        users.save(Account.newPlayer("bob"));
        assertEquals(List.of("bob secret 100 0"), Files.readAllLines(file));
    }

    @Test
    void debtIsSavedFromTheTokens() throws Exception {
        // this was a bug: cashing out kept the old debt
        users.save(new Account("bob", "pw", -35));
        assertEquals(List.of("bob pw -35 35"), Files.readAllLines(file));
    }

    @Test
    void savingReplacesTheLineAndKeepsTheOthers() throws Exception {
        Files.write(file, List.of("ananya secret -20 0", "charlie secret 100 null"));
        users.save(new Account("ananya", "newPw1", 50));
        assertEquals(List.of("ananya newPw1 50 0", "charlie secret 100 0"), Files.readAllLines(file));
    }

    @Test
    void addTokensForAMissingNameKeepsEveryoneElse() throws Exception {
        // this was a bug: a prize for a name not in the file wiped the file
        Files.write(file, List.of("ananya secret -20 0"));
        users.addTokens(Map.of("ghost", 2000));
        assertEquals(-20, users.find("ananya").orElseThrow().tokens());
        assertEquals(2100, users.find("ghost").orElseThrow().tokens());
    }

    @Test
    void addTokensForSeveralPlayers() {
        users.save(new Account("a", "pw", 10));
        users.save(new Account("b", "pw", 20));
        users.addTokens(Map.of("a", 5, "b", 10000));
        assertEquals(15, users.find("a").orElseThrow().tokens());
        assertEquals(10020, users.find("b").orElseThrow().tokens());
    }

    @Test
    void badLineIsReportedNotSkipped() throws Exception {
        Files.write(file, List.of("ananya secret lots 0"));
        assertThrows(IllegalStateException.class, () -> users.find("ananya"));
    }
}
