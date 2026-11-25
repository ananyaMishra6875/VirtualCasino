package virtual.casino.web;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.JacksonException;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import virtual.casino.core.Account;
import virtual.casino.core.BlackJack;
import virtual.casino.core.Dice;
import virtual.casino.core.HiLo;
import virtual.casino.core.Login;
import virtual.casino.core.Lottery;
import virtual.casino.core.LotteryStore;
import virtual.casino.core.UserStore;

/**
 * The web version of the Virtual Casino.
 * The pages (src/main/resources/public) talk to this server in JSON,
 * and the server uses the same rules (core) as the console version.
 * Run it, then open http://localhost:7070
 */
public class WebApp {

    public static final int PORT = 7070;

    /** Session keys: the login in progress, and the logged in player's name. */
    private static final String LOGIN = "login";
    private static final String LOGIN_NAME = "loginName";
    private static final String PLAYER = "player";
    /** Session key: the Hi-Lo game in progress, if there is one. */
    private static final String HILO = "hilo";
    /** Session key: the BlackJack game in progress, if there is one. */
    private static final String BLACKJACK = "blackjack";

    private final UserStore users;
    private final LotteryStore tickets;
    private final Random r = new Random();

    public WebApp(UserStore users, LotteryStore tickets) {
        this.users = users;
        this.tickets = tickets;
    }

    public static void main(String[] args) {
        //same save files as the console version
        new WebApp(new UserStore(Path.of("userInfo.txt")), new LotteryStore(Path.of("lottery.txt"))).start(PORT);
        System.out.println("Virtual Casino is open at http://localhost:" + PORT);
    }

    public Javalin start(int port) {
        return Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);

            config.routes.post("/api/login", this::login);
            config.routes.post("/api/logout", this::logout);
            config.routes.get("/api/me", this::me);
            config.routes.post("/api/dice", this::dice);
            config.routes.post("/api/hilo/start", this::hiLoStart);
            config.routes.post("/api/hilo/guess", this::hiLoGuess);
            config.routes.post("/api/blackjack/start", this::blackJackStart);
            config.routes.post("/api/blackjack/hit", this::blackJackHit);
            config.routes.post("/api/blackjack/stand", this::blackJackStand);
            config.routes.get("/api/lottery", this::lottery);
            config.routes.post("/api/lottery/buy", this::lotteryBuy);

