// The web pages of the Virtual Casino. The server (WebApp.java) does the
// rules and saving; this file only shows screens and sends what the player picks.

const $ = (id) => document.getElementById(id);

// ---------- talking to the server ----------
async function api(path, body) {
  const res = await fetch(path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || "Something went wrong.");
  return data;
}

// ---------- screens ----------
function show(view) {
  document.querySelectorAll(".view").forEach((v) => (v.hidden = v.id !== "view-" + view));
  $("topbar").hidden = view === "login";
  if (view === "dice") resetDice();
  if (view === "hilo") startHiLo();
  if (view === "blackjack") startBlackJack();
  if (view === "lottery") openLottery();
}

document.addEventListener("click", (e) => {
  const go = e.target.closest("[data-go]");
  if (go) show(go.dataset.go);
});

function showPlayer(p) {
  $("balance").textContent = p.balance;
  $("debt").hidden = p.debt === 0;
  $("debt").textContent = `(${p.debt} in debt)`;
  $("who").textContent = p.name;
}

// ---------- login ----------
$("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("login-error").textContent = "";
  try {
    const p = await api("/api/login", { name: $("name").value, password: $("password").value });
    showPlayer(p);
    $("greeting").textContent = p.isNew
      ? `Hello ${p.name}, and welcome to The Virtual Casino!`
      : `Welcome back, ${p.name}!`;
    $("password").value = "";
    show("lobby");
  } catch (err) {
    $("login-error").textContent = err.message;
  }
});

$("logout").addEventListener("click", async () => {
  await api("/api/logout", {});
  show("login");
});

// ---------- dice ----------
// which of the 9 grid spots have a pip, for each face
const PIPS = { 1: [4], 2: [0, 8], 3: [0, 4, 8], 4: [0, 2, 6, 8], 5: [0, 2, 4, 6, 8], 6: [0, 2, 3, 5, 6, 8] };

function drawDie(el, value) {
  el.innerHTML = "";
  for (let i = 0; i < 9; i++) {
    const cell = document.createElement("span");
    if (PIPS[value].includes(i)) cell.className = "pip";
    el.appendChild(cell);
  }
}

let guess = null;
let rolling = false;

for (let n = 4; n <= 12; n++) {
  const b = document.createElement("button");
  b.className = "guess";
  b.textContent = n;
  b.addEventListener("click", () => {
    if (rolling) return;
    guess = n;
    document.querySelectorAll(".guess").forEach((g) => g.classList.toggle("picked", g === b));
    $("roll").disabled = false;
    $("roll").textContent = `Roll for ${n}`;
  });
  $("guesses").appendChild(b);
}

