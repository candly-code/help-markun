# ヘルプマーくん（Help Markun）

障がいのある方と、手助けしたい人をつなぐ Android アプリです。

- **近くのヘルプ**：BLE タグを探し、Supabase に登録された方が近くにいると赤くお知らせします。アプリを閉じていてもバックグラウンドで見守り、見つけたら通知とアイコンのバッジで知らせます。
- **カードで確認**：FeliCa・NFC カードをスマホにタッチすると、登録された方の情報（お手伝いしてほしいこと・医療情報・緊急連絡先）を表示します。未登録のカードでも、カードの種類や ID を表示します。

Jetpack Compose と Material 3 Expressive で作っています（ライト／ダーク、文字サイズ、動きを控えめにする設定などに対応）。

## 必要なもの

- [Android Studio](https://developer.android.com/studio)（JDK も同梱されています）
- Android 7.0（API 24）以上の端末と USB ケーブル。BLE と NFC を使うので、エミュレーターではなく実機が必要です
- [Supabase](https://supabase.com) のプロジェクト（無料プランで動きます）

## セットアップ

### 1. データベースを用意する

Supabase の **SQL Editor** で [`supabase/schema.sql`](supabase/schema.sql) の中身を貼り付けて実行します。`help_profiles` テーブルと、サンプルの 1 人分のデータができます。

### 2. Android Studio でプロジェクトを開く

1. このリポジトリをクローン（またはZIPでダウンロードして展開）します。
2. Android Studio の **File → Open** でフォルダを開きます。
3. 初回は Gradle の同期が始まります。**Android SDK 36** が入っていない場合はインストールを求められるので、案内に従ってください。

開くとプロジェクト直下に `local.properties` が自動で作られます（中に SDK の場所 `sdk.dir=...` が書かれます）。

### 3. Supabase の接続情報を書く

`local.properties` に次の 2 行を**追記**します（`sdk.dir` の行は消さないでください）。このファイルは Git に含まれません。

```properties
supabase.url=https://xxxx.supabase.co
supabase.anonKey=eyJ...
```

- `supabase.url`：Supabase の **Project Settings → Data API** にある Project URL
- `supabase.anonKey`：**Project Settings → API Keys** の「Legacy API keys」にある **anon public** キー（`eyJ` で始まる長い文字列）

書き換えたら、Android Studio の **File → Sync Project with Gradle Files** を実行します。

### 4. スマホで起動する

1. スマホの **開発者向けオプション → USB デバッグ** をオンにして、パソコンにつなぎます（スマホに出る確認は「許可」）。
2. Android Studio 上部の端末一覧で自分のスマホを選び、**▶（Run）** を押します。

起動したら「近くのヘルプ」で **スキャンを開始** を押し、Bluetooth・位置情報・通知を許可してください。

#### コマンドラインから入れる場合

`JAVA_HOME` に Android Studio 同梱の JDK を指定してから実行します（未設定だと「Please set the JAVA_HOME variable」と出て止まります）。

```bash
# macOS
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# Windows（Git Bash）
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"

./gradlew :app:installRelease   # 軽量化した版（約 1.2MB）
./gradlew :app:installDebug     # 開発用の版
```

Windows の PowerShell では `.\gradlew.bat :app:installRelease` です。

## データの登録

`help_profiles` の主な列：

| 列 | 内容 |
| --- | --- |
| `display_name` | 名前 |
| `felica_idm` | カード ID（アプリのカード画面で長押しするとコピーできます） |
| `ble_id` | BLE タグのアドレス（`AA:BB:CC:DD:EE:FF` 形式。バックグラウンド見守りはこの形式のみ対応） |
| `help_request` | お手伝いしてほしいこと |
| `emergency_contact_name` / `emergency_contact_phone` | 緊急連絡先 |

## 注意

`schema.sql` の読み取りポリシーは試作用で、公開キーを持つ人なら全員分を読めます。実際の方の情報を登録する前に、読み取りの範囲を絞ってください。
