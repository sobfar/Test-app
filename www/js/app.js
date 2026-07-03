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
    person: () => `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></svg>`,
    coin: () => `<svg width="20" height="20" viewBox="0 0 24 24">
      <defs><radialGradient id="gradCoin" cx="35%" cy="30%" r="75%">
        <stop offset="0%" stop-color="#FFF3C4"/><stop offset="55%" stop-color="#F0C24A"/><stop offset="100%" stop-color="#B9822A"/>
      </radialGradient></defs>
      <circle cx="12" cy="12" r="9.4" fill="url(#gradCoin)" stroke="#8A5F1B" stroke-width="1"/>
      <circle cx="12" cy="12" r="7.2" fill="none" stroke="#8A5F1B" stroke-width="0.7" opacity="0.5"/>
      <path d="M12 7.4l1.3 2.6 2.9.4-2.1 2.1.5 2.9-2.6-1.4-2.6 1.4.5-2.9-2.1-2.1 2.9-.4z" fill="#fff" opacity="0.92"/>
    </svg>`,
    heart: () => `<svg width="18" height="18" viewBox="0 0 24 24">
      <defs><linearGradient id="gradHeart" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#FF8A80"/><stop offset="100%" stop-color="#C23B3B"/>
      </linearGradient></defs>
      <path d="M12 21s-7.5-4.6-10-9.3C.4 8 2.4 4.5 6 4.5c2 0 3.6 1.1 6 3.6 2.4-2.5 4-3.6 6-3.6 3.6 0 5.6 3.5 4 7.2C19.5 16.4 12 21 12 21z" fill="url(#gradHeart)"/>
      <path d="M7.2 7.6c-1.4 0-2.4 1-2.4 2.3" stroke="#FFC7C0" stroke-width="1.1" fill="none" stroke-linecap="round" opacity="0.75"/>
    </svg>`,
    gear: () => `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3.2"/><path d="M19.4 13a7.6 7.6 0 000-2l2-1.5-2-3.4-2.3.9a7.7 7.7 0 00-1.7-1L15 3.5h-4l-.4 2.5a7.7 7.7 0 00-1.7 1l-2.3-.9-2 3.4L6.6 11a7.6 7.6 0 000 2l-2 1.5 2 3.4 2.3-.9c.5.4 1.1.7 1.7 1l.4 2.5h4l.4-2.5c.6-.3 1.2-.6 1.7-1l2.3.9 2-3.4-2-1.5z"/></svg>`,
    bookmark: () => `<svg width="22" height="22" viewBox="0 0 24 24">
      <defs><linearGradient id="gradBookmark" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#FCE7A0"/><stop offset="100%" stop-color="#C4901F"/>
      </linearGradient></defs>
      <path d="M6 3h12v18l-6-4-6 4V3z" fill="url(#gradBookmark)" stroke="#8A5F1B" stroke-width="1"/>
    </svg>`,
    gift: () => `<svg width="30" height="30" viewBox="0 0 24 24">
      <defs>
        <linearGradient id="gradGiftBox" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#4FD8A8"/><stop offset="100%" stop-color="#1E9E74"/></linearGradient>
        <linearGradient id="gradGiftRibbon" x1="0" y1="0" x2="1" y2="0"><stop offset="0%" stop-color="#FCE7A0"/><stop offset="100%" stop-color="#C4901F"/></linearGradient>
      </defs>
      <rect x="4" y="10" width="16" height="10" rx="1.5" fill="url(#gradGiftBox)" stroke="#0E6D4E" stroke-width="0.8"/>
      <rect x="3.5" y="7.6" width="17" height="3.6" rx="1" fill="url(#gradGiftRibbon)" stroke="#8A5F1B" stroke-width="0.5"/>
      <rect x="10.8" y="7.6" width="2.4" height="12.4" fill="url(#gradGiftRibbon)"/>
      <path d="M12 7.4c-1-2.8-2.8-4-4.2-3.4-1.2.5-1.2 2.2.3 2.9 1 .4 2.5.5 3.9.5zM12 7.4c1-2.8 2.8-4 4.2-3.4 1.2.5 1.2 2.2-.3 2.9-1 .4-2.5.5-3.9.5z" fill="url(#gradGiftRibbon)" stroke="#8A5F1B" stroke-width="0.4"/>
    </svg>`,
    calendarStar: () => `<svg width="30" height="30" viewBox="0 0 24 24">
      <defs><linearGradient id="gradCalBody" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#FFFDF5"/><stop offset="100%" stop-color="#EFE6C8"/></linearGradient></defs>
      <rect x="3.5" y="5" width="17" height="15" rx="2" fill="url(#gradCalBody)" stroke="#B79A55" stroke-width="1"/>
      <rect x="3.5" y="5" width="17" height="4" rx="2" fill="#D8C282"/>
      <rect x="7" y="3" width="1.8" height="4" rx="0.9" fill="#8A5F1B"/>
      <rect x="15.2" y="3" width="1.8" height="4" rx="0.9" fill="#8A5F1B"/>
      <path d="M12 11l1.1 2.2 2.4.35-1.75 1.7.4 2.35L12 16.4l-2.15 1.2.4-2.35-1.75-1.7 2.4-.35z" fill="#17998C"/>
    </svg>`,
    leaderboard: () => `<svg width="30" height="30" viewBox="0 0 24 24">
      <defs>
        <linearGradient id="gradLBGold" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#FCE7A0"/><stop offset="100%" stop-color="#C4901F"/></linearGradient>
        <linearGradient id="gradLBTeal" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#6BF0DD"/><stop offset="100%" stop-color="#17998C"/></linearGradient>
      </defs>
      <rect x="2.5" y="13" width="5.5" height="7" rx="1" fill="url(#gradLBTeal)"/>
      <text x="5.2" y="18.2" font-family="Tahoma, sans-serif" font-size="4" font-weight="800" fill="#06231F" text-anchor="middle">2</text>
      <rect x="9.2" y="8.5" width="5.5" height="11.5" rx="1" fill="url(#gradLBGold)"/>
      <text x="12" y="16.5" font-family="Tahoma, sans-serif" font-size="4.4" font-weight="800" fill="#5b3d10" text-anchor="middle">1</text>
      <rect x="16" y="14.5" width="5.5" height="5.5" rx="1" fill="url(#gradLBTeal)"/>
      <text x="18.75" y="18.7" font-family="Tahoma, sans-serif" font-size="4" font-weight="800" fill="#06231F" text-anchor="middle">3</text>
    </svg>`,
    basket: () => `<svg width="30" height="30" viewBox="0 0 24 24">
      <defs><linearGradient id="gradBasket" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#6BF0DD"/><stop offset="100%" stop-color="#17998C"/></linearGradient></defs>
      <path d="M8 9a4 4 0 018 0" fill="none" stroke="#0E6D63" stroke-width="1.6"/>
      <path d="M4 9h16l-1.6 10.2a2 2 0 01-2 1.7H7.6a2 2 0 01-2-1.7L4 9z" fill="url(#gradBasket)" stroke="#0E6D63" stroke-width="1"/>
      <path d="M9 12v5M12 12v5M15 12v5" stroke="#0E6D63" stroke-width="1" opacity="0.5" stroke-linecap="round"/>
    </svg>`,
    sparkle: () => `<svg class="icon-inline" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M12 2l1.6 6.4L20 10l-6.4 1.6L12 18l-1.6-6.4L4 10l6.4-1.6z"/></svg>`,
    // ---- rich gold/teal variants used only on the menu stats bar; the
    // plain star()/flame()/trophy() above stay untouched since they're
    // reused on the parchment-colored game/store/complete screens where
    // they need to inherit currentColor instead of a fixed gradient. ----
    starGold: () => `<svg width="22" height="22" viewBox="0 0 24 24">
      <defs><linearGradient id="gradStarGold" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#FFF3C4"/><stop offset="60%" stop-color="#F0C24A"/><stop offset="100%" stop-color="#B9822A"/></linearGradient></defs>
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 21 12 17.27 5.82 21 7 14.14l-5-4.87 6.91-1.01L12 2z" fill="url(#gradStarGold)" stroke="#8A5F1B" stroke-width="0.6"/>
    </svg>`,
    flameGold: () => `<svg width="24" height="24" viewBox="0 0 24 24">
      <defs>
        <linearGradient id="gradFlameOuter" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#FFB199"/><stop offset="55%" stop-color="#F0623C"/><stop offset="100%" stop-color="#B23A2A"/></linearGradient>
        <linearGradient id="gradFlameInner" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#FFE9A8"/><stop offset="100%" stop-color="#F5B23C"/></linearGradient>
      </defs>
      <path d="M12 2s-6 6-6 12a6 6 0 0012 0c0-3-2-4-2-6 0 2-1 3-2 3 0-3 3-5 3-9-2 1-5 3-5 0z" fill="url(#gradFlameOuter)"/>
      <path d="M12 10c-1.6 2-2.4 3.6-2.4 5.2a2.4 2.4 0 004.8 0c0-1.1-.6-1.7-.9-2.5.4.1.7.1 1 0-.3-1-1.6-1.9-2.5-2.7z" fill="url(#gradFlameInner)"/>
    </svg>`,
    trophyGold: () => `<svg width="40" height="40" viewBox="0 0 24 24">
      <defs><linearGradient id="gradTrophy" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#FFF3C4"/><stop offset="55%" stop-color="#F0C24A"/><stop offset="100%" stop-color="#B9822A"/></linearGradient></defs>
      <path d="M8 21h8M12 17v4M7 4h10v4a5 5 0 01-10 0V4z" fill="url(#gradTrophy)" stroke="#8A5F1B" stroke-width="1"/>
      <path d="M7 5H4a3 3 0 003 3M17 5h3a3 3 0 01-3 3" fill="none" stroke="#8A5F1B" stroke-width="1.2"/>
      <path d="M5 1l.6 1.6L7 3l-1.4.6L5 5l-.6-1.4L3 3l1.4-.4z" fill="#F0C24A" opacity="0.85"/>
      <path d="M19 1.5l.5 1.3 1.2.4-1.2.4-.5 1.3-.5-1.3-1.2-.4 1.2-.4z" fill="#F0C24A" opacity="0.85"/>
    </svg>`,
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

  // Re-shuffles the on-screen letter tile order (does not change which
  // tiles are already picked/used). Purely a convenience/UX action.
  function shuffleTiles() {
    if (!state.round || state.status !== "playing") return;
    state.round.tiles = shuffle(state.round.tiles);
  }

  function shuffleWordOrder() {
    return shuffle(WORD_BANK);
  }

  function makeRound(index, wordList) {
    const entry = wordList[index % wordList.length];
    return { word: entry.word, hint: entry.hint, tiles: freshTiles(entry.word) };
  }

  function svgMedallion() {
    // Bold alternating gold/teal petals (not thin sunburst lines) — a
    // pointed leaf shape repeated around the center, echoing the reference.
    const petalPath = "M0,-10 C13,-10 18,-38 11,-66 C7,-80 3,-90 0,-96 C-3,-90 -7,-80 -11,-66 C-18,-38 -13,-10 0,-10 Z";
    let petals = "";
    for (let i = 0; i < 8; i++) {
      const angle = 22.5 + (360 / 8) * i;
      const gold = i % 2 === 0;
      petals += `<path d="${petalPath}" fill="${gold ? "url(#medGoldFill)" : "url(#medTealFill)"}" stroke="${gold ? "var(--gold)" : "var(--turquoise)"}" stroke-width="1.2" opacity="0.9" transform="rotate(${angle}) scale(0.72)"/>`;
    }
    let boteh = "";
    [[30,30,-20],[190,30,20],[30,190,-160],[190,190,160]].forEach(([cx,cy,rot]) => {
      boteh += `<g transform="translate(${cx},${cy}) rotate(${rot}) scale(0.5)" opacity="0.55">
        <path d="M0,-20 C10,-20 15,-8 9,6 C6,14 2,18 0,22 C-2,18 -6,14 -9,6 C-15,-8 -10,-20 0,-20 Z" fill="none" stroke="var(--gold)" stroke-width="1.6"/>
      </g>`;
    });
    const sparkles = [
      [40, 46, 5, 0.9], [178, 40, 4, 0.8], [186, 168, 5, 0.85], [34, 172, 4, 0.7], [110, 20, 3.5, 0.75],
    ];
    let sparkleHTML = "";
    sparkles.forEach(([sx, sy, s, op], i) => {
      sparkleHTML += `<path d="M0,-1 L${0.28 * s},${-0.28 * s} L${s},0 L${0.28 * s},${0.28 * s} L0,${s} L${-0.28 * s},${0.28 * s} L${-s},0 L${-0.28 * s},${-0.28 * s} Z" fill="var(--gold)" opacity="${op}" transform="translate(${sx},${sy})" class="star" style="animation-delay:${i * 0.4}s;"/>`;
    });
    return `<svg width="220" height="220" viewBox="0 0 220 220" class="kj-arch">
      <defs>
        <radialGradient id="medGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="var(--gold)" stop-opacity="0.5"/>
          <stop offset="100%" stop-color="var(--gold)" stop-opacity="0"/>
        </radialGradient>
        <linearGradient id="medGoldFill" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stop-color="var(--gold)" stop-opacity="0.05"/>
          <stop offset="100%" stop-color="var(--gold)" stop-opacity="0.55"/>
        </linearGradient>
        <linearGradient id="medTealFill" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stop-color="var(--turquoise)" stop-opacity="0.05"/>
          <stop offset="100%" stop-color="var(--turquoise)" stop-opacity="0.55"/>
        </linearGradient>
      </defs>
      <circle cx="110" cy="110" r="70" fill="url(#medGlow)"/>
      <g transform="translate(110,110)">
        ${petals}
        <circle r="38" fill="none" stroke="var(--gold)" stroke-width="1.6"/>
        <circle r="28" fill="none" stroke="var(--turquoise)" stroke-width="1.2" opacity="0.8"/>
        <circle r="5" fill="var(--gold)"/>
      </g>
      ${boteh}
      ${sparkleHTML}
    </svg>`;
  }

  // Decorative 3D letter tiles clustered over the medallion, echoing the
  // look of a physical word-tile logo. Purely cosmetic — the letters here
  // are a stylised mark, not a gameplay word.
  function logoTilesHTML() {
    const tiles = [
      { ch: "ا", top: 24, left: 62, rot: -5 },
      { ch: "و", top: 10, left: 116, rot: 5 },
      { ch: "ژ", top: 116, left: 24, rot: -8 },
      { ch: "ه", top: 124, left: 82, rot: 0, accent: true },
      { ch: "ب", top: 116, left: 140, rot: 8 },
    ];
    return `
      <div class="logo-tiles">
        ${tiles
          .map(
            (t) => `<div class="tile-3d${t.accent ? " accent" : ""}" style="top:${t.top}px;left:${t.left}px;transform:rotate(${t.rot}deg);">${t.ch}</div>`
          )
          .join("")}
      </div>
    `;
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

    // Large faint Persian letters that slowly fade in and out ("محو و نیمه‌محو"),
    // echoing the ghost-letter texture scattered behind the reference logo.
    // Most sit beside the medallion (left/right bands, upper half); a few
    // are scattered lower down for overall texture.
    const glyphs = ["د", "س", "ق", "و", "ت", "ن", "م", "ح", "ر", "ل", "ک", "ژ"];
    const letterColors = ["rgba(232,185,75,0.4)", "rgba(150,110,220,0.4)", "rgba(90,150,230,0.4)"];
    let letters = "";
    for (let i = 0; i < 22; i++) {
      let top, left;
      if (i < 16) {
        // beside the logo: left band or right band, upper half of the screen
        const rightSide = rnd() > 0.5;
        left = rightSide ? 78 + rnd() * 20 : rnd() * 20;
        top = 4 + rnd() * 46;
      } else {
        top = rnd() * 96;
        left = rnd() * 92;
      }
      const size = 34 + rnd() * 30;
      const dur = 5 + rnd() * 5, delay = rnd() * 5;
      const ch = glyphs[Math.floor(rnd() * glyphs.length)];
      const color = letterColors[Math.floor(rnd() * letterColors.length)];
      letters += `<span class="bg-letter" style="top:${top}%;left:${left}%;font-size:${size}px;color:${color};animation-duration:${dur}s;animation-delay:${delay}s;">${ch}</span>`;
    }

    // Thin golden swirl lines with uneven, staggered brightness (each pulses
    // on its own timer so they never glow in sync — "جایی پررنگ جایی کم‌رنگ").
    const lines = [
      { top: 6, left: 68, w: 130, rot: -18 },
      { top: 14, left: 4, w: 100, rot: 24 },
      { top: 78, left: 72, w: 150, rot: 10 },
      { top: 88, left: 6, w: 110, rot: -12 },
      { top: 34, left: 78, w: 90, rot: 30 },
      { top: 46, left: 2, w: 95, rot: -26 },
      { top: 60, left: 60, w: 120, rot: -6 },
      { top: 22, left: 40, w: 85, rot: 14 },
      { top: 68, left: 24, w: 100, rot: 20 },
    ];
    let goldLines = "";
    lines.forEach((l, i) => {
      const dur = 4.5 + rnd() * 3.5, delay = rnd() * 4;
      goldLines += `
        <svg class="bg-goldline" style="top:${l.top}%;left:${l.left}%;width:${l.w}px;transform:rotate(${l.rot}deg);animation-duration:${dur}s;animation-delay:${delay}s;" viewBox="0 0 120 40">
          <path d="M2 20c15-18 30-18 40-2s28 20 40 4 25-16 36-2" fill="none" stroke="var(--gold)" stroke-width="0.7" stroke-linecap="round"/>
        </svg>
      `;
    });

    return `<div class="star-field">${out}${letters}${goldLines}</div>`;
  }

  // ---------------------------------------------------------------------
  // app state
  // ---------------------------------------------------------------------
  const state = {
    view: "menu", // menu | howto | game | store
    highScore: 0,
    adsRemoved: false,
    bonusHints: 0,
    sessionWords: WORD_BANK,
    roundIndex: 0,
    round: null,
    picked: [],
    score: 0,
    streak: 0,
    hintsLeft: 3,
    status: "playing", // playing | correct | wrong
    revealedHint: false,
    settingsOpen: false,
  };

  const root = document.getElementById("app");
  const modalRoot = document.getElementById("modal-root");
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
        // on page load) — only persist highScore/purchases, leave any
        // existing saved game slot exactly as it was so "ادامه‌ی بازی" keeps working.
        const existing = loadSave() || {};
        localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({
            ...existing,
            highScore: state.highScore,
            adsRemoved: state.adsRemoved,
            bonusHints: state.bonusHints,
          })
        );
        return;
      }
      const hasActiveGame =
        !!state.round && state.roundIndex < state.sessionWords.length;
      const payload = {
        highScore: state.highScore,
        adsRemoved: state.adsRemoved,
        bonusHints: state.bonusHints,
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

  // restore best score + purchases on startup
  (function initFromStorage() {
    const saved = loadSave();
    if (saved && typeof saved.highScore === "number") {
      state.highScore = saved.highScore;
    }
    if (saved && typeof saved.adsRemoved === "boolean") {
      state.adsRemoved = saved.adsRemoved;
    }
    if (saved && typeof saved.bonusHints === "number") {
      state.bonusHints = saved.bonusHints;
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
        if (window.FX) {
          window.FX.playCorrect();
          window.FX.vibrate("medium");
        }
      } else {
        state.status = "wrong";
        state.streak = 0;
        if (window.FX) {
          window.FX.playWrong();
          window.FX.vibrate("heavy");
        }
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
  // NOTE on decorative-only stats below (coin/heart/level/streak-days):
  // these systems (currency, lives, XP levels, daily streak) don't exist
  // in the game yet. Per product decision, the menu is styled to already
  // include them visually, but they're inert placeholders (no data-action,
  // static numbers) until real progression/economy systems are built —
  // same "mock now, wire up later" pattern already used in ads.js/iap.js.
  function renderMenu() {
    const resumable = getResumableGame();
    const currentStage = resumable ? resumable.roundIndex + 1 : 0;

    root.innerHTML = `
      <div class="menu-scene menu-scene-v2">
        ${starFieldHTML()}

        <div class="menu-topbar">
          <div class="profile-badge">
            <img class="avatar-circle" src="assets/ic-avatar.png" alt="">
            <div class="profile-info">
              <div class="profile-name">واژه‌بافی</div>
              <div class="profile-level-row">
                <span class="profile-level-text">سطح ۱</span>
                <div class="level-bar"><div class="level-bar-fill" style="width:8%"></div></div>
              </div>
            </div>
          </div>
          <div class="topbar-right">
            <div class="pill-stat coin-pill"><img class="pill-icon-img" src="assets/ic-coin.png" alt="">۰<span class="pill-add">+</span></div>
            <div class="pill-stat heart-pill"><img class="pill-icon-img" src="assets/ic-heart.png" alt="">∞<span class="pill-add">+</span></div>
            <button class="icon-btn-round" data-action="settings-open" aria-label="تنظیمات"><img class="pill-icon-img" src="assets/ic-gear.png" alt=""></button>
          </div>
        </div>

        <div class="logo-medallion-wrap">
          <img class="logo-medallion-img" src="assets/logo-medallion.png" alt="واژه‌بافی">
        </div>
        <p class="game-subtitle">🌿 حروف را بچین، کلمه‌ی فارسی بساز! 🌿</p>

        <div class="stats-bar-4">
          <div class="stat-cell">
            <img class="stat-icon-img" src="assets/ic-trophy.png" alt="">
            <span class="stat-label">بهترین امتیاز</span>
            <span class="stat-num">${state.highScore}</span>
          </div>
          <div class="stat-cell">
            <img class="stat-icon-img" src="assets/ic-bookmark.png" alt="">
            <span class="stat-label">مرحله فعلی</span>
            <span class="stat-num">${currentStage}</span>
          </div>
          <div class="stat-cell">
            <img class="stat-icon-img" src="assets/ic-flame.png" alt="">
            <span class="stat-label">روزهای متوالی</span>
            <span class="stat-num">۰</span>
          </div>
          <div class="stat-cell">
            <img class="stat-icon-img" src="assets/ic-star.png" alt="">
            <span class="stat-label">کل کلمات شما</span>
            <span class="stat-num">${WORD_BANK.length}</span>
          </div>
        </div>

        ${resumable ? `
          <button class="btn-primary" data-action="resume">${ICON.play()} ادامه‌ی بازی (مرحله ${resumable.roundIndex + 1})</button>
          <button class="btn-secondary" data-action="play">${ICON.sparkle()} شروع بازی جدید</button>
        ` : `
          <button class="btn-primary" data-action="play">${ICON.play()} شروع بازی</button>
        `}
        <button class="btn-secondary" data-action="howto">${ICON.help()} راهنمای بازی</button>

        <div class="icon-grid">
          <button class="icon-grid-btn">
            <span class="notif-dot"></span>
            <img class="grid-icon-img" src="assets/ic-gift.png" alt="">
            <span class="icon-grid-label">جایزه روزانه</span>
          </button>
          <button class="icon-grid-btn">
            <span class="notif-dot"></span>
            <img class="grid-icon-img" src="assets/ic-calendar.png" alt="">
            <span class="icon-grid-label">چالش روز</span>
          </button>
          <button class="icon-grid-btn">
            <img class="grid-icon-img" src="assets/ic-leaderboard.png" alt="">
            <span class="icon-grid-label">جدول رتبه‌بندی</span>
          </button>
          <button class="icon-grid-btn" data-action="store">
            <img class="grid-icon-img" src="assets/ic-basket.png" alt="">
            <span class="icon-grid-label">فروشگاه</span>
          </button>
        </div>

        <div class="menu-footer">
          <img class="menu-divider-img" src="assets/divider-gold.png" alt="">
        </div>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // render: HOW TO
  // ---------------------------------------------------------------------
  function renderHowTo() {
    const steps = [
      {
        title: "کلمه پیدا کن",
        desc: "با حروف داده شده، کلمه‌ی مخفی مرحله را پیدا کنید.",
        visual: `<div class="howto-tiles"><div class="slot-demo"></div><div class="slot-demo"></div><div class="slot-demo"></div></div>`,
      },
      {
        title: "حروف را بکش",
        desc: "با لمس و کشیدن حروف، کلمه را بسازید.",
        visual: `<div class="howto-tiles"><div class="tile-demo">د</div><div class="tile-demo">ر</div><div class="tile-demo">د</div><div class="tile-demo">ب</div><span class="howto-cursor">👆</span></div>`,
      },
      {
        title: "کلمه صحیح",
        desc: "با ساختن کلمه‌ی صحیح، کلمه در جای خالی قرار می‌گیرد و به مرحله‌ی بعد می‌روید.",
        visual: `<div class="howto-tiles"><div class="tile-demo tile-demo-correct">ب</div><div class="tile-demo tile-demo-correct">ر</div><div class="tile-demo tile-demo-correct">د</div><span class="howto-sparkle s1">✨</span><span class="howto-sparkle s2">✨</span></div>`,
      },
      {
        title: "حذف حروف",
        desc: "برای پاک کردن آخرین حرف، روی آیکون حذف بزنید.",
        visual: `<div class="howto-tiles"><div class="tile-demo">د</div><div class="tile-demo">ر</div><div class="tile-demo">ب</div><span class="howto-trash">🗑️</span></div>`,
      },
      {
        title: "راهنما",
        desc: "اگر گیر کردید، از راهنما استفاده کنید. هر مرحله تعداد مشخصی راهنما دارید.",
        visual: `<div class="howto-hint-circle">💡<span class="howto-hint-badge">${state.hintsLeft}</span></div>`,
      },
    ];

    root.innerHTML = `
      <div class="howto-scene">
        ${starFieldHTML()}
        <div class="howto-card-v2">
          <button class="modal-close howto-close" data-action="back-to-menu" aria-label="بستن">✕</button>
          <h2 class="howto-title-v2">🌿 راهنمای بازی 🌿</h2>
          <p class="howto-subtitle-v2">••• چگونه بازی کنیم؟ •••</p>
          <div class="howto-steps">
            ${steps
              .map(
                (s, i) => `
                  <div class="howto-row">
                    <div class="howto-visual">${s.visual}</div>
                    <div class="howto-info">
                      <div class="howto-row-title">${s.title}</div>
                      <div class="howto-row-desc">${s.desc}</div>
                    </div>
                    <div class="howto-num-badge">${i + 1}</div>
                  </div>
                `
              )
              .join("")}
          </div>
          <div class="howto-tip">💡 کلمات ممکن است از راست به چپ یا چپ به راست باشند.</div>
          <button class="btn-ok-v2" data-action="back-to-menu">باشه، فهمیدم</button>
        </div>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // render: STORE
  // ---------------------------------------------------------------------
  function renderStore() {
    const P = (window.IAP && window.IAP.PRODUCTS) || {};
    const removeAds = P.remove_ads || {};
    const hint5 = P.hint_pack_5 || {};
    const hint20 = P.hint_pack_20 || {};
    const hint100 = P.hint_pack_100 || {};
    const bundle = P.bundle_gold || {};
    const adsOwned = state.adsRemoved;

    const priceBtn = (p) => `
      <button class="price-pill-btn" data-action="buy" data-product-id="${p.id}">
        <img class="price-pill-icon" src="assets/ic-coin.png" alt="">${p.price}
      </button>
    `;

    const hintRow = (p, iconSrc) => `
      <div class="store-hint-row">
        <img class="hint-row-icon" src="${iconSrc}" alt="">
        <div class="hint-row-info">
          <div class="hint-row-title">${p.title}</div>
          <div class="hint-row-desc">${p.desc}</div>
        </div>
        <div class="hint-row-price">
          <div class="discount-badges">
            <span class="discount-badge">${p.discount}</span>
            <span class="price-strike">${p.originalPrice}</span>
          </div>
          ${priceBtn(p)}
        </div>
      </div>
    `;

    root.innerHTML = `
      <div class="store-scene">
        ${starFieldHTML()}

        <div class="store-topbar">
          <button class="round-icon-btn" data-action="back-to-menu" aria-label="بازگشت"><img src="assets/ic-store-back.png" alt=""></button>
          <div class="store-title">🌿 فروشگاه 🌿</div>
          <div class="pill-stat coin-pill"><img class="pill-icon-img" src="assets/ic-coin.png" alt="">۳,۵۴۰<span class="pill-add">+</span></div>
        </div>
        <p class="store-subtitle">هر خرید، حمایت از ماست ❤️</p>

        <div class="store-hero-card">
          <span class="store-ribbon">${removeAds.badge || ""}</span>
          <div class="store-hero-body">
            <img class="store-hero-icon" src="assets/ic-noads.png" alt="">
            <div class="store-hero-info">
              <div class="store-hero-title">${removeAds.title}</div>
              <div class="store-hero-desc">${removeAds.desc}</div>
              <ul class="store-feature-list">
                <li>${ICON.check()} حذف دائمی همه‌ی تبلیغات</li>
                <li>📴 بدون نیاز به اینترنت</li>
                <li>📱 برای همیشه و روی همه دستگاه‌ها</li>
              </ul>
            </div>
          </div>
          <div class="store-hero-buy">
            ${
              adsOwned
                ? `<span class="store-owned-tag">${ICON.check()} خریداری‌شده</span>`
                : `
                  <button class="store-buy-big" data-action="buy" data-product-id="remove_ads">
                    <img src="assets/ic-bag.png" alt="">خرید
                  </button>
                  <div class="price-pill-static">${removeAds.price}</div>
                `
            }
          </div>
        </div>

        ${hintRow(hint5, "assets/ic-bulb5.png")}
        ${hintRow(hint20, "assets/ic-bulb20.png")}
        ${hintRow(hint100, "assets/ic-chest100.png")}

        <div class="store-bundle-card">
          <span class="store-ribbon store-ribbon-purple">${bundle.badge || ""}</span>
          <div class="bundle-percent-badge">${bundle.discount}</div>
          <div class="store-bundle-body">
            <img class="store-bundle-icon" src="assets/ic-bundle.png" alt="">
            <div class="store-bundle-info">
              <div class="store-bundle-title">👑 ${bundle.title}</div>
              <div class="store-bundle-desc">${bundle.desc}</div>
              <div class="bundle-features">
                <div class="bundle-feature"><span class="bundle-feature-emoji">🚫📶</span><span>حذف تبلیغات همیشگی</span></div>
                <div class="bundle-feature"><span class="bundle-feature-emoji">💡</span><span>۵۰ راهنمای اضافه</span></div>
                <div class="bundle-feature"><span class="bundle-feature-emoji">🏅</span><span>نشان ویژه کنار اسم شما</span></div>
              </div>
            </div>
          </div>
          <div class="store-bundle-buy">
            <span class="price-strike">${bundle.originalPrice}</span>
            ${priceBtn(bundle)}
          </div>
        </div>

        <div class="store-trust-row">
          <div class="trust-item">🛡️<span>پرداخت امن<br>با تمامی کارت‌های بانکی</span></div>
          <div class="trust-item">⏳<span>بازگشت وجه<br>تا ۲۴ ساعت</span></div>
          <div class="trust-item">🎧<span>پشتیبانی ۲۴ ساعته<br>همیشه در کنار شما</span></div>
        </div>

        ${state.bonusHints > 0 ? `<p class="store-bonus-note">💎 ${state.bonusHints} راهنمای خریداری‌شده در انتظار استفاده</p>` : ""}
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

    const hintLabel =
      state.hintsLeft > 0
        ? `${state.hintsLeft}`
        : state.bonusHints > 0
        ? `💎${state.bonusHints}`
        : "";

    let actionRowHTML = "";
    if (state.status === "wrong") {
      actionRowHTML = `<button class="btn-clear-v2 btn-retry-v2" data-action="retry">${ICON.undo()} تلاش دوباره</button>`;
    } else if (state.status === "correct") {
      actionRowHTML = `<button class="btn-hint-v2 btn-next-v2" data-action="next">مرحله بعد ←</button>`;
    } else {
      actionRowHTML = `
        <button class="btn-clear-v2" data-action="clear" ${state.picked.length === 0 ? "disabled" : ""}>
          <img src="assets/ic-undo.png" alt="">حذف
        </button>
        <button class="btn-hint-v2" data-action="hint" ${state.status !== "playing" ? "disabled" : ""}>
          <img src="assets/ic-bulb.png" alt="">راهنما
        </button>
      `;
    }

    const stageNum = state.roundIndex + 1;
    const wordLen = round.word.length;
    // Rough, real (not decorative) difficulty read-out based on the actual
    // word length of this round.
    const difficulty = wordLen <= 3 ? "آسان" : wordLen <= 5 ? "متوسط" : "سخت";
    // Position within the current 5-round reward cycle — reused for both
    // the star track and the promo banner so the two stay consistent.
    const cyclePos = (state.roundIndex % 5) + 1;

    root.innerHTML = `
      <div class="game-scene">
        ${starFieldHTML()}

        <div class="game-topbar-v2">
          <button class="round-icon-btn" data-action="back-to-menu" aria-label="بازگشت به منو"><img src="assets/ic-pause.png" alt=""></button>
          <div class="pill-stat hint-pill-top"><img class="pill-icon-img" src="assets/ic-bulb.png" alt="">${state.hintsLeft}</div>
          <div class="stage-badge">
            <div class="stage-badge-title">مرحله ${stageNum}</div>
            <div class="stage-badge-sub">${difficulty}</div>
          </div>
          <div class="pill-stat coin-pill"><img class="pill-icon-img" src="assets/ic-coin2.png" alt="">۳,۵۴۰</div>
          <button class="round-icon-btn" data-action="store" aria-label="فروشگاه"><img src="assets/ic-shop.png" alt=""></button>
        </div>

        <div class="star-progress-row">
          <div class="star-progress-track">
            <div class="star-progress-fill" style="width:${((cyclePos - 1) / 4) * 100}%"></div>
            <div class="star-progress-dots">
              ${[1, 2, 3, 4].map((n) => `<span class="star-dot ${n <= cyclePos ? "on" : ""}">★</span>`).join("")}
            </div>
          </div>
          <img class="star-progress-gift" src="assets/ic-gift.png" alt="">
        </div>

        <div class="game-card">
          ${cardMotifHTML()}
          <div class="card-top-row">
            <div class="card-hint-pill"><img src="assets/ic-bulb.png" alt="">${state.hintsLeft}</div>
            <div class="card-mystery">${wordLen}<span class="mystery-q">؟</span></div>
            <div class="card-score">امتیاز<span>${ICON.star("var(--gold)")} ${state.score}</span></div>
          </div>
          <div class="answer-slots">${slotsHTML}</div>
          ${statusHTML}
          <p class="hint-text">${state.revealedHint ? round.hint : "حروف را بچین، کلمه‌ی فارسی بساز!"}</p>
          <div class="tiles-row">${tilesHTML}</div>
          <div class="action-row-v2">${actionRowHTML}</div>
        </div>

        <div class="promo-banner">
          <img class="promo-gift-img" src="assets/ic-gift.png" alt="">
          <div class="promo-text">
            <div class="promo-line1">کلمه‌ها رو پیدا کن و جایزه بگیر!</div>
            <div class="promo-line2">بعد از هر ۵ مرحله یک جایزه داری</div>
          </div>
          <div class="promo-count">
            <span class="promo-count-num">${cyclePos}/۵</span>
            <div class="promo-count-bar"><div class="promo-count-fill" style="width:${(cyclePos / 5) * 100}%"></div></div>
          </div>
        </div>

        <div class="bottom-nav-v2">
          <button class="nav-icon-btn" data-action="back-to-menu">
            <img src="assets/ic-nav-back.png" alt="">
            <span class="nav-icon-label">بازگشت</span>
          </button>
          <button class="nav-icon-btn">
            <img src="assets/ic-nav-found.png" alt="">
            <span class="nav-badge">${state.roundIndex}</span>
            <span class="nav-icon-label">کلمات پیدا شده</span>
          </button>
          <button class="nav-icon-btn nav-icon-btn-active" data-action="hint" ${state.status !== "playing" ? "disabled" : ""}>
            <img src="assets/ic-nav-hint.png" alt="">
            ${hintLabel ? `<span class="nav-badge">${hintLabel}</span>` : ""}
            <span class="nav-icon-label">راهنمای رایگان</span>
          </button>
          <button class="nav-icon-btn" data-action="shuffle">
            <img src="assets/ic-nav-shuffle.png" alt="">
            <span class="nav-icon-label">جابه‌جایی حروف</span>
          </button>
          <button class="nav-icon-btn" data-action="settings-open">
            <img src="assets/ic-nav-settings.png" alt="">
            <span class="nav-icon-label">تنظیمات</span>
          </button>
        </div>

        <div class="ad-slot">جای تبلیغ بنری (AdMob / Bazaar Ads)</div>
      </div>
    `;
  }

  function renderModal() {
    if (!state.settingsOpen) {
      modalRoot.innerHTML = "";
      return;
    }
    const muted = window.FX ? window.FX.isMuted() : false;
    const vol = window.FX ? Math.round(window.FX.getVolume() * 100) : 80;
    modalRoot.innerHTML = `
      <div class="modal-backdrop" data-action="settings-close">
        <div class="modal-card" data-action="noop">
          <div class="modal-header">
            <span class="modal-title">${ICON.gear()} تنظیمات</span>
            <button class="modal-close" data-action="settings-close" aria-label="بستن">✕</button>
          </div>

          <div class="modal-row">
            <span class="modal-row-label">🔊 صدای جلوه‌های بازی</span>
            <button class="switch ${muted ? "" : "on"}" data-action="settings-mute-toggle" role="switch" aria-checked="${!muted}">
              <span class="switch-knob"></span>
            </button>
          </div>

          <div class="modal-row modal-row-slider${muted ? " is-disabled" : ""}">
            <span class="modal-row-label">میزان صدا</span>
            <input type="range" id="settings-volume-range" min="0" max="100" value="${vol}" ${muted ? "disabled" : ""} />
            <span class="modal-vol-num">${vol}٪</span>
          </div>
        </div>
      </div>
    `;
  }

  function render() {
    if (state.view === "menu") renderMenu();
    else if (state.view === "howto") renderHowTo();
    else if (state.view === "store") renderStore();
    else renderGame();
    document.body.classList.toggle("no-scroll", state.view === "menu");
    renderModal();
    saveProgress();
    mountBanners();
  }

  function mountBanners() {
    if (!window.Ads) return;
    const slots = document.querySelectorAll(".ad-slot");
    slots.forEach((el) => {
      if (state.adsRemoved) {
        el.style.display = "none";
      } else {
        el.style.display = "";
        window.Ads.renderBannerInto(el);
      }
    });
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
    if (window.FX) {
      window.FX.playTileClick();
      window.FX.vibrate("light");
    }
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
      if (window.FX) {
        window.FX.playWin();
        window.FX.vibrate("heavy");
      }
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

    if (!state.adsRemoved && window.Ads && window.Ads.shouldShowInterstitial(next)) {
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
      if (window.FX) window.FX.playHint();
      render();
      return;
    }
    if (state.bonusHints > 0) {
      state.bonusHints -= 1;
      state.revealedHint = true;
      if (window.FX) window.FX.playHint();
      render();
      return;
    }
    // بدون راهنمای رایگان یا خریداری‌شده — پیشنهاد تبلیغ جایزه‌ای برای یه راهنمای اضافه
    if (window.Ads) {
      window.Ads.showRewarded(() => {
        state.revealedHint = true;
        if (window.FX) window.FX.playHint();
        render();
      });
    }
  }

  function handlePurchase(productId) {
    if (!window.IAP) return;
    window.IAP.purchase(productId, (product) => {
      if (product.id === "remove_ads") {
        state.adsRemoved = true;
      } else if (product.id === "bundle_gold") {
        state.adsRemoved = true;
        state.bonusHints += product.qty || 0;
      } else if (product.type === "consumable") {
        state.bonusHints += product.qty || 0;
      }
      render();
    });
  }

  // ---------------------------------------------------------------------
  // event delegation
  // ---------------------------------------------------------------------
  const TAP_SOUND_ACTIONS = new Set([
    "play",
    "resume",
    "howto",
    "store",
    "back-to-menu",
    "restart",
    "next",
    "buy",
    "shuffle",
  ]);

  document.addEventListener("click", (e) => {
    const el = e.target.closest("[data-action]");
    if (!el) return;
    const action = el.getAttribute("data-action");
    if (window.FX && TAP_SOUND_ACTIONS.has(action)) {
      window.FX.playButtonTap();
    }
    switch (action) {
      case "play": startGame(); break;
      case "resume": resumeGame(); break;
      case "howto": state.view = "howto"; render(); break;
      case "store": state.view = "store"; render(); break;
      case "buy": handlePurchase(el.getAttribute("data-product-id")); break;
      case "back-to-menu": goToMenu(); break;
      case "pick": pickTile(el.getAttribute("data-tile-id")); break;
      case "undo": undoLast(); break;
      case "clear": resetPicks(); break;
      case "retry": tryAgain(); break;
      case "next": nextRound(); break;
      case "restart": startGame(); break;
      case "hint": useHint(); break;
      case "shuffle": shuffleTiles(); render(); break;
      case "toggle-sound":
        if (window.FX) window.FX.toggleMuted();
        render();
        break;
      case "settings-open":
        state.settingsOpen = true;
        renderModal();
        break;
      case "settings-close":
        state.settingsOpen = false;
        renderModal();
        break;
      case "settings-mute-toggle":
        if (window.FX) window.FX.toggleMuted();
        render();
        break;
      case "noop":
        break;
      default: break;
    }
  });

  document.addEventListener("input", (e) => {
    if (e.target && e.target.id === "settings-volume-range") {
      const val = parseInt(e.target.value, 10) / 100;
      if (window.FX) window.FX.setVolume(val);
      const numEl = document.querySelector(".modal-vol-num");
      if (numEl) numEl.textContent = e.target.value + "٪";
    }
  });

  document.addEventListener("change", (e) => {
    if (e.target && e.target.id === "settings-volume-range" && window.FX) {
      window.FX.playTileClick();
    }
  });

  if (window.Ads) window.Ads.initAds();
  render();
})();
