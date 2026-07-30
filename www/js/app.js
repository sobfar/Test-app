// ---------------------------------------------
// تنظیمات
// ---------------------------------------------
const BOARD_URL = 'http://192.168.4.1';
const STORAGE_KEY = 'board_calibration_values';

const DEFAULT_VALUES = {
  vmax: 240,
  vmin: 100,
  hyst: 7,
  delay: 5,
  under: 100,
  calout: 485,
  calin: 485,
  coeff: 90,
};

function getPlugin(name) {
  const plugin = window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins[name];
  if (!plugin) console.warn('پلاگین ' + name + ' پیدا نشد (روی مرورگر معمولی کار نمی‌کنه).');
  return plugin;
}
function getWifi() { return getPlugin('CapacitorWifi'); }
function getBrowser() { return getPlugin('InAppBrowser'); }

const screens = {
  list: document.getElementById('screen-list'),
  password: document.getElementById('screen-password'),
  board: document.getElementById('screen-board'),
  values: document.getElementById('screen-values'),
};
function showScreen(name) {
  Object.values(screens).forEach(s => s.classList.remove('active'));
  screens[name].classList.add('active');
}

const wifiListEl = document.getElementById('wifi-list');
const loadingEl = document.getElementById('loading');
const statusBoxEl = document.getElementById('status-box');
let selectedSsid = null;

function setStatus(message, type) {
  statusBoxEl.textContent = message;
  statusBoxEl.className = 'status-box ' + (type || '');
  statusBoxEl.classList.remove('hidden');
}
function clearStatus() { statusBoxEl.classList.add('hidden'); }

function renderNetworks(networks) {
  wifiListEl.innerHTML = '';
  if (!networks || networks.length === 0) {
    wifiListEl.innerHTML = '<li style="justify-content:center;color:#9aa0a6">شبکه‌ای پیدا نشد</li>';
    return;
  }
  const unique = Object.values(
    networks.reduce((acc, n) => {
      if (!acc[n.ssid] || acc[n.ssid].rssi < n.rssi) acc[n.ssid] = n;
      return acc;
    }, {})
  ).sort((a, b) => b.rssi - a.rssi);

  unique.forEach(net => {
    const li = document.createElement('li');
    li.innerHTML = '<span class="ssid">📶 ' + (net.ssid || '(بدون نام)') + '</span><span class="signal">' + net.rssi + ' dBm</span>';
    li.addEventListener('click', () => onNetworkTap(net.ssid));
    wifiListEl.appendChild(li);
  });
}

async function scanNetworks() {
  const Wifi = getWifi();
  if (!Wifi) {
    setStatus('این قابلیت فقط روی اپ نصب‌شده روی گوشی کار می‌کند، نه در مرورگر.', 'error');
    return;
  }
  clearStatus();
  loadingEl.classList.remove('hidden');
  wifiListEl.innerHTML = '';

  try {
    const perm = await Wifi.checkPermissions();
    if (perm.location !== 'granted') {
      const req = await Wifi.requestPermissions({ permissions: ['location'] });
      if (req.location !== 'granted') {
        setStatus('برای دیدن لیست وای‌فای‌ها به دسترسی موقعیت مکانی نیاز داریم.', 'error');
        loadingEl.classList.add('hidden');
        return;
      }
    }

    const handle = await Wifi.addListener('networksScanned', async () => {
      const result = await Wifi.getAvailableNetworks();
      renderNetworks(result.networks);
      loadingEl.classList.add('hidden');
      handle.remove();
    });

    await Wifi.startScan();
  } catch (err) {
    console.error(err);
    setStatus('خطا در اسکن شبکه‌ها: ' + (err.message || err), 'error');
    loadingEl.classList.add('hidden');
  }
}

document.getElementById('btn-scan').addEventListener('click', scanNetworks);

function onNetworkTap(ssid) {
  selectedSsid = ssid;
  document.getElementById('pass-ssid-title').textContent = 'اتصال به ' + ssid;
  document.getElementById('wifi-password').value = '';
  showScreen('password');
}

document.getElementById('btn-back-from-pass').addEventListener('click', () => showScreen('list'));
document.getElementById('show-password').addEventListener('change', (e) => {
  document.getElementById('wifi-password').type = e.target.checked ? 'text' : 'password';
});

document.getElementById('btn-connect').addEventListener('click', async () => {
  const password = document.getElementById('wifi-password').value;
  const Wifi = getWifi();
  if (!Wifi || !selectedSsid) return;

  const btn = document.getElementById('btn-connect');
  btn.disabled = true;
  btn.textContent = 'در حال اتصال...';

  try {
    await Wifi.connect({
      ssid: selectedSsid,
      password: password,
      autoRouteTraffic: true,
    });
    showScreen('board');
  } catch (err) {
    console.error(err);
    alert('اتصال ناموفق بود: ' + (err.message || err));
  } finally {
    btn.disabled = false;
    btn.textContent = 'اتصال';
  }
});

