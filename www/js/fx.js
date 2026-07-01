/*
 * fx.js — صدا و لرزش (Haptic) بازی
 * -----------------------------------------------------------------------
 * صداها همه با Web Audio API به‌صورت زنده ساخته می‌شن (نه فایل mp3/wav) —
 * یعنی هیچ فایل صوتی خارجی لازم نیست و بدون هیچ دانلود اضافه‌ای کار می‌کنن.
 * این صداها ساده و «نمونه»ان؛ هر وقت خواستی صدای واقعی و حرفه‌ای‌تر (که
 * توسط یه صداپرداز ساخته شده) جایگزینشون کن — کافیه توابع pickTile/
 * correct/wrong/... رو با new Audio('assets/sounds/xxx.mp3').play() عوض کنی.
 *
 * لرزش (Haptic) از طریق پلاگین Capacitor Haptics در اپ واقعی کار می‌کنه.
 * TODO: قبل از بیلد نهایی:
 *   npm install @capacitor/haptics
 *   npx cap sync android
 * اگه پلاگین نصب نباشه، خودکار به navigator.vibrate (مرورگر) یا هیچی
 * (اگه هیچ‌کدوم نبود) fallback می‌کنه — پس بدون نصب هم اپ کرش نمی‌کنه.
 * -----------------------------------------------------------------------
 */
(function (global) {
  "use strict";

  const MUTE_KEY = "vazhebafi_sound_muted";
  let muted = localStorage.getItem(MUTE_KEY) === "1";

  let audioCtx = null;
  function getCtx() {
    if (!audioCtx) {
      const Ctx = window.AudioContext || window.webkitAudioContext;
      if (Ctx) audioCtx = new Ctx();
    }
    // مرورگرها اکثراً AudioContext رو تا یه تعامل کاربر «suspended» نگه می‌دارن
    if (audioCtx && audioCtx.state === "suspended") {
      audioCtx.resume().catch(() => {});
    }
    return audioCtx;
  }

  // ---------------------------------------------------------------------
  // تولید یه تن ساده (sine/triangle) با envelope کوتاه، بدون نیاز به فایل
  // ---------------------------------------------------------------------
  function tone({ freq, duration = 0.12, type = "sine", startTime = 0, gain = 0.18 }) {
    if (muted) return;
    const ctx = getCtx();
    if (!ctx) return;
    const t0 = ctx.currentTime + startTime;
    const osc = ctx.createOscillator();
    const amp = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, t0);
    amp.gain.setValueAtTime(0, t0);
    amp.gain.linearRampToValueAtTime(gain, t0 + 0.012);
    amp.gain.exponentialRampToValueAtTime(0.001, t0 + duration);
    osc.connect(amp);
    amp.connect(ctx.destination);
    osc.start(t0);
    osc.stop(t0 + duration + 0.02);
  }

  function playTileClick() {
    tone({ freq: 520, duration: 0.06, type: "triangle", gain: 0.12 });
  }

  function playButtonTap() {
    tone({ freq: 340, duration: 0.05, type: "sine", gain: 0.1 });
  }

  function playCorrect() {
    tone({ freq: 523.25, duration: 0.1, type: "triangle" }); // C5
    tone({ freq: 659.25, duration: 0.16, type: "triangle", startTime: 0.08 }); // E5
  }

  function playWrong() {
    tone({ freq: 220, duration: 0.16, type: "sawtooth", gain: 0.14 });
    tone({ freq: 180, duration: 0.2, type: "sawtooth", startTime: 0.09, gain: 0.12 });
  }

  function playHint() {
    tone({ freq: 700, duration: 0.09, type: "sine", gain: 0.12 });
    tone({ freq: 900, duration: 0.12, type: "sine", startTime: 0.06, gain: 0.1 });
  }

  function playWin() {
    // یه آرپژ کوتاه صعودی برای لحظه‌ی پایان بازی
    [523.25, 659.25, 783.99, 1046.5].forEach((freq, i) => {
      tone({ freq, duration: 0.22, type: "triangle", startTime: i * 0.1, gain: 0.16 });
    });
  }

  // ---------------------------------------------------------------------
  // لرزش (haptics) — با fallback امن
  // ---------------------------------------------------------------------
  function nativeHaptics() {
    return (
      window.Capacitor &&
      window.Capacitor.Plugins &&
      window.Capacitor.Plugins.Haptics
    );
  }

  function vibrate(kind) {
    if (muted) return; // فعلاً لرزش رو هم زیر همون کلید صدا خاموش می‌کنیم
    const haptics = nativeHaptics();
    if (haptics) {
      // TODO: بعد از نصب @capacitor/haptics و cap sync، این بخش واقعی کار می‌کنه
      const style = kind === "light" ? "LIGHT" : kind === "heavy" ? "HEAVY" : "MEDIUM";
      try {
        haptics.impact({ style });
      } catch (e) {
        /* پلاگین هنوز واقعاً نصب/سینک نشده — بی‌خطر رد می‌شیم */
      }
      return;
    }
    if (navigator.vibrate) {
      const pattern = kind === "light" ? 10 : kind === "heavy" ? [20, 30, 20] : 15;
      navigator.vibrate(pattern);
    }
  }

  // ---------------------------------------------------------------------
  // کنترل صدا (mute toggle)
  // ---------------------------------------------------------------------
  function isMuted() {
    return muted;
  }

  function setMuted(value) {
    muted = !!value;
    try {
      localStorage.setItem(MUTE_KEY, muted ? "1" : "0");
    } catch (e) {
      /* localStorage در دسترس نیست — بی‌خطر رد می‌شیم */
    }
  }

  function toggleMuted() {
    setMuted(!muted);
    return muted;
  }

  global.FX = {
    playTileClick,
    playButtonTap,
    playCorrect,
    playWrong,
    playHint,
    playWin,
    vibrate,
    isMuted,
    setMuted,
    toggleMuted,
  };
})(window);
