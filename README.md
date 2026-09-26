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

## 使用した技術・フレームワーク
* Spring Boot 3
* PostgresSQL(Docker-Compose)
* Swift/SwiftUI
* Java 21 LTS

## ファイル構成
* `ios/`: iOSアプリ
* `server/`: バックエンド
* `sql`: Docker-Composeで動く、PostgresSQL

## 頑張ったポイント
* APNsを使用しているため、通知が来ます‼️
* TestFlightで簡単にデバッグできるようにしました

気が向いたらREADMEももう少し充実させれたらと思います。
TestFlight参加したい人いたら声かけてね by 木村
