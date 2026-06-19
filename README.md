# html2Pdf — 多语言 HTML → PDF POC

技术预研项目，验证完整链路：**Vue 前端输入 → MySQL 存储 HTML → Java (iText 7) 按语种将 HTML 转为 PDF**。

## 已选技术栈

| 层 | 技术 | 备注 |
|---|---|---|
| 前端 | Vue 3 + Vite 6 | |
| UI | Element Plus 2.9 | 含 el-table、el-select |
| 富文本 | wangEditor v5 (5.1.x) | 中文友好，免费，Vue 3 兼容版用 `@next` |
| 后端 | Spring Boot 3.3.5 + Java 17 | 本机 23 已兼容 |
| PDF | iText 7 (8.0.5) + pdfHTML (5.0.5) | AGPL，仅限内部预研 |
| 字体 | Noto Sans + Noto Sans Thai + Noto Sans Arabic + Noto Sans CJK (sc/tc/jp/kr) | OFL 1.1，可商用 |
| 数据库 | MySQL 8.0 | 字符集 `utf8mb4` |

## 关键设计：多语言字体

直接 `font-family` 切换在 iText 7 里不可靠（缺字形会显示 tofu）。本项目用 **CSS `@font-face` + `unicode-range`**：多个字体声明为同一个 `AppFont` 字体族，pdfHTML 按字符码点自动选字体。

`backend/src/main/resources/fonts/noto.css` 把 6 个 Noto Sans 系列字体声明为同一 `AppFont`：

| 字体文件 | unicode-range | 覆盖语言 |
|---|---|---|
| NotoSans-Regular/Bold/Italic/BoldItalic | 拉丁 / 西里尔 / 越南 / 捷克 / 希腊 | 德、法、意、西、葡、俄、保 |
| NotoSansThai-Regular/Bold | U+0E00-0E7F | 泰文 |
| NotoSansCJKjp-Regular.otf | Hiragana / Katakana / 半角全角 | 日文假名 |
| NotoSansCJKkr-Regular.otf | Hangul / Jamo | 韩文音节 |
| NotoSansCJKtc-Regular.otf | CJK 扩展 A / 兼容汉字 / Kangxi | 繁中专用字 |
| NotoSansCJKsc-Regular.otf | CJK 统一汉字主体 (U+4E00-9FFF) | 简中 + 通用 CJK |
| NotoSansArabic-Regular/Bold | U+0600-06FF 等 | 阿拉伯 + 波斯/乌尔都/普什图 |

每个区域使用对应字体；混合文字（如 "Hello 你好"）由 pdfHTML 自动切换。

## 目录结构

```
SpringProject/
├── backend/
│   ├── src/main/
│   │   ├── java/com/example/agreement/
│   │   │   ├── AgreementApplication.java
│   │   │   ├── config/CorsConfig.java
│   │   │   ├── controller/AgreementController.java
│   │   │   ├── dto/{AgreementRequest,LanguageOption}.java
│   │   │   ├── entity/Agreement.java
│   │   │   ├── exception/{GlobalExceptionHandler,NotFoundException}.java
│   │   │   ├── repository/AgreementRepository.java
│   │   │   └── service/{AgreementService,LanguageService,PdfService}.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── db/init.sql
│   │       ├── languages.json
│   │       └── fonts/{noto.css,OFL.txt,NotoSans*.ttf,NotoSansCJK*.otf}
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/agreement.js
│   │   ├── components/{LanguageSelect,RichTextEditor,AgreementList}.vue
│   │   ├── views/Home.vue
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── scripts/
│   └── download-fonts.js        # 一键下载 Noto 字体（从 jsDelivr CDN）
└── README.md
```

## API 列表

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/languages` | 获取支持的语种列表 |
| GET | `/api/agreements` | 协议列表 |
| GET | `/api/agreements/{id}` | 协议详情 |
| POST | `/api/agreements` | 创建协议（JSON: language, title?, content） |
| DELETE | `/api/agreements/{id}` | 删除协议 |
| GET | `/api/agreements/{id}/pdf` | **PDF 下载**（`application/pdf`） |

## 运行步骤

### 1. 初始化数据库

```bash
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p < backend/src/main/resources/db/init.sql
```

数据库密码**不**写进 `application.yml`，而是通过 `DB_PASSWORD` 环境变量注入（见下）。`.env.example` 是模板。

### 2. 下载字体（首次运行）

```bash
node scripts/download-fonts.js
```

脚本会从 jsDelivr CDN 拉取 10 个 Noto 字体到 `backend/src/main/resources/fonts/`（共约 65 MB）。已下载可跳过。

### 3. 启动后端

设置 `DB_PASSWORD` 后再启动（**必须**，否则 `application.yml` 的 `${DB_PASSWORD:SET_DB_PASSWORD_ENV_VAR}` 占位符会让应用启动失败）：

```bash
# bash / git-bash
export DB_PASSWORD=your_mysql_password
cd backend
mvn spring-boot:run

