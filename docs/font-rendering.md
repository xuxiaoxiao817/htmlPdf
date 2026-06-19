# 多语言字体渲染原理与调用链

本文档说明 `html2Pdf` 项目如何把存进 MySQL 的富文本 HTML 渲染为多语言 PDF，覆盖 43 个语种（含 CJK、泰文、阿拉伯、希伯来等需要特殊字形处理的脚本）而不出现"豆腐块"（tofu）。重点是后端代码路径。

## 1. 问题与总体方案

**问题**：iText 7 内置的 `DefaultFontProvider` **不会做字形级 fallback**——如果注册的字体缺某个字符的 glyph，它直接输出 `.notdef`（tofu），不会自动跳到另一已注册字体去找。要在一个 PDF 内同时正确渲染"你好 + สวัสดี + مرحبا + ελληνικά"，必须自己组织字体策略。

**方案**：借 pdfHTML 的"类浏览器"渲染管线，在 HTML head 里嵌入一段 CSS `@font-face` 列表，把 14 个 Noto Sans 字体文件全部声明为同一字体族名 `AppFont`，每条声明带一个 `unicode-range` 划定负责的码点段。pdfHTML 解析 CSS 时建好"码点 → 字体"映射，渲染文字时按字符逐码点挑字体。`DefaultFontProvider` 仅作"按名查找"的兜底。

| 层 | 角色 | 位置 |
|---|---|---|
| 字体文件 | 14 个 Noto Sans 字体（OFL 1.1） | `backend/src/main/resources/fonts/` |
| 字体声明 | 12 条 `@font-face` 规则 | `backend/src/main/resources/fonts/noto.css` |
| 渲染服务 | 读 CSS → 嵌入 HTML → 调 iText | `service/PdfService.java` |
| API 入口 | 接收 `id` → 返回 `application/pdf` | `controller/AgreementController.java` |
| 数据层 | MySQL 存 HTML 原文（`LONGTEXT utf8mb4`） | `db/init.sql` |
| 语种清单 | 43 条静态 JSON | `resources/languages.json` |

## 2. 字体覆盖矩阵

| 字体文件 | `unicode-range` | 负责语种 |
|---|---|---|
| `NotoSans-Regular.ttf` / `-Bold` / `-Italic` / `-BoldItalic` | 拉丁 / 拉丁扩展 / 西里尔 / 希腊 / 越南 / 捷克 / 土耳其 | en, de, fr, it, es, pt, nl, pl, sv, no, da, fi, tr, ro, hu, cs, ru, uk, bg, be, kk, el, vi, id, ms, tl, ca, sk, sl, hr, sr, lt, lv, et, is |
| `NotoSansThai-Regular.ttf` / `-Bold` | `U+0E00-0E7F` | th（泰文，需 OpenType 形变才能摆正元音位置） |
| `NotoSansHebrew-Regular.ttf` / `-Bold` | `U+0590-05FF` + `U+FB1D-FB4F` | he（希伯来） |
| `NotoSansArabic-Regular.ttf` / `-Bold` | `U+0600-06FF` 等 | ar, fa（阿拉伯 / 波斯共享阿拉伯字母；需 RTL + 连字处理） |
| `NotoSansCJKsc-Regular.otf` | `U+4E00-9FFF` 等 CJK 主体 + 标点 | zh-CN 简体 + 通用 CJK |
| `NotoSansCJKtc-Regular.otf` | `U+3400-4DBF` 扩展 A + 兼容汉字 | zh-TW 繁中专用字形 |
| `NotoSansCJKjp-Regular.otf` | `U+3040-309F` 平假名 / `U+30A0-30FF` 片假名 | ja 假名 + JP 汉字形 |
| `NotoSansCJKkr-Regular.otf` | `U+AC00-D7AF` 谚文音节 | ko 谚文 + KR 汉字 |

**为什么不合并成单一 CJK**：NotoSansCJK 拆 SC/TC/JP/KR 四份是因为同一汉字在不同地区有不同字形（如"骨/骨"、"海/海"）。`unicode-range` 让"日文"用日文专属字形、"繁中"用繁中专属字形，简单混用会导致繁中用户看到日文风字形。

## 3. 关键设计：CSS `unicode-range` vs 默认 FontProvider

