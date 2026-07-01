(() => {
  "use strict";

  // ---------------------------------------------------------------------
  // Word bank — sorted by length for an easy -> hard difficulty curve
  // ---------------------------------------------------------------------
  const RAW_WORD_BANK = [
    { word: "سیب", hint: "میوه‌ی قرمز یا سبز" },
    { word: "فیل", hint: "بزرگ‌ترین حیوان خشکی" },
    { word: "ببر", hint: "گربه‌سان راه‌راه" },
    { word: "شیر", hint: "پادشاه جنگل" },
    { word: "مرغ", hint: "تخم می‌گذارد" },
    { word: "نان", hint: "هر روز می‌خوریمش" },
    { word: "میز", hint: "روی آن غذا می‌خوریم" },
    { word: "سقف", hint: "بالای اتاق" },
    { word: "قفل", hint: "با کلید باز می‌شود" },
    { word: "چتر", hint: "زیر باران از آن استفاده می‌کنیم" },
    { word: "ابر", hint: "در آسمان، پیش از باران" },
    { word: "برف", hint: "در زمستان می‌بارد" },
    { word: "باد", hint: "هوا را حرکت می‌دهد" },
    { word: "کوه", hint: "بلند و سنگی" },
    { word: "دشت", hint: "زمین صاف و وسیع" },
    { word: "چشم", hint: "با آن می‌بینیم" },
    { word: "گوش", hint: "با آن می‌شنویم" },
    { word: "دست", hint: "با آن کار می‌کنیم" },
    { word: "قلب", hint: "خون را پمپاژ می‌کند" },
    { word: "باغ", hint: "پر از درخت و گل" },
    { word: "درب", hint: "از آن وارد می‌شویم" },
    { word: "پله", hint: "برای بالا رفتن" },
    { word: "کیف", hint: "وسایل را در آن می‌گذاریم" },
    { word: "توپ", hint: "در فوتبال از آن استفاده می‌شود" },
    { word: "رنگ", hint: "قرمز، آبی، زرد..." },
    { word: "قلم", hint: "وسیله‌ی نوشتن یا نقاشی" },
    { word: "سیر", hint: "ادویه‌ی تند و سفید" },
    { word: "موز", hint: "میوه‌ی زرد و دراز" },
    { word: "هلو", hint: "میوه‌ی نرم و پرمو" },
    { word: "نمک", hint: "به غذا طعم می‌دهد" },
    { word: "شکر", hint: "غذا را شیرین می‌کند" },
    { word: "چای", hint: "نوشیدنی داغ صبحگاهی" },
    { word: "کتاب", hint: "چیزی که می‌خوانیم" },
    { word: "دریا", hint: "آب نمکی بزرگ" },
    { word: "آینه", hint: "خودت را در آن می‌بینی" },
    { word: "مداد", hint: "با آن می‌نویسیم" },
    { word: "درخت", hint: "ریشه، تنه و برگ دارد" },
    { word: "ماهی", hint: "در آب زندگی می‌کند" },
    { word: "گربه", hint: "میو میو می‌کند" },
    { word: "خانه", hint: "در آن زندگی می‌کنیم" },
    { word: "اتاق", hint: "بخشی از خانه" },
    { word: "حیاط", hint: "فضای باز خانه" },
    { word: "جاده", hint: "ماشین‌ها در آن حرکت می‌کنند" },
    { word: "کشتی", hint: "روی آب حرکت می‌کند" },
    { word: "پارک", hint: "جای بازی و پیاده‌روی" },
    { word: "معلم", hint: "درس می‌دهد" },
    { word: "کلاس", hint: "در مدرسه درس می‌خوانیم" },
    { word: "دفتر", hint: "در آن می‌نویسیم" },
    { word: "بازی", hint: "برای سرگرمی می‌کنیم" },
    { word: "طبل", hint: "با کوبیدن صدا می‌دهد" },
    { word: "کاغذ", hint: "روی آن می‌نویسیم" },
    { word: "هویج", hint: "سبزی نارنجی" },
    { word: "گوجه", hint: "سبزی قرمز برای سالاد" },
    { word: "خیار", hint: "سبزی سبز و خنک" },
    { word: "پیاز", hint: "لایه‌لایه و تند" },
    { word: "قهوه", hint: "نوشیدنی تلخ و داغ" },
    { word: "پنیر", hint: "از شیر درست می‌شود" },
    { word: "ماست", hint: "لبنیات سفید و ترش" },
    { word: "برنج", hint: "غذای اصلی ایرانی" },
    { word: "زبان", hint: "با آن صحبت می‌کنیم" },
    { word: "دهان", hint: "با آن غذا می‌خوریم" },
    { word: "جنگل", hint: "پر از درخت" },
    { word: "چشمه", hint: "آب از زمین می‌جوشد" },
    { word: "ستاره", hint: "در آسمان شب می‌درخشد" },
    { word: "باران", hint: "از آسمان می‌بارد" },
    { word: "پرنده", hint: "در آسمان پرواز می‌کند" },
    { word: "گلدان", hint: "گل در آن می‌کاریم" },
    { word: "پنجره", hint: "از آن بیرون را می‌بینیم" },
    { word: "صندلی", hint: "روی آن می‌نشینیم" },
    { word: "دیوار", hint: "اتاق را احاطه می‌کند" },
    { word: "چراغ", hint: "اتاق را روشن می‌کند" },
    { word: "کلید", hint: "در را با آن باز می‌کنیم" },
    { word: "ساعت", hint: "زمان را نشان می‌دهد" },
    { word: "عینک", hint: "روی چشم می‌گذاریم" },
    { word: "کفش", hint: "روی پا می‌پوشیم" },
    { word: "لباس", hint: "روی بدن می‌پوشیم" },
    { word: "کلاه", hint: "روی سر می‌گذاریم" },
    { word: "جوراب", hint: "روی پا، زیر کفش" },
    { word: "دستکش", hint: "دست را در زمستان گرم می‌کند" },
    { word: "دندان", hint: "غذا را با آن می‌جویم" },
    { word: "انگشت", hint: "پنج‌تا در هر دست" },
    { word: "گیتار", hint: "ساز شش‌سیم" },
    { word: "نقاشی", hint: "تصویری که می‌کشیم" },
    { word: "بستنی", hint: "خوراکی سرد و شیرین" },
    { word: "شیرینی", hint: "خوراکی شیرین جشن‌ها" },
    { word: "عروسک", hint: "بازیچه‌ی شبیه انسان" },
    { word: "ماشین", hint: "با چهار چرخ حرکت می‌کند" },
    { word: "مدرسه", hint: "جایی که درس می‌خوانیم" },
    { word: "خورشید", hint: "روز را روشن می‌کند" },
  ];

  const WORD_BANK = [...RAW_WORD_BANK].sort((a, b) => a.word.length - b.word.length);

  // ---------------------------------------------------------------------
  // tiny inline icon set (no external deps)
  // ---------------------------------------------------------------------
  const ICON = {
    star: (fill) => `<svg class="icon-inline" width="14" height="14" viewBox="0 0 24 24" fill="${fill||'none'}" stroke="${fill||'currentColor'}" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 21 12 17.27 5.82 21 7 14.14l-5-4.87 6.91-1.01L12 2z"/></svg>`,
    flame: (fill) => `<svg class="icon-inline" width="16" height="16" viewBox="0 0 24 24" fill="${fill||'none'}" stroke="currentColor" stroke-width="2"><path d="M12 2s-6 6-6 12a6 6 0 0012 0c0-3-2-4-2-6 0 2-1 3-2 3 0-3 3-5 3-9-2 1-5 3-5 0z"/></svg>`,
    bulb: () => `<svg class="icon-inline" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18h6M10 22h4M12 2a7 7 0 00-4 12.7c.5.4.9 1 1 1.6V17h6v-.7c.1-.6.5-1.2 1-1.6A7 7 0 0012 2z"/></svg>`,
    check: () => `<svg class="icon-inline" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>`,
    trophy: () => `<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M8 21h8M12 17v4M7 4h10v4a5 5 0 01-10 0V4z"/><path d="M7 5H4a3 3 0 003 3M17 5h3a3 3 0 01-3 3"/></svg>`,
    play: () => `<svg class="icon-inline" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>`,
    help: () => `<svg class="icon-inline" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.5 9a2.5 2.5 0 115 .5c0 1.5-2 2-2 3.5M12 17h.01"/></svg>`,
    back: () => `<svg class="icon-inline" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M13 6l6 6-6 6"/></svg>`,
    undo: () => `<svg class="icon-inline" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 10h11a5 5 0 010 10H9M3 10l5-5M3 10l5 5"/></svg>`,
  };

  // ---------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------
  function shuffle(letters) {
    const arr = [...letters];
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    if (arr.join("") === letters.join("") && arr.length > 1) {
      [arr[0], arr[1]] = [arr[1], arr[0]];
    }
    return arr;
  }

  function freshTiles(word) {
    return shuffle(word.split("")).map((ch, i) => ({
      id: `${word}-${Date.now()}-${i}-${Math.random().toString(36).slice(2, 6)}`,
      ch,
      used: false,
    }));
  }

  function shuffleWordOrder() {
    return shuffle(WORD_BANK);
  }

  function makeRound(index, wordList) {
    const entry = wordList[index % wordList.length];
    return { word: entry.word, hint: entry.hint, tiles: freshTiles(entry.word) };
  }

  function svgMedallion() {
    let petals = "";
    for (let i = 0; i < 16; i++) {
      const angle = (360 / 16) * i;
      petals += `<ellipse cx="0" cy="-58" rx="7" ry="20" fill="none" stroke="url(#medGrad)" stroke-width="1.4" opacity="0.85" transform="rotate(${angle})"/>`;
    }
    let boteh = "";
    [[28,28,-20],[172,28,20],[28,172,-160],[172,172,160]].forEach(([cx,cy,rot]) => {
      boteh += `<g transform="translate(${cx},${cy}) rotate(${rot}) scale(0.55)" opacity="0.8">
        <path d="M0,-20 C10,-20 15,-8 9,6 C6,14 2,18 0,22 C-2,18 -6,14 -9,6 C-15,-8 -10,-20 0,-20 Z" fill="none" stroke="var(--gold)" stroke-width="1.6"/>
      </g>`;
    });
    return `<svg width="200" height="200" viewBox="0 0 200 200" class="kj-arch">
      <defs><linearGradient id="medGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="var(--gold)"/><stop offset="100%" stop-color="var(--turquoise)"/>
      </linearGradient></defs>
      <g transform="translate(100,100)">
        ${petals}
        <circle r="40" fill="none" stroke="var(--gold)" stroke-width="1.6"/>
        <circle r="30" fill="none" stroke="var(--turquoise)" stroke-width="1.2" opacity="0.8"/>
        <circle r="5" fill="var(--gold)"/>
      </g>
      ${boteh}
    </svg>`;
  }

  function svgBotehBand() {
    let items = "";
    for (let i = 0; i < 10; i++) {
      const cx = 12 + i * 24;
      const rot = i % 2 === 0 ? -12 : 12;
      items += `<g transform="translate(${cx},10) rotate(${rot}) scale(0.4)" stroke="var(--gold)" stroke-width="1.6" fill="none" opacity="0.85">
        <path d="M0,-20 C10,-20 15,-8 9,6 C6,14 2,18 0,22 C-2,18 -6,14 -9,6 C-15,-8 -10,-20 0,-20 Z"/>
      </g>`;
    }
    return `<svg viewBox="0 0 240 20" width="100%" height="20" preserveAspectRatio="xMidYMid meet">${items}</svg>`;
  }

  function starFieldHTML() {
    let seed = 42;
    const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
    let out = "";
    for (let i = 0; i < 36; i++) {
      const top = rnd() * 100, left = rnd() * 100, size = 1 + rnd() * 2, delay = rnd() * 3;
      out += `<div class="star" style="top:${top}%;left:${left}%;width:${size}px;height:${size}px;animation-delay:${delay}s;"></div>`;
    }
    return `<div class="star-field">${out}</div>`;
  }

  // ---------------------------------------------------------------------
  // app state
  // ---------------------------------------------------------------------
  const state = {
    view: "menu", // menu | howto | game
    highScore: 0,
    sessionWords: WORD_BANK,
    roundIndex: 0,
    round: null,
    picked: [],
    score: 0,
    streak: 0,
    hintsLeft: 3,
    status: "playing", // playing | correct | wrong
    revealedHint: false,
  };

  const root = document.getElementById("app");
  let gameTouchedThisSession = false; // becomes true once startGame/resumeGame runs

  // ---------------------------------------------------------------------
  // persistence — best score + resumable in-progress game, via localStorage
  // ---------------------------------------------------------------------
  const STORAGE_KEY = "vazhebafi_save_v1";

  function loadSave() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null; // storage unavailable (private mode, etc.) — fail silently
    }
  }

  function saveProgress() {
    try {
      if (!gameTouchedThisSession) {
        // Nothing has happened yet this session (e.g. the very first render
        // on page load) — only persist highScore, leave any existing saved
        // game slot exactly as it was so "ادامه‌ی بازی" keeps working.
        const existing = loadSave() || {};
        localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({ ...existing, highScore: state.highScore })
        );
        return;
      }
      const hasActiveGame =
        !!state.round && state.roundIndex < state.sessionWords.length;
      const payload = {
        highScore: state.highScore,
        game: hasActiveGame
          ? {
              sessionWords: state.sessionWords,
              roundIndex: state.roundIndex,
              round: state.round,
              picked: state.picked,
              score: state.score,
              streak: state.streak,
              hintsLeft: state.hintsLeft,
              status: state.status,
              revealedHint: state.revealedHint,
            }
          : null,
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch (e) {
      // storage unavailable — nothing we can do, fail silently
    }
  }

  function getResumableGame() {
    const saved = loadSave();
    return saved && saved.game ? saved.game : null;
  }

  function resumeGame() {
    const g = getResumableGame();
    if (!g) return;
    gameTouchedThisSession = true;
    state.view = "game";
    state.sessionWords = g.sessionWords;
    state.roundIndex = g.roundIndex;
    state.round = g.round;
    state.picked = g.picked;
    state.score = g.score;
    state.streak = g.streak;
    state.hintsLeft = g.hintsLeft;
    state.status = g.status;
    state.revealedHint = g.revealedHint;
    render();
  }

  // restore best score on startup
  (function initFromStorage() {
    const saved = loadSave();
    if (saved && typeof saved.highScore === "number") {
      state.highScore = saved.highScore;
    }
  })();

  function currentWord() {
    return state.picked
      .map((id) => state.round.tiles.find((t) => t.id === id)?.ch)
      .join("");
  }

  function checkAnswer() {
    const cw = currentWord();
    if (cw.length === state.round.word.length && cw.length > 0) {
      if (cw === state.round.word) {
        state.status = "correct";
        state.score += 10 + (state.revealedHint ? 0 : 5);
        state.streak += 1;
      } else {
        state.status = "wrong";
        state.streak = 0;
      }
    }
  }

  function ambientOrbsHTML() {
    return `
      <div class="ambient-bg">
        <div class="ambient-orb" style="width:180px;height:180px;top:8%;left:-6%;background:var(--turquoise);animation-delay:0s;"></div>
        <div class="ambient-orb" style="width:140px;height:140px;bottom:10%;right:-8%;background:var(--gold);animation-delay:4s;"></div>
        <div class="ambient-orb" style="width:110px;height:110px;top:55%;left:70%;background:var(--terracotta);animation-delay:8s;"></div>
      </div>
    `;
  }

  function cardMotifHTML() {
    return `
      <div class="card-motif">
        <svg width="90" height="120" viewBox="0 0 90 120" style="top:-10px;right:-16px;">
          <path d="M45,4 C68,4 78,34 62,58 C54,70 48,80 45,92 C42,80 36,70 28,58 C12,34 22,4 45,4 Z" fill="none" stroke="var(--ink)" stroke-width="3"/>
        </svg>
        <svg width="70" height="95" viewBox="0 0 70 95" style="bottom:-14px;left:-12px;animation-delay:6s;">
          <path d="M35,3 C53,3 61,27 48,46 C42,55 38,63 35,72 C32,63 28,55 22,46 C9,27 17,3 35,3 Z" fill="none" stroke="var(--ink)" stroke-width="2.5"/>
        </svg>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // render: MENU
  // ---------------------------------------------------------------------
  function renderMenu() {
    const resumable = getResumableGame();
    root.innerHTML = `
      <div class="menu-scene">
        ${starFieldHTML()}
        <div class="medallion-wrap">
          ${svgMedallion()}
          <h1 class="game-title">واژه‌بافی</h1>
        </div>
        <p class="game-subtitle">حروف را بچین، کلمه‌ی فارسی را پیدا کن</p>
        ${state.highScore > 0 ? `
          <div class="highscore-badge">${ICON.star("var(--turquoise)")} بهترین امتیاز: ${state.highScore}</div>
        ` : ""}
        ${resumable ? `
          <button class="btn-primary" data-action="resume">${ICON.play()} ادامه‌ی بازی (مرحله ${resumable.roundIndex + 1})</button>
          <button class="btn-secondary" data-action="play">شروع بازی جدید</button>
        ` : `
          <button class="btn-primary" data-action="play">${ICON.play()} شروع بازی</button>
        `}
        <button class="btn-secondary" data-action="howto">${ICON.help()} راهنمای بازی</button>
        <div class="menu-footer">
          ${svgBotehBand()}
          <p class="menu-footer-text">${WORD_BANK.length} کلمه · نسخه‌ی آزمایشی</p>
        </div>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // render: HOW TO
  // ---------------------------------------------------------------------
  function renderHowTo() {
    const steps = [
      "یک راهنما برای کلمه‌ی مخفی نشان داده می‌شود.",
      "با لمس حروف به‌ترتیب، کلمه را بساز.",
      "برای حذف آخرین حرف، روی آن در ردیف بالا بزن.",
      "اگر گیر کردی، از چراغ راهنما استفاده کن (تعداد محدود).",
      "با هر پاسخ درست امتیاز و استریک می‌گیری.",
    ];
    root.innerHTML = `
      <div class="screen-center">
        ${ambientOrbsHTML()}
        <div class="card">
          ${cardMotifHTML()}
          <button class="back-link" data-action="back-to-menu">${ICON.back()} بازگشت</button>
          <h2 class="howto-title">چگونه بازی کنیم؟</h2>
          <ol class="howto-list">
            ${steps.map((s, i) => `<li><span class="howto-num">${i + 1}</span><span>${s}</span></li>`).join("")}
          </ol>
          <button class="btn-ok" data-action="back-to-menu">باشه، فهمیدم</button>
        </div>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // render: GAME
  // ---------------------------------------------------------------------
  function renderGame() {
    const isComplete = state.roundIndex >= state.sessionWords.length;

    if (isComplete) {
      root.innerHTML = `
        <div class="screen-center">
          <div class="game-card">
            <div class="complete-wrap">
              <div style="color:var(--terracotta)">${ICON.trophy()}</div>
              <h2 class="complete-title">تبریک! بازی تمام شد</h2>
              <p class="complete-sub">${state.sessionWords.length} کلمه را حل کردی</p>
              <div class="complete-score">${ICON.star("var(--terracotta)")} ${state.score} امتیاز</div>
              <div class="complete-actions">
                <button class="btn-flex btn-next" data-action="restart">شروع دوباره</button>
                <button class="btn-flex btn-clear" data-action="back-to-menu">بازگشت به منو</button>
              </div>
            </div>
            <div class="ad-slot">جای تبلیغ بنری (AdMob / Bazaar Ads)</div>
          </div>
        </div>
      `;
      return;
    }

    const round = state.round;
    const slotsHTML = round.word
      .split("")
      .map((_, i) => {
        const id = state.picked[i];
        const tile = round.tiles.find((t) => t.id === id);
        return `<div class="slot ${tile ? "filled" : ""}" data-action="${i === state.picked.length - 1 ? "undo" : ""}">${tile ? tile.ch : ""}</div>`;
      })
      .join("");

    const tilesHTML = round.tiles
      .map(
        (t) => `<button class="tile" data-action="pick" data-tile-id="${t.id}" ${t.used || state.status !== "playing" ? "disabled" : ""}>${t.ch}</button>`
      )
      .join("");

    let statusHTML = "";
    if (state.status === "correct") {
      statusHTML = `<div class="status-banner status-correct">${ICON.check()} درست بود!</div>`;
    } else if (state.status === "wrong") {
      statusHTML = `<div class="status-banner status-wrong">اشتباس، دوباره تلاش کن</div>`;
    }

    let actionHTML = "";
    if (state.status === "wrong") {
      actionHTML = `<button class="btn-flex btn-retry" data-action="retry">${ICON.undo()} تلاش دوباره</button>`;
    } else if (state.status === "correct") {
      actionHTML = `<button class="btn-flex btn-next" data-action="next">مرحله بعد ←</button>`;
    } else {
      actionHTML = `<button class="btn-flex btn-clear" data-action="clear" ${state.picked.length === 0 ? "disabled" : ""}>${ICON.undo()} پاک کردن</button>`;
    }

    root.innerHTML = `
      <div class="screen-center">
        ${ambientOrbsHTML()}
        <div class="game-card">
          ${cardMotifHTML()}
          <button class="back-link" data-action="back-to-menu" style="opacity:.6">${ICON.back()} منو</button>
          <div class="game-topbar">
            <div class="stat-score">${ICON.star("var(--terracotta)")} ${state.score}</div>
            <div class="stat-streak">${ICON.flame(state.streak > 0 ? "var(--red)" : "none")} ${state.streak}</div>
            <button class="hint-btn" data-action="hint" ${state.status !== "playing" ? "disabled" : ""}>${state.hintsLeft > 0 ? `${ICON.bulb()} ${state.hintsLeft}` : "🎬 راهنمای رایگان"}</button>
          </div>
          <p class="hint-text">${state.revealedHint ? round.hint : ""}</p>
          <div class="answer-slots">${slotsHTML}</div>
          ${statusHTML}
          <div class="tiles-row">${tilesHTML}</div>
          <div class="action-row">${actionHTML}</div>
          <p class="progress-text">مرحله ${state.roundIndex + 1} از ${state.sessionWords.length}</p>
          <div class="ad-slot">جای تبلیغ بنری (AdMob / Bazaar Ads)</div>
        </div>
      </div>
    `;
  }

  function render() {
    if (state.view === "menu") renderMenu();
    else if (state.view === "howto") renderHowTo();
    else renderGame();
    saveProgress();
    mountBanners();
  }

  function mountBanners() {
    if (!window.Ads) return;
    document.querySelectorAll(".ad-slot").forEach((el) => window.Ads.renderBannerInto(el));
  }

  // ---------------------------------------------------------------------
  // actions
  // ---------------------------------------------------------------------
  function startGame() {
    gameTouchedThisSession = true;
    state.view = "game";
    state.sessionWords = shuffleWordOrder();
    state.roundIndex = 0;
    state.round = makeRound(0, state.sessionWords);
    state.picked = [];
    state.score = 0;
    state.streak = 0;
    state.hintsLeft = 3;
    state.status = "playing";
    state.revealedHint = false;
    render();
  }

  function goToMenu() {
    if (state.view === "game" && state.score > state.highScore) {
      state.highScore = state.score;
    }
    state.view = "menu";
    render();
  }

  function pickTile(tileId) {
    if (state.status !== "playing") return;
    const tile = state.round.tiles.find((t) => t.id === tileId);
    if (!tile || tile.used) return;
    tile.used = true;
    state.picked.push(tileId);
    checkAnswer();
    render();
  }

  function undoLast() {
    if (state.status !== "playing" || state.picked.length === 0) return;
    const lastId = state.picked.pop();
    const tile = state.round.tiles.find((t) => t.id === lastId);
    if (tile) tile.used = false;
    render();
  }

  function resetPicks() {
    state.picked = [];
    state.round.tiles = freshTiles(state.round.word);
    render();
  }

  function tryAgain() {
    state.picked = [];
    state.round.tiles = freshTiles(state.round.word);
    state.status = "playing";
    render();
  }

  function nextRound() {
    const next = state.roundIndex + 1;
    if (next >= state.sessionWords.length) {
      state.roundIndex = next;
      if (state.score > state.highScore) state.highScore = state.score;
      render();
      return;
    }

    const advance = () => {
      state.roundIndex = next;
      state.round = makeRound(next, state.sessionWords);
      state.picked = [];
      state.status = "playing";
      state.revealedHint = false;
      render();
    };

    if (window.Ads && window.Ads.shouldShowInterstitial(next)) {
      window.Ads.showInterstitial(advance);
    } else {
      advance();
    }
  }

  function useHint() {
    if (state.status !== "playing") return;
    if (state.hintsLeft > 0) {
      state.hintsLeft -= 1;
      state.revealedHint = true;
      render();
      return;
    }
    // بدون راهنمای رایگان مونده — پیشنهاد تبلیغ جایزه‌ای برای یه راهنمای اضافه
    if (window.Ads) {
      window.Ads.showRewarded(() => {
        state.revealedHint = true;
        render();
      });
    }
  }

  // ---------------------------------------------------------------------
  // event delegation
  // ---------------------------------------------------------------------
  document.addEventListener("click", (e) => {
    const el = e.target.closest("[data-action]");
    if (!el) return;
    const action = el.getAttribute("data-action");
    switch (action) {
      case "play": startGame(); break;
      case "resume": resumeGame(); break;
      case "howto": state.view = "howto"; render(); break;
      case "back-to-menu": goToMenu(); break;
      case "pick": pickTile(el.getAttribute("data-tile-id")); break;
      case "undo": undoLast(); break;
      case "clear": resetPicks(); break;
      case "retry": tryAgain(); break;
      case "next": nextRound(); break;
      case "restart": startGame(); break;
      case "hint": useHint(); break;
      default: break;
    }
  });

  if (window.Ads) window.Ads.initAds();
  render();
})();
