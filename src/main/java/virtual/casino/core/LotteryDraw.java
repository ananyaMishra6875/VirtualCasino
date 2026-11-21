package virtual.casino.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The manager's lottery draw: pays every ticket's prize, then clears the tickets. */
public class LotteryDraw {

    private LotteryDraw() {}

    /** How one ticket did in the draw. */
    public record TicketResult(String name, int[] numbers, int matches, int prize) {}

    public static List<TicketResult> payOut(int[] drawn, LotteryStore tickets, UserStore users) {
        //hold the ticket file so no ticket bought during the draw gets lost
        synchronized (tickets) {
            List<TicketResult> results = new ArrayList<>();
            Map<String, Integer> prizes = new LinkedHashMap<>();
            for (LotteryStore.Ticket ticket : tickets.all()) {
                int matches = Lottery.matches(ticket.numbers(), drawn);
                int prize = Lottery.prize(matches);
                results.add(new TicketResult(ticket.name(), ticket.numbers(), matches, prize));
                if (prize > 0) {
                    prizes.merge(ticket.name(), prize, Integer::sum);
                }
            }
            //save all prizes in one go, then clear the tickets
            users.addTokens(prizes);
            tickets.clear();
            return results;
        }
    }
}