iText 7 的 `DefaultFontProvider.addFont(bytes)` 只把字体加入"按名解析"池：

```java
DefaultFontProvider fp = new DefaultFontProvider(true, true, true);
fp.addFont(notoSansBytes);  // 缺中文字形 → 输出 .notdef，不回退到 CJK 字体
```

而 CSS 的 `@font-face` + `unicode-range` 是 iText 7 内部 **按 codepoint 路由字体**的唯一可靠方式：

```css
@font-face {
  font-family: 'AppFont';
  src: url('fonts/NotoSansArabic-Regular.ttf') format('truetype');
  unicode-range: U+0600-06FF, U+FE70-FEFF;
}
@font-face {
  font-family: 'AppFont';
  src: url('fonts/NotoSansCJKsc-Regular.otf') format('opentype');
  unicode-range: U+4E00-9FFF;
}
body { font-family: 'AppFont', sans-serif; }
```

一段 HTML 里写"Hello 你好 مرحبا"时，pdfHTML 逐字符找第一个 `unicode-range` 包含该码点的 `@font-face`，找到就用它对应的字体文件。CSS 解析结果存在 iText 内部 `CssFontFaceRule` 链表中，是 v8.0 起 pdfHTML 唯一支持的"多脚本混排"机制。

## 4. 完整调用链

```
[前端 Vue]                    [后端 Spring Boot]                    [存储]
─────────────────             ────────────────────────               ─────
Home.vue                      AgreementController.downloadPdf(id)    MySQL
  └─ axios GET                  ├─ agreementService.get(id) ────────► agreements
     /api/agreements/{id}/pdf   │     └─ JPA findById
     /pdf ◄───────────────────  ├─ pdfService.renderPdf(...)
                                │     ├─ 读 classpath:fonts/noto.css
                                │     ├─ HTML_TEMPLATE.formatted(lang, css, content)
                                │     ├─ ConverterProperties
                                │     │   ├─ setCharset("UTF-8")
                                │     │   ├─ setBaseUri("classpath:/")
                                │     │   └─ setFontProvider(fp)
                                │     │       └─ fp.addFont(14 个字体 bytes)
                                │     └─ HtmlConverter.convertToPdf(html, pdf, props)
                                │           └─ 内部：CSS 解析 → unicode-range 建表
                                │                     → 逐码点选字体 → 输出 PDF 流
                                ├─ pdfService.suggestedFileName(id, lang)
                                └─ ResponseEntity<byte[]>
                                     Content-Type: application/pdf
                                     Content-Disposition: attachment
                                                       │ (PDF 字节)
[浏览器]                       [下载]
──────────                     ──────
<a download> 触发                agreement_<id>_<lang>_<yyyyMMdd>.pdf
URL.createObjectURL(blob)
```

## 5. 关键方法源码级解析（`PdfService.java`）

### 5.1 `renderPdf(String htmlContent, String language, String title)`

入口方法，三步走。

**第一步：构造完整 HTML**（`PdfService.java:25-38`）

```java
private static final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="%s">
        <head>
          <meta charset="UTF-8"/>
          <title>Agreement</title>
          <style>%s</style>
        </head>
        <body>
          <div class="content">%s</div>
        </body>
        </html>
        """;
```

- `lang="%s"` 注入 `BCP 47` 语种代码，让 iText 在 RTL 场景下正确处理 `<html dir>` 默认值。
- `<style>` 内联 `noto.css` 全文，绕开"额外 CSS 文件路径解析"的坑。
- `<div class="content">` 包裹用户富文本，`noto.css` 里的 `.content p/h1/ul` 等选择器只作用于该子树。

**第二步：配 `ConverterProperties`**（`PdfService.java:53-78`）

```java
ConverterProperties props = new ConverterProperties();
props.setCharset("UTF-8");
props.setBaseUri(ResourceUtils.getURL(ResourceUtils.CLASSPATH_URL_PREFIX).toString());
// → classpath:/   后续 CSS 里 url('fonts/xxx.ttf') 相对此 URI 解析

DefaultFontProvider fontProvider = new DefaultFontProvider(true, true, true);
//                       ↑        ↑         ↑
//                       useSys   embed    subset
//                       fonts
registerFontSafely(fontProvider, "fonts/NotoSans-Regular.ttf");
// ... 12 个 registerFontSafely 调用
props.setFontProvider(fontProvider);
```