# PowerShell
$env:DB_PASSWORD="your_mysql_password"
cd backend
mvn spring-boot:run
```

或直接运行打包后的 jar：

```bash
mvn -DskipTests package
DB_PASSWORD=your_mysql_password java -jar target/agreement-0.0.1-SNAPSHOT.jar
```

后端监听 `http://localhost:8090`（避开 GeneaCloud 等占用 8080 端口的项目）。

### 4. 启动前端

```bash
cd frontend
npm install   # 已安装可跳过
npm run dev
```

前端开发服务器：`http://localhost:5173`（Vite 配置了 `/api` 代理到 8090）。

## 验证清单

冒烟测试每个语种 → 列表显示 → 下载 PDF → 打开 PDF 视觉确认无 tofu：

- 泰文（th）→ สวัสดี + 列表 → PDF 验证泰文（注意元音位置）
- 简体中文（zh-CN）→ 你好世界 + 富文本 → PDF 验证 CJK 简体
- 繁体中文（zh-TW）→ 繁體中文 → PDF 验证 CJK 繁体
- 日文（ja）→ こんにちは → PDF 验证假名 + 汉字
- 韩文（ko）→ 안녕하세요 → PDF 验证 Hangul
- 越南语（vi）→ Tiếng Việt có dấu → PDF 验证变音符号
- 德语 / 法语 / 意大利语 / 西班牙语 / 葡萄牙语 / 荷兰 / 波兰 / 瑞典 / 挪威 / 丹麦 / 芬兰 → 同 Latin 测试（覆盖拉丁扩展字符）
- 俄语 / 保加利亚语 / 乌克兰语 / 白俄 / 塞尔维亚（西里尔字母）→ PDF 验证
- 希腊语（el）→ PDF 验证希腊字母
- 阿拉伯语（ar）→ مرحبا بالعالم → PDF 验证（注意 RTL 排版）
- 波斯语（fa）→ فارسی → PDF 验证
- 希伯来语（he）→ עברית → PDF 验证
- 捷克语（cs）→ háčky → PDF 验证变音

跨语言混合：选中文，编辑中英文混排"Hello 你好 World 世界" → 验证 PDF 中英文字体自动切换。

## 已知限制

- **iText 7 AGPL**：仅限内部预研。如要对外提供 PDF 生成服务，需购买商用授权或迁移到 OpenPDF（LGPL/MPL，HTML 支持弱一些）。
- **JAR 体积**：因 CJK 字体大，jar 包约 116 MB。生产环境建议用 fonttools 对 CJK 字体做按字形 subsetting。
- **未做**：用户登录鉴权、富文本图片上传到 OSS、列表分页、表单国际化（错误提示默认中文）。
- **测试数据**：数据库密码通过 `DB_PASSWORD` 环境变量注入，**不要**写进 `application.yml`（避免误推到 git）。

## 排错速查

| 现象 | 排查 |
|---|---|
| 启动报 `Unsupported character encoding 'utf8mb4'` | JDBC URL 应用 `characterEncoding=UTF-8`（Java 字符集），数据库用 `utf8mb4` 字符集 |
| 启动报 Lombok 找不到 getter | 确认 `pom.xml` 有 `maven-compiler-plugin` 的 `annotationProcessorPaths` 指向 Lombok |
| PDF 中文变 tofu | 确认 `backend/src/main/resources/fonts/` 10 个字体文件就位，且 `noto.css` 里的字体名拼写正确 |
| 泰文元音位置错 | 确认 `NotoSansThai-Regular.ttf` 已下载（37 KB），不是空文件 |
| 后端连不上 MySQL | 确认 MySQL80 服务已启动（`net start MySQL80`），密码正确 |
| 前端跨域报错 | 确认 Vite 代理配置存在（已在 `vite.config.js`），或访问经 Vite 不要直连 8080 |
