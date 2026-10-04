# Re:Me
プロファイル分けできる、シンプルなメッセージアプリ。

## コンセプト
```
LINEは「リア友」、Discordは「ネッ友」、Slackは「お仕事」。
そのアプリ、1つにしませんか？

用途ごとにプロファイル分けして、1つのアプリで。
「Re:Me」
```

## 機能
* アカウントの新規登録, ログイン, ログアウト
* メッセージ（やり取り）
* グループチャット（グループ作成、メッセージ、未読数）

## 使用した技術・フレームワーク
* Spring Boot 3
* PostgresSQL(Docker-Compose)
* Swift/SwiftUI
* Java 21 LTS

## ファイル構成
* `ios/`: iOSアプリ
* `android/`: Androidアプリ（Android Studioで開くプロジェクト）
* `server/`: バックエンド
* `sql`: Docker-Composeで動く、PostgresSQL

## Androidアプリ
`android/`をAndroid Studioで開き、Gradle同期後に実行してください。Jetpack ComposeとMaterial 3を使用しています。初期接続先はiOSアプリと同じサーバーURLで、ログイン画面の設定アイコンまたはアカウント画面から変更できます。Android Emulatorからローカルサーバーへ接続する場合は、接続先を`http://10.0.2.2:<ポート>`に設定してください。

プロフィール別の会話、未分類の会話、グループ一覧、個別/グループメッセージに対応しています。

## 頑張ったポイント
* APNsを使用しているため、通知が来ます‼️
* TestFlightで簡単にデバッグできるようにしました
* L◯NEの痒いところに手が届かない点を拾いに拾い上げ、使いやすくしました

気が向いたらREADMEももう少し充実させれたらと思います。
TestFlight参加したい人いたら声かけてね by 木村
