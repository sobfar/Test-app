/*
 * iap.js — لایه‌ی انتزاعی خریدهای درون‌برنامه‌ای (IAP)
 * -----------------------------------------------------------------------
 * فعلاً هیچ درگاه یا قرارداد واقعی وصل نیست. این فایل خریدها رو با یه
 * نسخه‌ی شبیه‌سازی‌شده (mock) تقلید می‌کنه — با یه دیالوگ تایید ساده به‌جای
 * درگاه پرداخت واقعی. هدف این‌ه که:
 *   ۱) کل تجربه‌ی فروشگاه (قیمت‌ها، محصولات، فلوی خرید) از الان قابل تست باشه.
 *   ۲) وقتی به یه درگاه واقعی وصل شدی، فقط تابع purchase() پایین رو با
 *      SDK واقعی جایگزین کنی — بقیه‌ی کد بازی (app.js) دست نمی‌خوره.
 *
 * -----------------------------------------------------------------------
 * گزینه‌های رایج برای اتصال واقعی:
 *   - IAP خودِ کافه‌بازار (Bazaar Billing) / مایکت (Myket IAB) — پرداخت از
 *     کیف‌پول یا کارت داخل خودِ مارکت، مناسب محصولات درون‌اپ مثل این‌ها.
 *   - زرین‌پال (یا هر درگاه ایرانی دیگه) — اگه بخوای مستقل از مارکت‌ها
 *     پرداخت بگیری (نیاز به یه سرور کوچیک برای تایید تراکنش داری).
 *
 * TODO (وقتی به درگاه واقعی وصل شدی):
 *   - SDK بازار/مایکت رو طبق داکیومنت رسمی‌شون اضافه کن (یه پلاگین
 *     Capacitor/Cordova واسط لازمه چون SDK اصلی نیتیو اندرویده).
 *   - purchase() رو با فراخوانی واقعی درگاه جایگزین کن؛ فقط بعد از
 *     تایید قطعی تراکنش (نه فقط باز شدن دیالوگ)، onSuccess رو صدا بزن.
 *   - برای زرین‌پال، تایید پرداخت باید سمت سرور انجام شه (کد کلاینت به‌
 *     تنهایی برای تایید تراکنش کافی و امن نیست).
 * -----------------------------------------------------------------------
 */
(function (global) {
  "use strict";

  const PRODUCTS = {
    remove_ads: {
      id: "remove_ads",
      title: "حذف تبلیغات",
      desc: "برای همیشه بدون تبلیغ بازی کن (بنری و بینابینی)",
      price: "۱۵,۰۰۰ تومان",
      type: "non_consumable",
    },
    hint_pack_5: {
      id: "hint_pack_5",
      title: "۵ راهنمای اضافه",
      desc: "بدون نیاز به دیدن تبلیغ، فوری استفاده می‌شه",
      price: "۵,۰۰۰ تومان",
      type: "consumable",
      qty: 5,
    },
    hint_pack_20: {
      id: "hint_pack_20",
      title: "۲۰ راهنمای اضافه",
      desc: "به‌صرفه‌تر برای کسانی که زیاد گیر می‌کنن",
      price: "۱۵,۰۰۰ تومان",
      type: "consumable",
      qty: 20,
    },
  };

  function listProducts() {
    return Object.values(PRODUCTS);
  }

  // ---------------------------------------------------------------------
  // خرید — فعلاً فقط یه دیالوگ تایید شبیه‌سازی‌شده
  // ---------------------------------------------------------------------
  function purchase(productId, onSuccess, onCancel) {
    const product = PRODUCTS[productId];
    if (!product) return;

    // TODO: جایگزین با فراخوانی واقعی Bazaar Billing / Myket IAB / زرین‌پال.
    // فقط وقتی تراکنش واقعاً تایید شد (نه فقط دیالوگ باز شد)، onSuccess رو صدا بزن.
    mountPurchaseDialog({
      product,
      onConfirm: () => onSuccess && onSuccess(product),
      onCancel,
    });
  }

  function mountPurchaseDialog({ product, onConfirm, onCancel }) {
    const overlay = document.createElement("div");
    overlay.className = "mock-ad-overlay"; // از همون استایل overlay تبلیغات استفاده می‌کنیم
    overlay.innerHTML = `
      <div class="mock-ad-card">
        <div class="mock-ad-badge">نمونه خرید — بدون درگاه واقعی</div>
        <h3>${product.title}</h3>
        <p>${product.desc}</p>
        <p style="font-weight:800; font-size:1.1rem; margin:-6px 0 18px;">${product.price}</p>
        <button class="mock-ad-close-btn" data-role="confirm">تایید خرید (شبیه‌سازی)</button>
        <button class="mock-ad-close-btn" data-role="cancel" style="background:transparent; color:var(--ink); opacity:.6; margin-top:8px;">انصراف</button>
      </div>
    `;
    document.body.appendChild(overlay);

    const cleanup = (confirmed) => {
      overlay.remove();
      if (confirmed) onConfirm && onConfirm();
      else onCancel && onCancel();
    };

    overlay.querySelector('[data-role="confirm"]').addEventListener("click", () => cleanup(true));
    overlay.querySelector('[data-role="cancel"]').addEventListener("click", () => cleanup(false));
  }

  global.IAP = {
    PRODUCTS,
    listProducts,
    purchase,
  };
})(window);