function resetDice() {
  drawDie($("die1"), 6);
  drawDie($("die2"), 6);
  $("dice-outcome").className = "outcome";
  $("dice-outcome").innerHTML = "&nbsp;";
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function animateRoll(roll) {
  const dice = [$("die1"), $("die2")];
  dice.forEach((d) => d.classList.remove("rolling"));
  void dice[0].offsetWidth; // restart the animation
  dice.forEach((d) => d.classList.add("rolling"));
  // flicker random faces, then land on the real ones
  for (let i = 0; i < 6; i++) {
    dice.forEach((d) => drawDie(d, 1 + Math.floor(Math.random() * 6)));
    await sleep(90);
  }
  drawDie(dice[0], roll.die1);
  drawDie(dice[1], roll.die2);
}

$("roll").addEventListener("click", async () => {
  if (guess === null || rolling) return;
  rolling = true;
  $("roll").disabled = true;
  const out = $("dice-outcome");
  out.className = "outcome";
  out.innerHTML = "Rolling…";
  try {
    const { result, player } = await api("/api/dice", { guess });
    await animateRoll(result.firstRoll);
    if (result.reroll) {
      out.textContent = `Rolled a ${total(result.firstRoll)}. Rolling again!`;
      await sleep(700);
      await animateRoll(result.reroll);
    }
    const final = total(result.reroll || result.firstRoll);
    out.className = "outcome " + (result.won ? "win" : "lose");
    out.innerHTML = (result.won ? `You won! +${result.tokenChange} tokens` : `You lost! ${result.tokenChange} tokens`)
      + `<small>You picked ${result.guess}, the dice show ${final}.</small>`;
    showPlayer(player);
  } catch (err) {
    out.className = "outcome lose";
    out.textContent = err.message;
    if (err.message === "Please log in.") show("login");
  } finally {
    rolling = false;
    $("roll").disabled = false;
  }
});

const total = (roll) => roll.die1 + roll.die2;

// ---------- hi-lo ----------
// The server only knows card values (1-13), so the suit is just for looks.
const SUITS = ["♠", "♥", "♦", "♣"];
let hiLoBusy = false;

function drawCard(el, name, suit = SUITS[Math.floor(Math.random() * 4)]) {
  const front = el.querySelector(".front");
  front.classList.toggle("red", suit === "♥" || suit === "♦");
  front.dataset.suit = suit;
  front.innerHTML =
    `<span class="card-corner top">${name}<small>${suit}</small></span>` +
    `<span class="card-big">${suit}</span>` +
    `<span class="card-corner bottom">${name}<small>${suit}</small></span>`;
}

// turn the next card face down straight away, with no flip
function hideNextCard() {
  const next = $("hilo-next");
  next.classList.add("instant", "down");
  void next.offsetWidth;
  next.classList.remove("instant");
}

function showHiLoButtons(playing) {
  $("hilo-guesses").hidden = !playing;
  $("hilo-again").hidden = playing;
  $("hilo-lower").disabled = $("hilo-higher").disabled = !playing;
}

function showTies(ties, prize) {
  $("hilo-ties").hidden = ties === 0;
  $("hilo-ties").textContent = `Tie ×${ties}`;
  $("hilo-prize").textContent = prize;
}

// deal a new game, or show the one already going
async function startHiLo() {
  if (hiLoBusy) return;
  hiLoBusy = true;
  const out = $("hilo-outcome");
  out.className = "outcome";
  out.innerHTML = "&nbsp;";
  $("hilo-lower").disabled = $("hilo-higher").disabled = true;
  try {
    const game = await api("/api/hilo/start", {});
    hideNextCard();
    drawCard($("hilo-current"), game.cardName);
    showTies(game.ties, game.prize);
    showPlayer(game.player);
    showHiLoButtons(true);
  } catch (err) {
    out.className = "outcome lose";
    out.textContent = err.message;
    if (err.message === "Please log in.") show("login");
  } finally {
    hiLoBusy = false;
  }
}

async function guessHiLo(guess) {
  if (hiLoBusy) return;
  hiLoBusy = true;
  $("hilo-lower").disabled = $("hilo-higher").disabled = true;
  const out = $("hilo-outcome");
  out.className = "outcome";
  out.innerHTML = "&nbsp;";
  try {
    const { turn, cardName, nextCardName, prize, player } = await api("/api/hilo/guess", { guess });
    const next = $("hilo-next");
    const shownSuit = $("hilo-current").querySelector(".front").dataset.suit;
    drawCard(next, nextCardName, nextCardName === cardName
      ? SUITS.filter((x) => x !== shownSuit)[Math.floor(Math.random() * 3)]
      : undefined);
    next.classList.remove("down"); // flip it over
    await sleep(650);

    if (turn.outcome === "TIE") {
      showTies(turn.ties, prize);
      out.innerHTML = `A tie, it's another ${nextCardName}! Guess again.` +
        `<small>The prize is now ${prize} tokens.</small>`;
      await sleep(1100);
      // the tied card becomes the card to beat
      drawCard($("hilo-current"), nextCardName, next.querySelector(".front").dataset.suit);
      hideNextCard();
      showHiLoButtons(true);
      return;
    }

    const won = turn.outcome === "WIN";
    out.className = "outcome " + (won ? "win" : "lose");
    out.innerHTML = (won ? `You won! +${turn.tokenChange} tokens` : `You lost! ${turn.tokenChange} tokens`) +
      `<small>You said ${guess.toLowerCase()} than ${cardName}, the next card was ${nextCardName}.</small>`;
    showPlayer(player);
    showHiLoButtons(false);
  } catch (err) {
    out.className = "outcome lose";
    out.textContent = err.message;
    if (err.message === "Please log in.") show("login");
    else showHiLoButtons(true);
  } finally {
    hiLoBusy = false;
  }
}

$("hilo-lower").addEventListener("click", () => guessHiLo("LOWER"));
$("hilo-higher").addEventListener("click", () => guessHiLo("HIGHER"));
$("hilo-again").addEventListener("click", startHiLo);

// ---------- blackjack ----------
// The server sends card values (1-10). An ace is A, and a 10 is shown as
// 10, J, Q or K at random: like the suits, that is only for looks.
let bjBusy = false;
let bjHole = null; // the dealer's face-down card while the game is going
let bjUsed = new Set(); // faces already shown this game, so no card shows up twice

const pick = (list) => list[Math.floor(Math.random() * list.length)];

function bjFace(value) {
  const names = value === 1 ? ["A"] : value === 10 ? ["10", "J", "Q", "K"] : [String(value)];
  const free = names.flatMap((name) => SUITS.map((suit) => ({ name, suit })))
    .filter((f) => !bjUsed.has(f.name + f.suit));
  const face = pick(free);
  bjUsed.add(face.name + face.suit);
  return face;
}

function drawBjCard(card, value) {
  const face = bjFace(value);
  drawCard(card, face.name, face.suit);
}

// add a card to a hand with a dealing animation
function dealCard(hand, value, faceDown = false) {
  const card = document.createElement("div");
  card.className = "playing-card dealt" + (faceDown ? " down" : "");
  card.innerHTML = '<div class="card-inner"><div class="card-face front"></div><div class="card-face back"></div></div>';
  if (!faceDown) drawBjCard(card, value);
  hand.appendChild(card);
  return card;
}

function showBjMoves(playing) {
  $("bj-moves").hidden = !playing;
  $("bj-again").hidden = playing;
  $("bj-hit").disabled = $("bj-stand").disabled = !playing;
}

function bjError(err) {
  const out = $("bj-outcome");
  out.className = "outcome lose";
  out.textContent = err.message;
  if (err.message === "Please log in.") show("login");
}

// deal a new game (or show the one already going), one card at a time
async function startBlackJack() {
  if (bjBusy) return;
  bjBusy = true;
  showBjMoves(true);
  $("bj-hit").disabled = $("bj-stand").disabled = true;
  $("bj-outcome").className = "outcome";
  $("bj-outcome").innerHTML = "&nbsp;";
  $("bj-player").innerHTML = $("bj-dealer").innerHTML = "";
  $("bj-player-total").textContent = $("bj-dealer-total").textContent = "";
  bjUsed = new Set();
  try {
    const t = await api("/api/blackjack/start", {});
    // like a real table: you, dealer, you, dealer (face down), then any cards you already hit
    dealCard($("bj-player"), t.playerHand[0]); await sleep(300);
    dealCard($("bj-dealer"), t.dealerHand[0]); await sleep(300);
    dealCard($("bj-player"), t.playerHand[1]); await sleep(300);
    bjHole = dealCard($("bj-dealer"), null, true); await sleep(300);
    for (const v of t.playerHand.slice(2)) { dealCard($("bj-player"), v); await sleep(300); }
    $("bj-player-total").textContent = t.playerTotal;
    $("bj-dealer-total").textContent = t.dealerTotal;
    showPlayer(t.player);
    if (t.over) await finishBlackJack(t);
    else $("bj-hit").disabled = $("bj-stand").disabled = false;
  } catch (err) {
    bjError(err);
  } finally {
    bjBusy = false;
  }
}

async function bjMove(move) {
  if (bjBusy) return;
  bjBusy = true;
  $("bj-hit").disabled = $("bj-stand").disabled = true;
  try {
    const t = await api("/api/blackjack/" + move, {});
    if (move === "hit") {
      dealCard($("bj-player"), t.playerHand[t.playerHand.length - 1]);
      $("bj-player-total").textContent = t.playerTotal;
      await sleep(400);
    }
    if (t.over) await finishBlackJack(t);
    else $("bj-hit").disabled = $("bj-stand").disabled = false;
  } catch (err) {
    bjError(err);
    if (err.message !== "Please log in.") showBjMoves(false);
  } finally {
    bjBusy = false;
  }
}

// the dealer's turn: flip the face-down card, then draw the rest one by one
async function finishBlackJack(t) {
  $("bj-dealer-total").textContent = "…";
  drawBjCard(bjHole, t.dealerHand[1]);
  bjHole.classList.remove("down");
  await sleep(700);
  for (const v of t.dealerHand.slice(2)) {
    dealCard($("bj-dealer"), v);
    await sleep(500);
  }
  $("bj-dealer-total").textContent = t.dealerTotal;

  const r = t.result;
  const blackJack = r.playerTotal === 21 && t.playerHand.length === 2;
  const out = $("bj-outcome");
  out.className = "outcome " + (r.outcome === "WIN" ? "win" : r.outcome === "PUSH" ? "" : "lose");
  const headline = {
    WIN: (blackJack ? "BlackJack! " : "") + `You won! +${r.tokenChange} tokens`,
    LOSE: `You lost! ${r.tokenChange} tokens`,
    BUST: `Bust! ${r.tokenChange} tokens`,
    PUSH: "Push! It's a tie, no tokens won or lost.",
  }[r.outcome];
  const detail = r.outcome === "BUST"
    ? `You went over 21 with ${r.playerTotal}.`
    : `You have ${r.playerTotal}, the dealer has ${r.dealerTotal}` + (r.dealerTotal > 21 ? " (bust)." : ".");
  out.innerHTML = `${headline}<small>${detail}</small>`;
  showPlayer(t.player);
  showBjMoves(false);
}

$("bj-hit").addEventListener("click", () => bjMove("hit"));
$("bj-stand").addEventListener("click", () => bjMove("stand"));
$("bj-again").addEventListener("click", startBlackJack);

// ---------- lottery ----------
// The server has the rules (how many numbers, the range, cost, prizes) and keeps the tickets.
let lotto = null; // the rules from the server
let lottoPicked = [];
let lottoBusy = false;

// a lottery ball; its colour shows which third of the range the number is in
function ball(n, delay = 0) {
  const b = document.createElement("span");
  if (n === null) {
    b.className = "ball empty";
  } else {
    const third = Math.min(3, 1 + Math.floor((3 * (n - lotto.min)) / (lotto.max - lotto.min + 1)));
    b.className = "ball r" + third + (delay >= 0 ? " pop" : "");
    b.dataset.n = n;
    b.setAttribute("role", "img");
    b.setAttribute("aria-label", String(n));
    b.style.animationDelay = Math.max(delay, 0) + "ms";
  }
  return b;
}

async function openLottery() {
  try {
    const t = await api("/api/lottery");
    lotto = t;
    $("lotto-rules").textContent = `Pick ${t.numbers} different numbers from ${t.min}-${t.max}. A ticket costs ${t.cost} tokens.`;
    $("lotto-prizes").innerHTML = Object.entries(t.prizes)
      .map(([matches, prize]) => `<span class="prize-chip">${matches} match → <strong>${Number(prize).toLocaleString()}</strong></span>`)
      .join("");
    $("lotto-buy").textContent = `Buy ticket (${t.cost} tokens)`;
    $("lotto-quick").textContent = `Quick pick (${t.cost} tokens)`;
    if (!$("lotto-grid").children.length) {
      for (let n = t.min; n <= t.max; n++) {
        const b = document.createElement("button");
        b.className = "lotto-num";
        b.textContent = n;
        b.dataset.n = n;
        b.addEventListener("click", () => toggleNumber(n));
        $("lotto-grid").appendChild(b);
      }
    }
    lottoPicked = [];
    $("lotto-outcome").className = "outcome";
    $("lotto-outcome").innerHTML = "&nbsp;";
    showPicked();
    showTickets(t.tickets, false);
    showPlayer(t.player);
  } catch (err) {
    $("lotto-outcome").className = "outcome lose";
    $("lotto-outcome").textContent = err.message;
    if (err.message === "Please log in.") show("login");
  }
}

function toggleNumber(n) {
  if (lottoBusy) return;
  if (lottoPicked.includes(n)) lottoPicked = lottoPicked.filter((x) => x !== n);
  else if (lottoPicked.length < lotto.numbers) lottoPicked.push(n);
  showPicked();
}

function showPicked() {
  const full = lottoPicked.length === lotto.numbers;
  document.querySelectorAll(".lotto-num").forEach((b) => {
    const picked = lottoPicked.includes(Number(b.dataset.n));
    b.classList.toggle("picked", picked);
    b.classList.toggle("full", full && !picked);
  });
  const row = $("lotto-picked");
  row.innerHTML = "";
  const sorted = [...lottoPicked].sort((a, b) => a - b);
  for (let i = 0; i < lotto.numbers; i++) row.appendChild(ball(i < sorted.length ? sorted[i] : null, -1));
  $("lotto-buy").disabled = !full || lottoBusy;
  $("lotto-clear").disabled = !lottoPicked.length || lottoBusy;
  $("lotto-quick").disabled = lottoBusy;
}

// the list of tickets; the newest one pops in when it was just bought
function showTickets(tickets, newestPops) {
  const list = $("lotto-tickets");
  list.innerHTML = tickets.length ? "" : '<p class="none">No tickets yet. Pick your numbers above!</p>';
  tickets.forEach((nums, i) => {
    const pop = newestPops && i === tickets.length - 1;
    const row = document.createElement("div");
    row.className = "ticket";
    row.innerHTML = `<span class="ticket-no">Ticket ${i + 1}</span>`;
    const balls = document.createElement("div");
    balls.className = "ball-row";
    [...nums].sort((a, b) => a - b).forEach((n, j) => balls.appendChild(ball(n, pop ? j * 120 : -1)));
    row.appendChild(balls);
    list.appendChild(row);
  });
}

async function buyTicket(body) {
  if (lottoBusy) return;
  lottoBusy = true;
  showPicked();
  const out = $("lotto-outcome");
  try {
    const t = await api("/api/lottery/buy", body);
    lottoPicked = [];
    out.className = "outcome win";
    out.innerHTML = `Ticket bought! -${t.cost} tokens` +
      `<small>Your numbers: ${[...t.bought].sort((a, b) => a - b).join(", ")}. Good luck in the draw!</small>`;
    showTickets(t.tickets, true);
    showPlayer(t.player);
  } catch (err) {
    out.className = "outcome lose";
    out.textContent = err.message;
    if (err.message === "Please log in.") show("login");
  } finally {
    lottoBusy = false;
    showPicked();
  }
}

$("lotto-buy").addEventListener("click", () => buyTicket({ numbers: lottoPicked }));
$("lotto-quick").addEventListener("click", () => buyTicket({ random: true }));
$("lotto-clear").addEventListener("click", () => { lottoPicked = []; showPicked(); });

// ---------- start: stay logged in after a page reload ----------
(async () => {
  try {
    const p = await api("/api/me");
    showPlayer(p);
    $("greeting").textContent = `Welcome back, ${p.name}!`;
    show("lobby");
  } catch {
    show("login");
  }
})();