const valueFieldIds = ['vmax', 'vmin', 'hyst', 'delay', 'under', 'calout', 'calin', 'coeff'];

function loadValues() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : Object.assign({}, DEFAULT_VALUES);
  } catch (e) {
    return Object.assign({}, DEFAULT_VALUES);
  }
}
function saveValues(values) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
}
function fillValueInputs(values) {
  valueFieldIds.forEach(id => {
    document.getElementById('val-' + id).value = values[id];
  });
}
function readValueInputs() {
  const values = {};
  valueFieldIds.forEach(id => {
    values[id] = document.getElementById('val-' + id).value;
  });
  return values;
}

document.getElementById('btn-edit-values').addEventListener('click', () => {
  fillValueInputs(loadValues());
  showScreen('values');
});
document.getElementById('btn-back-from-values').addEventListener('click', () => showScreen('board'));
document.getElementById('btn-save-values').addEventListener('click', () => {
  saveValues(readValueInputs());
  showScreen('board');
});

const boardStatusEl = document.getElementById('board-status');
function setBoardStatus(message, type) {
  boardStatusEl.textContent = message;
  boardStatusEl.className = 'status-box ' + (type || '');
  boardStatusEl.classList.remove('hidden');
}
function clearBoardStatus() { boardStatusEl.classList.add('hidden'); }

function buildFillScript(values) {
  const fieldMap = {
    vmax:   { value: values.vmax,   keywords: ['vmax', 'v_max', 'maxv', 'حداکثر'] },
    vmin:   { value: values.vmin,   keywords: ['vmin', 'v_min', 'minv', 'حداقل'] },
    hyst:   { value: values.hyst,   keywords: ['hyst', 'هیستر'] },
    delay:  { value: values.delay,  keywords: ['delay', 'تاخیر', 'تأخیر'] },
    under:  { value: values.under,  keywords: ['under', 'زیربار', 'زیر بار'] },
    calout: { value: values.calout, keywords: ['calout', 'cal_out', 'خروج'] },
    calin:  { value: values.calin,  keywords: ['calin', 'cal_in', 'ورودی'] },
    coeff:  { value: values.coeff,  keywords: ['coeff', 'ضریب', 'k_cal'] },
  };

  return (
    "(function(){\n" +
    "  var fieldMap = " + JSON.stringify(fieldMap) + ";\n" +
    "  var results = [];\n" +
    "  function norm(t){ return (t||'').toLowerCase().replace(/\\s+/g,''); }\n" +
    "  function findByAttrs(keywords){\n" +
    "    var inputs = Array.from(document.querySelectorAll('input, select'));\n" +
    "    for (var i=0;i<inputs.length;i++){\n" +
    "      var el = inputs[i];\n" +
    "      var hay = norm((el.name||'') + ' ' + (el.id||'') + ' ' + (el.placeholder||''));\n" +
    "      for (var k=0;k<keywords.length;k++){\n" +
    "        if (hay.indexOf(norm(keywords[k])) !== -1) return el;\n" +
    "      }\n" +
    "    }\n" +
    "    return null;\n" +
    "  }\n" +
    "  function findByLabelText(keywords){\n" +
    "    var candidates = Array.from(document.querySelectorAll('label, td, span, div, p, b, strong'));\n" +
    "    for (var i=0;i<candidates.length;i++){\n" +
    "      var el = candidates[i];\n" +
    "      if (el.children.length > 0) continue;\n" +
    "      var text = norm(el.textContent);\n" +
    "      for (var k=0;k<keywords.length;k++){\n" +
    "        if (text.indexOf(norm(keywords[k])) === -1) continue;\n" +
    "        if (el.tagName === 'LABEL' && el.htmlFor){\n" +
    "          var t = document.getElementById(el.htmlFor);\n" +
    "          if (t) return t;\n" +
    "        }\n" +
    "        var row = el.closest('tr, li') || el.parentElement;\n" +
    "        if (row){\n" +
    "          var input = row.querySelector('input, select');\n" +
    "          if (input) return input;\n" +
    "        }\n" +
    "        var sib = el.nextElementSibling;\n" +
    "        while (sib){\n" +
    "          if (sib.matches && sib.matches('input, select')) return sib;\n" +
    "          sib = sib.nextElementSibling;\n" +
    "        }\n" +
    "      }\n" +
    "    }\n" +
    "    return null;\n" +
    "  }\n" +
    "  Object.keys(fieldMap).forEach(function(key){\n" +
    "    var f = fieldMap[key];\n" +
    "    var target = findByAttrs(f.keywords) || findByLabelText(f.keywords);\n" +
    "    if (target){\n" +
    "      target.focus();\n" +
    "      if (target.tagName === 'SELECT'){ target.value = String(f.value); }\n" +
    "      else { target.value = f.value; }\n" +
    "      target.dispatchEvent(new Event('input', {bubbles:true}));\n" +
    "      target.dispatchEvent(new Event('change', {bubbles:true}));\n" +
    "      results.push(key + ': پیدا و پر شد');\n" +
    "    } else {\n" +
    "      results.push(key + ': پیدا نشد');\n" +
    "    }\n" +
    "  });\n" +
    "  if (window.mobileApp) { window.mobileApp.postMessage({ detail: { type: 'fillResult', results: results } }); }\n" +
    "})();"
  );
}

