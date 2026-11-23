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
    drawCard(next, nextCardName);
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
