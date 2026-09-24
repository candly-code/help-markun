# ヘルプマーくん（Help Markun）

障がいのある方と、手助けしたい人をつなぐ Android アプリです。

- **近くのヘルプ**：BLE タグを探し、Supabase に登録された方が近くにいると赤くお知らせします。アプリを閉じていてもバックグラウンドで見守り、見つけたら通知とアイコンのバッジで知らせます。
- **カードで確認**：FeliCa・NFC カードをスマホにタッチすると、登録された方の情報（お手伝いしてほしいこと・医療情報・緊急連絡先）を表示します。未登録のカードでも、カードの種類や ID を表示します。

Jetpack Compose と Material 3 Expressive で作っています（ライト／ダーク、文字サイズ、動きを控えめにする設定などに対応）。

## 必要なもの

- Android Studio（JDK 同梱のもの）
- Android 7.0（API 24）以上の端末。BLE と NFC を使う機能は実機が必要です
- Supabase のプロジェクト

## セットアップ

1. Supabase の SQL Editor で [`supabase/schema.sql`](supabase/schema.sql) を実行し、`help_profiles` テーブルを作ります。
2. プロジェクト直下の `local.properties` に接続情報を書きます（このファイルは Git に含めません）。

   ```properties
   supabase.url=https://xxxx.supabase.co
   supabase.anonKey=eyJ...
   ```

3. 端末をつないでインストールします。

   ```bash
   ./gradlew :app:installRelease
   ```

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