| 构造器参数 | 含义 |
|---|---|
| `useSystemFonts = true` | 仍允许 iText 使用 OS 已装字体（兜底，缺字时回退到默认） |
| `embed = true` | 把字体子集嵌进 PDF（无嵌入则不同机器显示会回退到系统字体） |
| `subset = true` | 只嵌 PDF 实际用到的 glyph，缩体积（可惜 NotoSansCJK ~16MB 不会变，因为字形本身就多） |

**第三步：转换**（`PdfService.java:80-89`）

```java
try (PdfWriter writer = new PdfWriter(out);
     PdfDocument pdf = new PdfDocument(writer)) {
    pdf.setDefaultPageSize(PageSize.A4);
    HtmlConverter.convertToPdf(fullHtml, pdf, props);
}
```

`HtmlConverter.convertToPdf` 内部 pipeline：

1. 用 jsoup-like 解析器把 HTML 转成 W3C DOM 树
2. 解析 `<style>`，构造 `CssFontFaceRule` 链表（按 `unicode-range` 索引）
3. 遍历 DOM，逐个文本节点按字符切片：每段连续相同字体范围的字符 → 调 iText `Canvas.showText` 写一行
4. 输出 PDF 流

### 5.2 `registerFontSafely(DefaultFontProvider, String)`（`PdfService.java:101-109`）

```java
private void registerFontSafely(DefaultFontProvider provider, String classpathPath) {
    try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
        byte[] bytes = in.readAllBytes();
        provider.addFont(bytes);
        log.debug("registered font: {}", classpathPath);
    } catch (IOException e) {
        log.warn("font not available, skipping: {}", classpathPath);
    }
}
```

**为什么用 `addFont(bytes)` 而不是 `addFont(path)`**：

- `addFont(path)` 是基于文件路径，加载发生在首次渲染时——但 Spring Boot jar 内资源是嵌套在 `BOOT-INF/classes/` 里的，文件路径 API 找不到。
- `addFont(bytes)` 接受已读取的字节数组，绕开文件系统解析，让字体在 fat jar / dev IDE 两种运行环境行为一致。

`try/catch` 兜底缺字体场景：万一某个字体文件没下载成功，**PDF 仍能生成**（用 OS 兜底字体或 `.notdef`），不会让一个 100MB 的项目因 27KB 的 Hebrew 字体缺失就 500。

### 5.3 `readClasspathFile(String)`（`PdfService.java:111-117`）

读 `noto.css` 用 `StandardCharsets.UTF_8` 显式解码。**不能**用默认字符集——Windows 中文环境下默认 GBK，CSS 里的中文注释会变乱码，最终 iText 解析 CSS 失败抛异常。

### 5.4 `escapeXml(String)`（`PdfService.java:119-124`）

只对 `<title>` 内的 `title` 参数做 XML 转义，因为 `HTML_TEMPLATE` 把它拼进 `<title>` 节点（XML 必须转义）。**不**对用户富文本 `content` 做转义——富文本本身就是 HTML，要原样注入 `<div class="content">`，由 iText 当 HTML 解析。

## 6. Controller 串接（`AgreementController.java:53-64`）

```java
@GetMapping("/agreements/{id}/pdf")
public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
    Agreement a = agreementService.get(id);
    byte[] pdf = pdfService.renderPdf(a.getContent(), a.getLanguage(), a.getTitle());
    String filename = pdfService.suggestedFileName(a.getId(), a.getLanguage());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(pdfService.contentType()));
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(pdf.length);
    return new ResponseEntity<>(pdf, headers, 200);
}
```

- `a.getContent()` 直接是 MySQL 存的 HTML 原文（`<p>...</p>`、`<strong>` 都在），不做任何转义。
- `Content-Disposition: attachment; filename=...` 让浏览器弹下载框而不是内嵌预览。
- 文件名格式 `agreement_{id}_{lang}_{yyyyMMdd}.pdf`（`PdfService.suggestedFileName`，line 92-95）。

## 7. 数据存储格式

`db/init.sql` 的表结构：

