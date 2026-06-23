#!/usr/bin/env node
/**
 * Download Noto Sans font files into backend/src/main/resources/fonts/
 *
 * Source: jsDelivr CDN, which mirrors GitHub repos.  Used because direct GitHub
 * access may be blocked in some networks (e.g. China).  jsDelivr provides the
 * same files via cdn.jsdelivr.net.
 *
 * Mirrors used:
 *   notofonts/notofonts.github.io  — Latin / Thai families (TTF, hinted)
 *   notofonts/noto-cjk              — CJK regional subsets (OTF)
 *
 * Usage:  node scripts/download-fonts.js
 * Requires Node 18+.
 */
const fs = require('fs');
const path = require('path');
const https = require('https');

const OUT_DIR = path.resolve(__dirname, '..', 'backend', 'src', 'main', 'resources', 'fonts');

const OFL_TEXT = `Noto Sans, Noto Sans Thai, Noto Sans CJK
Copyright 2014-2024 Adobe Inc. (http://www.adobe.com/), with Reserved Font Name 'Source'.
Copyright 2014-2024 Google LLC.

This Font Software is licensed under the SIL Open Font License, Version 1.1.
Full text: https://scripts.sil.org/OFL
`;

const FONTS = [
  // Latin / Greek / Cyrillic / Vietnamese / Czech — single TTF with full coverage
  ['NotoSans-Regular.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSans/hinted/ttf/NotoSans-Regular.ttf'],
  ['NotoSans-Bold.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSans/hinted/ttf/NotoSans-Bold.ttf'],
  ['NotoSans-Italic.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSans/hinted/ttf/NotoSans-Italic.ttf'],
  ['NotoSans-BoldItalic.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSans/hinted/ttf/NotoSans-BoldItalic.ttf'],

  // Thai
  ['NotoSansThai-Regular.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansThai/hinted/ttf/NotoSansThai-Regular.ttf'],
  ['NotoSansThai-Bold.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansThai/hinted/ttf/NotoSansThai-Bold.ttf'],

  // CJK — regional subsets (~16MB each). noto.css maps each to the right unicode-range.
  ['NotoSansCJKsc-Regular.otf',
    'https://cdn.jsdelivr.net/gh/notofonts/noto-cjk@main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf'],
  ['NotoSansCJKtc-Regular.otf',
    'https://cdn.jsdelivr.net/gh/notofonts/noto-cjk@main/Sans/OTF/TraditionalChinese/NotoSansCJKtc-Regular.otf'],
  ['NotoSansCJKjp-Regular.otf',
    'https://cdn.jsdelivr.net/gh/notofonts/noto-cjk@main/Sans/OTF/Japanese/NotoSansCJKjp-Regular.otf'],
  ['NotoSansCJKkr-Regular.otf',
    'https://cdn.jsdelivr.net/gh/notofonts/noto-cjk@main/Sans/OTF/Korean/NotoSansCJKkr-Regular.otf'],

  // Hebrew
  ['NotoSansHebrew-Regular.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansHebrew/hinted/ttf/NotoSansHebrew-Regular.ttf'],
  ['NotoSansHebrew-Bold.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansHebrew/hinted/ttf/NotoSansHebrew-Bold.ttf'],

  // Arabic
  ['NotoSansArabic-Regular.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansArabic/hinted/ttf/NotoSansArabic-Regular.ttf'],
  ['NotoSansArabic-Bold.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansArabic/hinted/ttf/NotoSansArabic-Bold.ttf'],

  // Myanmar (Burmese)
  ['NotoSansMyanmar-Regular.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansMyanmar/hinted/ttf/NotoSansMyanmar-Regular.ttf'],
  ['NotoSansMyanmar-Bold.ttf',
    'https://cdn.jsdelivr.net/gh/notofonts/notofonts.github.io@main/fonts/NotoSansMyanmar/hinted/ttf/NotoSansMyanmar-Bold.ttf'],
];

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const doRequest = (u) => {
      https.get(u, (res) => {
        if (res.statusCode === 301 || res.statusCode === 302) {
          res.resume();
          doRequest(res.headers.location);
          return;
        }
        if (res.statusCode !== 200) {
          reject(new Error(`${u} -> HTTP ${res.statusCode}`));
          return;
        }
        const file = fs.createWriteStream(dest);
        res.pipe(file);
        file.on('finish', () => file.close(() => resolve()));
        file.on('error', reject);
      }).on('error', reject);
    };
    doRequest(url);
  });
}

(async () => {
  ensureDir(OUT_DIR);
  fs.writeFileSync(path.join(OUT_DIR, 'OFL.txt'), OFL_TEXT, 'utf8');
  console.log(`[fonts] target dir: ${OUT_DIR}`);
  console.log(`[fonts] wrote OFL.txt`);

  let ok = 0, fail = 0, totalBytes = 0;
  for (const [name, url] of FONTS) {
    const dest = path.join(OUT_DIR, name);
    process.stdout.write(`[fonts] ${name.padEnd(34)} `);
    try {
      await download(url, dest);
      const size = fs.statSync(dest).size;
      totalBytes += size;
      console.log(`OK  (${(size / 1024 / 1024).toFixed(2)} MB)`);
      ok++;
    } catch (e) {
      console.log(`FAIL  ${e.message}`);
      fail++;
    }
  }

  console.log(`\n[fonts] done.  succeeded: ${ok},  failed: ${fail},  total: ${(totalBytes / 1024 / 1024).toFixed(1)} MB`);
  if (fail > 0) {
    console.log('[fonts] Re-run to retry.  Failed paths:');
    FONTS.forEach(([n]) => console.log(`         ${path.join(OUT_DIR, n)}`));
  }
})();