            //rules in core throw this for bad input: send the message to the page
            config.routes.exception(IllegalArgumentException.class, (e, ctx) ->
                error(ctx, HttpStatus.BAD_REQUEST, e.getMessage()));
            //a request body that isn't the JSON we expect
            config.routes.exception(JacksonException.class, (e, ctx) ->
                error(ctx, HttpStatus.BAD_REQUEST, "That request didn't make sense."));
        }).start(port);
    }

    record LoginRequest(String name, String password) {}
    record DiceRequest(int guess) {}
    record HiLoRequest(HiLo.Guess guess) {}
    /** Either 5 numbers, or random = true for a quick pick. */
    record LotteryRequest(int[] numbers, boolean random) {}

    /** Log in, with 3 tries for the password like the console version. */
    private void login(Context ctx) {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
        String name = req.name() == null ? "" : req.name().trim();
        if (Login.isManager(name)) {
            error(ctx, HttpStatus.FORBIDDEN, "The manager's page isn't in the web version yet.");
            return;
        }
        if (!Account.isValidName(name)) {
            error(ctx, HttpStatus.BAD_REQUEST, "Usernames can't be empty or contain spaces or \"|\".");
            return;
        }
        //keep the same login between tries, so the tries count down
        Login login = ctx.sessionAttribute(LOGIN);
        if (login == null || !name.equals(ctx.sessionAttribute(LOGIN_NAME))) {
            login = new Login(users, name);
            ctx.sessionAttribute(LOGIN, login);
            ctx.sessionAttribute(LOGIN_NAME, name);
        }
        if (login.failed()) {
            error(ctx, HttpStatus.FORBIDDEN, "Out of tries for " + name + ".");
            return;
        }
        if (!login.tryPassword(req.password() == null ? "" : req.password())) {
            String msg = login.failed()
                ? "Incorrect password. Out of tries for " + name + "."
                : "Incorrect password. " + login.triesLeft() + " " + (login.triesLeft() == 1 ? "try" : "tries") + " left.";
            error(ctx, HttpStatus.UNAUTHORIZED, msg);
            return;
        }
        //a new player is saved now, so their 100 starting tokens are kept
        if (login.isNewPlayer()) {
            users.save(login.account());
        }
        ctx.sessionAttribute(LOGIN, null);
        ctx.sessionAttribute(LOGIN_NAME, null);
        ctx.sessionAttribute(PLAYER, name);
        ctx.json(player(login.account(), login.isNewPlayer()));
    }

    private void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.status(HttpStatus.NO_CONTENT);
    }

    /** Who is logged in, so a page reload stays logged in. */
    private void me(Context ctx) {
        Account account = loggedIn(ctx);
        if (account != null) {
            ctx.json(player(account, false));
        }
    }

    /** One game of CRAPS (rules in core/Dice.java). */
    private void dice(Context ctx) {
        Account account = loggedIn(ctx);
        if (account == null) {
            return;
        }
        int guess = ctx.bodyAsClass(DiceRequest.class).guess();
        if (guess < 4 || guess > 12) {
            error(ctx, HttpStatus.BAD_REQUEST, "Pick a number from 4-12.");
            return;
        }
        Dice.Result result = new Dice(r).play(guess);
        account.addTokens(result.tokenChange());
        users.save(account);
        ctx.json(Map.of("result", result, "player", player(account, false)));
    }

    /**
     * Deal a new Hi-Lo game (rules in core/HiLo.java) and show the first card.
     * If a game is already going, that one is shown again instead,
     * so reloading the page can't swap away a bad card.
     */
    private void hiLoStart(Context ctx) {
        Account account = loggedIn(ctx);
        if (account == null) {
            return;
        }
        HiLo game = ctx.sessionAttribute(HILO);
        if (game == null) {
            game = new HiLo(shuffled(hiLoDeck()));
            ctx.sessionAttribute(HILO, game);
        }
        ctx.json(Map.of(
            "card", game.currentCard(),
            "cardName", HiLo.cardName(game.currentCard()),
            "ties", game.ties(),
            "prize", hiLoPrize(game.ties()),
            "player", player(account, false)));
    }

    /** Guess if the next card is HIGHER or LOWER. On a tie the game goes on. */
    private void hiLoGuess(Context ctx) {
        Account account = loggedIn(ctx);
        if (account == null) {
            return;
        }
        HiLo game = ctx.sessionAttribute(HILO);
        if (game == null) {
            error(ctx, HttpStatus.BAD_REQUEST, "Deal a card first.");
            return;
        }
        HiLo.Guess guess = ctx.bodyAsClass(HiLoRequest.class).guess();
        if (guess == null) {
            error(ctx, HttpStatus.BAD_REQUEST, "Pick higher or lower.");
            return;
        }
        HiLo.Turn turn = game.guess(guess);
        if (game.isOver()) {
            ctx.sessionAttribute(HILO, null);
            account.addTokens(turn.tokenChange());
            users.save(account);
        }
        ctx.json(Map.of(
            "turn", turn,
            "cardName", HiLo.cardName(turn.card()),
            "nextCardName", HiLo.cardName(turn.nextCard()),
            "prize", hiLoPrize(turn.ties()),
            "player", player(account, false)));
    }

    /** The lottery's rules, and the player's tickets waiting for the next draw. */
    private void lottery(Context ctx) {
        Account account = loggedIn(ctx);
        if (account != null) {
            ctx.json(lotteryTable(account));
        }
    }

    /**
     * Buy a ticket (rules in core/Lottery.java) for the next draw, which the manager runs.
     * The ticket is saved before the tokens are taken, so a bad ticket costs nothing.
     */
    private void lotteryBuy(Context ctx) {
        Account account = loggedIn(ctx);
        if (account == null) {
            return;
        }
        LotteryRequest req = ctx.bodyAsClass(LotteryRequest.class);
        int[] numbers = req.random() ? Lottery.randomNumbers(r) : req.numbers();
        if (!Lottery.isValidTicket(numbers)) {
            error(ctx, HttpStatus.BAD_REQUEST,
                "Pick " + Lottery.NUMBERS + " different numbers from " + Lottery.MIN + "-" + Lottery.MAX + ".");
            return;
        }
        tickets.add(account.name(), numbers);
        account.addTokens(-Lottery.TICKET_COST);
        users.save(account);
        Map<String, Object> table = lotteryTable(account);
        table.put("bought", numbers);
        ctx.json(table);
    }

    private Map<String, Object> lotteryTable(Account account) {
        List<int[]> mine = new ArrayList<>();
        for (LotteryStore.Ticket t : tickets.all()) {
            if (t.name().equals(account.name())) {
                mine.add(t.numbers());
            }
        }
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("tickets", mine);
        table.put("cost", Lottery.TICKET_COST);
        table.put("numbers", Lottery.NUMBERS);
        table.put("min", Lottery.MIN);
        table.put("max", Lottery.MAX);
        table.put("prizes", Map.of(3, Lottery.prize(3), 4, Lottery.prize(4), 5, Lottery.prize(5)));
        table.put("player", player(account, false));
        return table;
    }

    /**
     * Deal a new BlackJack game (rules in core/BlackJack.java).
     * If a game is already going, that one is shown again instead.
     * A 21 straight away ends the game at once.
     */
    private void blackJackStart(Context ctx) {
        Account account = loggedIn(ctx);
        if (account == null) {
            return;
        }
        BlackJack game = ctx.sessionAttribute(BLACKJACK);
        if (game == null) {
            game = new BlackJack(shuffled(blackJackDeck()));
            ctx.sessionAttribute(BLACKJACK, game);
        }
        sendBlackJack(ctx, account, game);
    }

    private void blackJackHit(Context ctx) {
        Account account = loggedIn(ctx);
        BlackJack game = account == null ? null : blackJackInProgress(ctx);
        if (game != null) {
            game.hit();
            sendBlackJack(ctx, account, game);
        }
    }

    private void blackJackStand(Context ctx) {
        Account account = loggedIn(ctx);
        BlackJack game = account == null ? null : blackJackInProgress(ctx);
        if (game != null) {
            game.stand();
            sendBlackJack(ctx, account, game);
        }
    }

    /** The BlackJack game in progress, or null after sending a 400. */
    private BlackJack blackJackInProgress(Context ctx) {
        BlackJack game = ctx.sessionAttribute(BLACKJACK);
        if (game == null) {
            error(ctx, HttpStatus.BAD_REQUEST, "Deal the cards first.");
        }
        return game;
    }

    /**
     * Send the table. While the game is going only the dealer's first card is sent,
     * so the page can't peek at the face-down one. When it's over the tokens are
     * saved and the game is cleared, ready for the next deal.
     */
    private void sendBlackJack(Context ctx, Account account, BlackJack game) {
        List<Integer> dealer = game.dealerHand();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("playerHand", game.playerHand());
        table.put("playerTotal", BlackJack.handTotal(game.playerHand()));
        if (game.isOver()) {
            ctx.sessionAttribute(BLACKJACK, null);
            account.addTokens(game.result().tokenChange());
            users.save(account);
            table.put("dealerHand", dealer);
            table.put("dealerTotal", BlackJack.handTotal(dealer));
            table.put("result", game.result());
        }
        else {
            table.put("dealerHand", dealer.subList(0, 1));
            table.put("dealerTotal", BlackJack.handTotal(dealer.subList(0, 1)));
        }
        table.put("over", game.isOver());
        table.put("player", player(account, false));
        ctx.json(table);
    }

    /** A BlackJack deck like the console version: aces are 1, and 10, J, Q and K are all 10. */
    private static List<Integer> blackJackDeck() {
        List<Integer> deck = new ArrayList<>();
        for (int suit = 0; suit < 4; suit++) {
            for (int card = 1; card <= 13; card++) {
                deck.add(Math.min(card, 10));
            }
        }
        return deck;
    }

    /** What a right guess wins after this many ties (see core/HiLo.java). */
    private static int hiLoPrize(int ties) {
        return ties == 0 ? HiLo.PRIZE : HiLo.PRIZE * ties * 2;
    }

    /** A Hi-Lo deck: 52 cards, four of each 1-13, like the console version. */
    private static List<Integer> hiLoDeck() {
        List<Integer> deck = new ArrayList<>();
        for (int suit = 0; suit < 4; suit++) {
            for (int card = 1; card <= 13; card++) {
                deck.add(card);
            }
        }
        return deck;
    }

    private List<Integer> shuffled(List<Integer> deck) {
        Collections.shuffle(deck, r);
        return deck;
    }

    /** The logged in player's saved account, or null after sending a 401. */
    private Account loggedIn(Context ctx) {
        String name = ctx.sessionAttribute(PLAYER);
        Account account = name == null ? null : users.find(name).orElse(null);
        if (account == null) {
            error(ctx, HttpStatus.UNAUTHORIZED, "Please log in.");
        }
        return account;
    }

    private static Map<String, Object> player(Account a, boolean isNew) {
        return Map.of("name", a.name(), "balance", a.balance(), "debt", a.debt(), "isNew", isNew);
    }

    private static void error(Context ctx, HttpStatus status, String message) {
        ctx.status(status).json(Map.of("error", message));
    }
}
