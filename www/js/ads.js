/*
 * ads.js — لایه‌ی انتزاعی تبلیغات
 * -----------------------------------------------------------------------
 * فعلاً هیچ قرارداد یا SDK واقعی وصل نیست. این فایل رفتار واقعی تبلیغات
 * رو با یه نسخه‌ی شبیه‌سازی‌شده (mock) تقلید می‌کنه تا:
 *   ۱) تجربه‌ی کاربر (تعداد نمایش، جای بنر، لحظه‌ی نمایش بینابینی) از الان
 *      قابل تست باشه.
 *   ۲) وقتی قرارداد بستی، فقط کافیه توابع پایین (initAds/showBanner/
 *      showInterstitial/showRewarded) رو با SDK واقعی جایگزین کنی —
 *      بقیه‌ی کد بازی (app.js) هیچ تغییری لازم نداره.
 *
 * رایج‌ترین گزینه‌ی بازار ایران الان تپسل (Tapsell) است — چون توسط
 * کافه‌بازار پشتیبانی می‌شه و بنر/بینابینی/جایزه‌ای (rewarded) داره.
 * AdMob گوگل هم از نظر فنی کار می‌کنه ولی به‌خاطر محدودیت خدمات گوگل در
 * ایران، اکثر اپ‌های بازار/مایکت به‌جاش از تپسل استفاده می‌کنن.
 *
 * -----------------------------------------------------------------------
 * TODO (وقتی قرارداد بستی):
 *   - Tapsell SDK رو طبق داکیومنت رسمی به پروژه‌ی Capacitor اضافه کن
 *     (یه پلاگین Capacitor/Cordova واسط لازمه چون SDK اصلی نیتیو اندرویده).
 *   - Zone ID های واقعی (بنر/بینابینی/جایزه‌ای) رو جای مقادیر "MOCK_ZONE_*"
 *     پایین بگذار.
 *   - initAds() رو با Tapsell.initialize(...) واقعی پر کن.
 *   - showBanner/showInterstitial/showRewarded رو با متدهای واقعی
 *     Tapsell.requestAd / Tapsell.showAd جایگزین کن.
 * -----------------------------------------------------------------------
 */
(function (global) {
  "use strict";

  const ZONE = {
    BANNER: "MOCK_ZONE_BANNER",
    INTERSTITIAL: "MOCK_ZONE_INTERSTITIAL",
    REWARDED: "MOCK_ZONE_REWARDED",
  };

  let initialized = false;

  function initAds() {
    // TODO: جایگزین با Tapsell.initialize(APP_ID) واقعی
    initialized = true;
    console.log("[ads:mock] initialized (no real network connected yet)");
  }

  // ---------------------------------------------------------------------
  // بنر — یه نوار ثابت داخل یه container مشخص
  // ---------------------------------------------------------------------
  function renderBannerInto(containerEl) {
    if (!containerEl) return;
    // TODO: جایگزین با درخواست بنر واقعی از تپسل/ادموب و mount کردنش این‌جا
    containerEl.innerHTML = `
      <div class="mock-ad-banner">
        <span>جای تبلیغ بنری</span>
        <span class="mock-ad-tag">(نمونه — بدون شبکه‌ی واقعی)</span>
      </div>
    `;
  }

  // ---------------------------------------------------------------------
  // بینابینی — بین مراحل، با یه سقف زمانی/تعدادی تا آزاردهنده نشه
  // ---------------------------------------------------------------------
  let lastInterstitialAt = 0;
  const INTERSTITIAL_EVERY_N_ROUNDS = 5;
  const INTERSTITIAL_MIN_GAP_MS = 45_000; // حداقل فاصله بین دو تبلیغ بینابینی

  function shouldShowInterstitial(roundsCompleted) {
    const now = Date.now();
    const enoughRoundsPassed =
      roundsCompleted > 0 && roundsCompleted % INTERSTITIAL_EVERY_N_ROUNDS === 0;
    const enoughTimePassed = now - lastInterstitialAt > INTERSTITIAL_MIN_GAP_MS;
    return enoughRoundsPassed && enoughTimePassed;
  }

  function showInterstitial(onClose) {
    // TODO: جایگزین با Tapsell.requestAd(ZONE.INTERSTITIAL, ...) واقعی
    lastInterstitialAt = Date.now();
    mountOverlay({
      title: "تبلیغ بینابینی",
      body: "این‌جا بعداً یه تبلیغ واقعی (ویدیویی یا تصویری) از تپسل نشون داده می‌شه.",
      closeLabel: "ادامه‌ی بازی",
      onClose,
      autoCloseMs: 3000, // شبیه‌سازی طول تبلیغ واقعی
    });
  }

  // ---------------------------------------------------------------------
  // جایزه‌ای — کاربر با میل خودش می‌بینه و یه پاداش می‌گیره (مثلاً راهنما)
  // ---------------------------------------------------------------------
  function showRewarded(onReward, onClose) {
    // TODO: جایگزین با Tapsell.requestAd(ZONE.REWARDED, ...) واقعی +
    // فقط وقتی SDK واقعاً onAdRewarded داد، onReward() رو صدا بزن.
    mountOverlay({
      title: "تبلیغ جایزه‌ای",
      body: "این‌جا بعداً یه تبلیغ واقعی نشون داده می‌شه؛ بعد از دیدنش، یه راهنمای رایگان جایزه می‌گیری.",
      closeLabel: "دریافت پاداش",
      onClose: () => {
        onReward && onReward();
        onClose && onClose();
      },
      autoCloseMs: null, // جایزه‌ای معمولاً کاربر خودش می‌بندش، نه خودکار
    });
  }

  // ---------------------------------------------------------------------
  // helper: یه overlay ساده برای شبیه‌سازی تبلیغ تمام‌صفحه
  // ---------------------------------------------------------------------
  function mountOverlay({ title, body, closeLabel, onClose, autoCloseMs }) {
    const overlay = document.createElement("div");
    overlay.className = "mock-ad-overlay";
    overlay.innerHTML = `
      <div class="mock-ad-card">
        <div class="mock-ad-badge">نمونه تبلیغ</div>
        <h3>${title}</h3>
        <p>${body}</p>
        <button class="mock-ad-close-btn">${closeLabel}</button>
      </div>
    `;
    document.body.appendChild(overlay);

    const cleanup = () => {
      overlay.remove();
      onClose && onClose();
    };

    overlay.querySelector(".mock-ad-close-btn").addEventListener("click", cleanup);

    if (autoCloseMs) {
      setTimeout(cleanup, autoCloseMs);
    }
  }

  global.Ads = {
    ZONE,
    initAds,
    renderBannerInto,
    shouldShowInterstitial,
    showInterstitial,
    showRewarded,
  };
})(window);
