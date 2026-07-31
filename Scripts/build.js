// این اسکریپت app.html (تک‌فایل سورس شامل HTML+CSS+JS) رو می‌خونه،
// کد جاوااسکریپت داخل تگ <script type="module"> رو (به همراه importهای پلاگین‌ها)
// با esbuild باندل می‌کنه، و نتیجه رو به‌عنوان www/index.html می‌نویسه.
const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');

async function main() {
  const srcPath = path.join(__dirname, '..', 'app.html');
  const html = fs.readFileSync(srcPath, 'utf8');

  const match = html.match(/<script type="module">([\s\S]*?)<\/script>/);
  if (!match) {
    throw new Error('تگ <script type="module"> توی app.html پیدا نشد.');
  }

  const result = await esbuild.build({
    stdin: {
      contents: match[1],
      resolveDir: path.join(__dirname, '..'),
      loader: 'js',
    },
    bundle: true,
    format: 'iife',
    write: false,
    target: ['es2020'],
  });
  const bundledJs = result.outputFiles[0].text;

  const finalHtml = html.replace(match[0], '<script>\n' + bundledJs + '\n</script>');

  const outDir = path.join(__dirname, '..', 'www');
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'index.html'), finalHtml, 'utf8');

  console.log('ساخته شد: www/index.html (' + (finalHtml.length / 1024).toFixed(1) + ' KB)');
}

main().catch((err) => {
  console.error('خطا در بیلد:', err);
  process.exit(1);
});