```sql
CREATE TABLE agreements (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  language    VARCHAR(10)  NOT NULL,    -- BCP 47 代码：zh-CN / ja / th
  title       VARCHAR(255),
  content     LONGTEXT     NOT NULL,     -- 原文是 HTML，富文本编辑器原样输出
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_language (language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- `LONGTEXT` 容量 ~4GB，远超富文本需求。
- `utf8mb4` 是 MySQL 字符集；JDBC URL 里却要写 `characterEncoding=UTF-8`（Java 字符集）——**两者不是一回事**，混用会启动报 `Unsupported character encoding 'utf8mb4'`。
- `language` 列存 BCP 47 简码（`zh-CN` 而非 ISO 639-3 `zho`），跟 `languages.json` 的 `code` 字段对齐。

## 8. 新增语种的标准流程

按"成本由低到高"分三档。

### A 档：零成本（仅 `languages.json` 加条目）

适用条件：所需字符**已**被某个现有 `unicode-range` 覆盖。例如要加克罗地亚语（`hr`）：

- 克罗地亚用拉丁字母 + 几个变音字符（`Č`, `Š`, `Ž`）→ 全在 `NotoSans-Regular.ttf` 的 `U+0100-017F`（Latin Extended-A）段
- 改动：只改 `backend/src/main/resources/languages.json`，加一行 `{ "code": "hr", "name": "Croatian", "nativeName": "Hrvatski" }`
- 不需要重打包字体

当前所有 A 档语种（已加）：`id, ms, tl, ca, sk, sl, hr, sr, lt, lv, et, is, be, kk`

### B 档：需小字体（~30-80 KB）

适用条件：脚本有独立码点段且未被现有字体覆盖。例如希伯来 `U+0590-05FF`。

1. 下载字体到 `backend/src/main/resources/fonts/`（参考 `scripts/download-fonts.js` 的 jsDelivr 路径）
2. 在 `scripts/download-fonts.js` 的 `FONTS` 数组追加 2 条（Regular + Bold）——让脚本可重跑
3. 在 `fonts/noto.css` 加 `@font-face` 段：

```css
@font-face {
  font-family: 'AppFont';
  src: url('fonts/NotoSansHebrew-Regular.ttf') format('truetype');
  unicode-range: U+0590-05FF, U+FB1D-FB4F;
}
@font-face {
  font-family: 'AppFont';
  font-weight: bold;
  src: url('fonts/NotoSansHebrew-Bold.ttf') format('truetype');
  unicode-range: U+0590-05FF, U+FB1D-FB4F;
}
```

4. 在 `PdfService.java` 的字体注册段追加 `registerFontSafely(...)` 两行
5. 在 `languages.json` 加语种条目
6. 重新 `mvn package` 验证：jar 内 `BOOT-INF/classes/fonts/NotoSansHebrew-*.ttf` 存在

### C 档：需大字体（每套 ~5-16 MB）

印度 / 东南亚 / 高加索等需要全新字体的语种（天城文、孟加拉、泰米尔、格鲁吉亚、亚美尼亚等）。操作同 B 档，但 `unicode-range` 段需查 Unicode 块表精确划分，**不能**用整段 `U+0000-FFFF`（会把其他字体的码点抢走导致回退失效）。

## 9. 已知限制

- **iText 7 AGPL**：仅限内部预研 / 不分发 jar。如对外服务化需购买商用授权或迁移 OpenPDF。
- **CJK 字体不 subset**：iText 的 subset 算法对超大 CJK OTF 支持有限，单个 PDF 内用到的中文字形会全量嵌入。生产环境应在 CI 用 `fonttools` 预做字形子集化，把 16MB 的 OTF 压到 ~500KB-2MB。
- **字体下载源**：脚本走 jsDelivr CDN（GitHub 在国内不稳）。如 jsDelivr 抽风，临时改 `scripts/download-fonts.js` 的 URL 为 `https://raw.githubusercontent.com/notofonts/...` 配合代理。
- **RTL 语种**：阿拉伯 / 希伯来依赖 pdfHTML 的 RTL 算法自动处理，但表格内 RTL 单元格目前不支持——非 POC 需求范围内。
- **未做**：登录鉴权、列表分页、富文本图片上传到 OSS、错误提示的 i18n。
