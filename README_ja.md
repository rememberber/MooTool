![logo](assets/logo/logo-128.png)

<p align="center">
  <a href="README.md">English</a> ·
  <a href="README_zh_CN.md">简体中文</a> ·
  <strong>日本語</strong>
</p>

# MooTool

開発者向けデスクトップツールセット

[![码云Gitee](https://gitee.com/zhoubochina/MooTool/badge/star.svg?theme=blue)](https://gitee.com/zhoubochina/MooTool)
[![GitHub stars](https://img.shields.io/github/stars/rememberber/MooTool.svg)](https://github.com/rememberber/MooTool)
[![GitHub release](https://img.shields.io/github/v/release/rememberber/MooTool)](https://github.com/rememberber/MooTool/releases)
[![GitHub license](https://img.shields.io/github/license/rememberber/MooTool)](https://github.com/rememberber/MooTool/blob/master/LICENSE.txt)

<a href="https://hellogithub.com/repository/4e5f287079734f98890a69d56000b361" target="_blank"><img src="https://api.hellogithub.com/v1/widgets/recommend.svg?rid=4e5f287079734f98890a69d56000b361&claim_uid=0UhXFJvP9ndHtiB" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>

## 対応プラットフォーム
Windows • Linux • macOS

## スクリーンショット

> 主要機能のスクリーンショットは以下の通りです。翻訳、UA 分析、画像アシスタント、PDF、環境変数、システム情報、設定ファイル変換、Protobuf などのモジュールは個別の画像がありません。インストール後、該当タブでお試しください。

![クイックメモ](screen_shoot/quick_note_2_mac.png)

![クイックメモ - クイック置換](screen_shoot/quick_replace_mac.png)

![Overview](screen_shoot/time_mac_2.png)

![Overview](screen_shoot/json_mac_2.png)

![Overview](screen_shoot/host_mac_2.png)

![Overview](screen_shoot/http_mac_2.png)

![Overview](screen_shoot/encode_mac_2.png)

![Overview](screen_shoot/qr_code_mac_2.png)

![Overview](screen_shoot/crypto_mac_2.png)

![Overview](screen_shoot/calculator_mac_2.png)

![Overview](screen_shoot/net_mac_2.png)

![Overview](screen_shoot/color_board_mac_2.png)

![Overview](screen_shoot/regex_mac_2.png)

![Overview](screen_shoot/cron_mac_2.png)

![Overview](screen_shoot/java_mac_2.png)

![Overview](screen_shoot/diff_mac.png)

![Overview](screen_shoot/mt-favoriteColor.png)

![Overview](screen_shoot/quick_note_light_mac.png)

![テーマと外観](screen_shoot/theme.png)

## ダウンロード

- [MooTool Next Electron 1.0.7（推奨）](https://github.com/rememberber/MooTool/releases/tag/next-electron-v1.0.7)
- [MooTool Java 1.8.2](https://github.com/rememberber/MooTool/releases/tag/v1.8.2)
- [GitHub Releases 一覧](https://github.com/rememberber/MooTool/releases)
- [Gitee の MooTool Java Releases](https://gitee.com/zhoubochina/MooTool/releases)

## 支援 / スポンサー

**MooTool がお役に立った場合は、日々の仕事終わりや週末に費やした開発への支援をご検討ください。皆さまの応援が励みになります。**

![zanshang](assets/material/wx-zanshang.jpg)

# MooTool 機能一覧

> 多くのモジュールに **履歴** サブタブがあり、検索・適用・入出力のコピー・削除・全消去に対応しています。

```text
MooTool
├── クイックメモ
│   ├── 多言語シンタックスハイライト
│   ├── 主要言語のコード整形
│   │   ├── SQL
│   │   ├── JSON
│   │   └── Java
│   ├── 番号付き / 箇条書きリスト
│   ├── Markdown リアルタイムプレビュー
│   ├── Markdown への画像挿入
│   ├── エクスポート / 一括エクスポート、検索、全体検索
│   ├── ドキュメント情報（作成/更新日時、文字数など）
│   ├── リンク検出とクリックで開く
│   ├── フォント、サイズ、リストの色
│   └── クイック置換パネル
│       ├── サイドパネルで以下の一括テキスト操作（選択行のみも可）：
│       ├── 自動保存
│       ├── 空白を削除
│       ├── 空行を削除
│       ├── タブ (\t) を削除
│       ├── 指数表記 → 通常の数値
│       ├── 通常の数値 → 指数表記
│       ├── 通常の数値 → 桁区切り
│       ├── 桁区切り → 通常の数値
│       ├── snake_case → camelCase
│       ├── camelCase → snake_case
│       ├── 大文字 → 小文字
│       ├── 小文字 → 大文字
│       ├── 改行を削除
│       ├── 改行 → カンマ
│       ├── 改行 → ','
│       ├── 改行 → ","
│       ├── カンマ → 改行
│       ├── ',' → 改行
│       ├── "," → 改行
│       ├── タブ (\t) → 改行
│       ├── 行単位で重複削除
│       ├── 行単位で重複削除し出現回数を集計
│       ├── エスケープ
│       ├── アンエスケープ
│       ├── 行を逆順に
│       ├── 行を A→Z でソート
│       ├── 行を Z→A でソート
│       └── ピンイン順でソート
├── 時刻変換
│   ├── タイムスタンプ変換
│   │   ├── 日時 → タイムスタンプ（ミリ秒）
│   │   ├── タイムスタンプ（ミリ秒）→ 日時
│   │   ├── タイムスタンプ（秒）→ 日時
│   │   └── 日時 → タイムスタンプ（秒）
│   ├── 履歴
│   ├── フルスクリーン時計
│   └── タイムゾーン選択とショートカット
├── JSON
│   ├── JSON 整形
│   │   ├── キーをアルファベット順にソート
│   │   ├── キーの大文字小文字を無視
│   │   └── 重複キーをチェック
│   ├── JSON 圧縮
│   ├── エクスポート / 一括エクスポート、検索
│   ├── フォントとサイズ
│   ├── JSON キーと値の入れ替え
│   ├── JSON → XML
│   ├── XML → JSON
│   ├── JavaBean → JSON
│   ├── JSON → JavaBean
│   ├── エスケープ
│   ├── アンエスケープ
│   ├── JSON Path で JSON データを取得
│   └── JSON Path をビジュアル選択
├── 翻訳
│   ├── 20 以上の言語を相互翻訳（中/英/日/韓/仏/西/独/露など、自動検出対応）
│   ├── Google / Bing 翻訳（失敗時に自動フォールバック）
│   ├── 単語帳に保存
│   ├── 単語帳
│   │   ├── 検索、新規、編集、削除
│   │   └── 再翻訳
│   └── 翻訳履歴
├── Host
│   ├── Host 整形 / シンタックスハイライト
│   ├── システム hosts の管理 / 表示
│   ├── Host のインポート / エクスポート
│   └── 検索、検索と置換
├── HTTP
│   ├── HTTP リクエスト（GET/POST/PUT/DELETE/HEAD/PATCH/OPTIONS）
│   ├── cURL インポート
│   ├── HTTP ヘッダー / ボディの整形
│   ├── リクエスト管理
│   ├── リクエスト履歴
│   └── 検索
├── UA 分析
│   ├── User-Agent 解析（ブラウザ、エンジン、OS、デバイスタイプ/ブランド/モデルなど）
│   ├── モバイル / Bot クローラーの検出
│   ├── プリセット UA（Chrome、Firefox、Safari、Edge、WeChat 内蔵ブラウザ、curl など）
│   ├── 貼り付け / クリア
│   └── 履歴
├── エンコード / デコード
│   ├── Native → Unicode
│   ├── Unicode → Native
│   ├── URL エンコード / デコード
│   ├── Native → 16 進数
│   ├── 16 進数 → Native
│   ├── Native → ASCII（10/16 進コードポイント）
│   ├── ASCII → Native
│   └── 履歴
├── QR コード
│   ├── QR コード生成
│   │   ├── サイズをカスタム
│   │   ├── 誤り訂正レベルをカスタム
│   │   └── ロゴをカスタム
│   ├── QR コード解析
│   ├── クリップボードから読み取り
│   └── 履歴
├── 暗号化 / ランダム
│   ├── 中国国密 SM2 / SM3 / SM4 に対応
│   ├── 対称暗号の暗号化 / 復号
│   │   ├── AES
│   │   ├── DES
│   │   └── SM4
│   ├── 非対称暗号の暗号化 / 復号
│   │   ├── RSA
│   │   └── SM2（暗号化/復号、秘密鍵署名、公開鍵検証）
│   ├── ダイジェスト（ファイル / テキスト）
│   │   ├── MD5
│   │   ├── SHA1
│   │   ├── SHA256
│   │   ├── SHA384
│   │   ├── SHA512
│   │   └── SM3
│   ├── Base64 エンコード / デコード
│   ├── Base32 エンコード / デコード
│   ├── ランダム UUID 生成
│   ├── 数字 / 英字 / 英数字のランダム文字列（桁数指定）
│   ├── 複雑なランダムパスワード（桁数指定）
│   └── 履歴
├── 電卓
│   ├── 四則演算
│   ├── 進数変換
│   ├── 最大公約数
│   ├── 最小公倍数
│   ├── 順列と組み合わせ
│   └── 履歴
├── ネットワーク / IP
│   ├── IP 検索
│   ├── ドメイン検索
│   ├── netstat
│   ├── ping
│   ├── IPv4 ↔ long 変換
│   ├── WHOIS 検索
│   └── DNS フラッシュ
├── カラーパレット
│   ├── テーマ / 標準カラー
│   ├── スクリーンカラーピッカー
│   ├── 自由な色選択
│   ├── カラー形式変換
│   ├── お気に入りカラー
│   ├── カラー演算（反転、交差、加算、差分、平均）
│   └── 履歴
├── 画像アシスタント
│   ├── ローカル画像ホスト
│   ├── スクリーンショット
│   ├── クリップボードから取得 / クリップボードへコピー
│   ├── ズームツールバー（拡大/縮小/原寸/ウィンドウに合わせる）
│   ├── 画像 Base64 エンコード / デコード
│   ├── 画像圧縮
│   ├── 画像ウォーターマーク
│   └── 画像 OCR（Tesseract）
├── Cron
│   ├── Cron 式ビルダー
│   ├── Cron 式解析（Linux 5 フィールド / Quartz 6〜7 フィールド）
│   ├── Cron 式検証
│   ├── Cron を自然言語に変換
│   ├── 次の 10 回の実行時刻
│   ├── お気に入り Cron 式
│   ├── 履歴
│   └── よく使う Cron 式
├── 正規表現
│   ├── 正規表現マッチテスト
│   ├── お気に入り正規表現
│   ├── よく使う正規表現
│   └── 履歴
├── Java
│   ├── Java/Groovy 整形とハイライト
│   ├── Java/Groovy インタプリタ実行
│   └── 履歴
├── フォーマット
│   ├── アップロードファイルの整形
│   │   ├── Nginx 設定
│   │   ├── XML
│   │   ├── HTML
│   │   └── Java
│   ├── 貼り付けて整形（Nginx / Java / XML / HTML）
│   └── 履歴
├── PDF
│   ├── PDF 分割
│   └── PDF 結合
├── 環境変数
│   ├── システム環境変数（表形式）
│   ├── Java properties
│   └── 更新 / エクスポート
├── システム情報
│   ├── OSHI でローカルのシステム/ハードウェア情報を収集。初回タブ表示または更新時に読み込み：
│   ├── システム（OS、コンピュータ、ファームウェア、マザーボードなど）
│   ├── CPU
│   ├── メモリ
│   ├── ストレージ
│   └── ネットワーク
├── 設定ファイル変換
│   ├── Properties → YAML
│   ├── YAML → Properties
│   ├── YAML 検証（構文と行番号）
│   ├── YAML 整形
│   ├── 履歴
│   ├── JSON → YAML（TODO）
│   └── YAML → JSON（TODO）
├── テキスト比較
│   ├── 左右並列比較（スクロール同期）
│   ├── 統合 diff
│   └── diff をコピー
├── Protobuf
│   ├── JSON ↔ Protobuf バイナリ変換
│   │   ├── Hex / Base64 出力
│   │   └── `.proto` 定義の整形
│   ├── Wire Format デコード（`.proto` 不要）
│   ├── Hex / Base64 変換
│   └── 履歴
└── アプリと設定
    ├── 同期とバックアップ（Git 同期、データエクスポート）
    ├── キーボードショートカット
    ├── データディレクトリのカスタム
    ├── 起動時に更新を確認
    ├── SQL 方言
    ├── システムトレイ
    ├── ウィンドウ動作
    │   ├── macOS / Windows：閉じるボタンで Dock / タスクバーに最小化、バックグラウンドで継続
    │   └── Linux：閉じるボタンでアプリを終了
    ├── 外観
    │   ├── 多数のテーマ（Flat Light/Dark、macOS、One Dark、Monokai など）
    │   ├── アクセントカラー
    │   ├── システムのアクセントに追従
    │   ├── ウィンドウ背景の没入型表示
    │   ├── 起動時に最大化
    │   └── タブをアイコンのみ表示
    └── レイアウト
        ├── パネル位置（上/下/左/右）
        └── 全体フォントとサイズ
```

## 謝辞

[Hutool](http://hutool.cn/)  
[FlatLaf](https://github.com/JFormDesigner/FlatLaf)  
[vscode-icons](https://github.com/microsoft/vscode-icons)  
[iconfont](https://www.iconfont.cn/)

## 開発者向けメモ

複数製品のバージョン、Git tag、GitHub Release、`Latest`、CI 分離ルールについては、[複数製品のリリース規約](RELEASE_CONVENTIONS.md)を参照してください。

最低 JDK：**21**  
開発を始める前に、**下図のとおり IntelliJ IDEA を設定**し、**maven clean** を実行してください：
![considerations](assets/material/gui_build.png)

### パッケージングと CI

パッケージング用 JDK をローカルにダウンロードしてキャッシュできます：

- JDK アーカイブ：`downloads/jdks/`
- 展開済み JDK：`jdks/<os>/<arch>/home`

ダウンロードスクリプトは Eclipse Temurin 21 を使用し、既存の JDK がある場合は再ダウンロードしません。

#### ローカルパッケージング JDK の準備

```bash
python3 scripts/prepare_jdks.py --targets mac-x64
python3 scripts/prepare_jdks.py --targets mac-arm64
python3 scripts/prepare_jdks.py --targets windows-x64
python3 scripts/prepare_jdks.py --targets linux-x64
```

すべてのダウンロード先を一度に確認：

```bash
python3 scripts/prepare_jdks.py --targets all --resolve-only
```

#### ローカルパッケージコマンド

デフォルトの `mvn clean package` は現在の JDK で macOS universal パッケージをビルドします。

プロファイル `mac-intel`、`mac-apple-silicon`、`windows-x64`、`linux-x64` は `validate` 段階で `jdks/` の JDK を検証します。先に `scripts/prepare_jdks.py` を実行しないとビルドは失敗します。

キャッシュ済み JDK で特定プラットフォーム向けにパッケージ：

```bash
mvn clean package -Pmac-intel -Dmaven.test.skip=true
mvn clean package -Pmac-apple-silicon -Dmaven.test.skip=true
mvn clean package -Pwindows-x64 -Dmaven.test.skip=true
mvn clean package -Plinux-x64 -Dmaven.test.skip=true
```

出力ディレクトリ：

- デフォルト：`target/`
- Intel Mac：`target/mac-intel/`
- Apple Silicon Mac：`target/mac-apple-silicon/`
- Windows x64：`target/windows-x64/`
- Linux x64：`target/linux-x64/`