const EXTRACT_SCRIPT =
  "(function(){\n" +
  "  var html = document.documentElement.outerHTML;\n" +
  "  if (window.mobileApp) { window.mobileApp.postMessage({ detail: { type: 'htmlExtract', html: html } }); }\n" +
  "})();";

let messageListenerAttached = false;
function attachBrowserMessageListener() {
  const Browser = getBrowser();
  if (!Browser || messageListenerAttached) return;
  messageListenerAttached = true;
  Browser.addListener('messageFromWebview', (event) => {
    const detail = event && event.detail;
    if (!detail) return;
    if (detail.type === 'fillResult') {
      alert('نتیجه پر کردن فیلدها:\n\n' + detail.results.join('\n'));
    } else if (detail.type === 'htmlExtract') {
      const out = document.getElementById('extract-output');
      out.value = detail.html;
      out.classList.remove('hidden');
      document.getElementById('btn-copy-html').classList.remove('hidden');
      setBoardStatus('HTML استخراج شد؛ می‌تونی کپی و برای بررسی بفرستی.', 'success');
    }
  });
}

document.getElementById('btn-open-fill').addEventListener('click', async () => {
  const Browser = getBrowser();
  if (!Browser) { setBoardStatus('پلاگین InAppBrowser در دسترس نیست.', 'error'); return; }
  attachBrowserMessageListener();
  clearBoardStatus();
  try {
    const res = await Browser.openWebView({
      url: BOARD_URL,
      title: 'تنظیمات برد',
      showReloadButton: true,
      toolbarType: 'default',
      isPresentAfterPageLoad: true,
    });
    setTimeout(async () => {
      const values = loadValues();
      await Browser.executeScript({ id: res.id, code: buildFillScript(values) });
    }, 1500);
  } catch (err) {
    console.error(err);
    setBoardStatus('خطا در باز کردن صفحه برد: ' + (err.message || err), 'error');
  }
});

document.getElementById('btn-open-only').addEventListener('click', async () => {
  const Browser = getBrowser();
  if (!Browser) { setBoardStatus('پلاگین InAppBrowser در دسترس نیست.', 'error'); return; }
  clearBoardStatus();
  try {
    await Browser.openWebView({
      url: BOARD_URL,
      title: 'تنظیمات برد',
      showReloadButton: true,
      toolbarType: 'default',
    });
  } catch (err) {
    console.error(err);
    setBoardStatus('خطا در باز کردن صفحه برد: ' + (err.message || err), 'error');
  }
});

document.getElementById('btn-extract-html').addEventListener('click', async () => {
  const Browser = getBrowser();
  if (!Browser) { setBoardStatus('پلاگین InAppBrowser در دسترس نیست.', 'error'); return; }
  attachBrowserMessageListener();
  clearBoardStatus();
  setBoardStatus('در حال بارگذاری صفحه برد برای استخراج...', '');
  try {
    const res = await Browser.openWebView({
      url: BOARD_URL,
      hidden: true,
      isPresentAfterPageLoad: true,
    });
    setTimeout(async () => {
      await Browser.executeScript({ id: res.id, code: EXTRACT_SCRIPT });
      setTimeout(() => Browser.close({ id: res.id }), 1500);
    }, 1500);
  } catch (err) {
    console.error(err);
    setBoardStatus('خطا در استخراج HTML: ' + (err.message || err), 'error');
  }
});

document.getElementById('btn-copy-html').addEventListener('click', () => {
  const out = document.getElementById('extract-output');
  out.select();
  document.execCommand('copy');
  setBoardStatus('کپی شد.', 'success');
});

document.getElementById('btn-back-from-board').addEventListener('click', () => showScreen('list'));

window.addEventListener('DOMContentLoaded', () => {
  fillValueInputs(loadValues());
  scanNetworks();
});

