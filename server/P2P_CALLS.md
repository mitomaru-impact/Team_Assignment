# 1対1 P2P WebRTC通話

通話音声はWebRTCでiPhone同士が直接送受信します。Spring Bootはログインセッションを使って発着信を認証し、SDPとICE candidateを既存の`/ws/messages` WebSocket経由で中継するだけです。LiveKitサーバー、APIキー、TURNサーバーはこの構成では使いません。

## 通話の着信とCallKit

iOSはCallKitで発着信UIを表示し、PushKitのVoIP pushでアプリがバックグラウンドまたは終了中の着信を受け取ります。VoIPトークンはログイン中に`/api/devices/voip`へ登録され、発信後はサーバーがAPNsの`.voip` topicで着信側へ通知します。WebSocketとVoIP pushの両方で同じ着信が届く場合は、call IDで重複を除外します。

XcodeのSigning & CapabilitiesでPush NotificationsとBackground Modesを有効にし、アプリの署名にPush Notifications entitlementを含めてください。サーバー側APNs設定は下記の手順を参照してください。開発ビルドはAPNs sandbox、TestFlight/App Storeビルドはproduction endpointと対応する`aps-environment` entitlementを使います。VoIP pushを含む端末間テストには、実機2台とPushKit登録済みのビルドが必要です。

## ローカル開発

1. PostgreSQLとSpring Bootを通常どおり起動します。
2. 1本だけngrok HTTPトンネルを起動します。

   ```sh
   ngrok http 8080
   ```

3. 表示されたHTTPS URLをiOSの`APIClient.swift`の`baseURL`に設定し、アプリをビルドします。WebSocketはこの同じURLから`wss://.../ws/messages`へ接続します。
4. 2台の実機にアプリを入れてサインインし、両方を前面表示にします。片方からチャット画面の電話アイコンで発信してください。

ngrokが中継するのはAPIとWebSocketのシグナリングです。音声パケットはngrokを通らず、端末間で直接流れます。LiveKitのDockerコンテナやRTC用のngrokトンネルは起動しません。

## 接続性と制約

- iOSはGoogle公開STUNサーバーを使い、NAT越しの直接接続を試みます。TURNリレーを含まないため、対称NAT、UDP遮断、企業・公衆Wi-Fiなどでは接続できないことがあります。
- P2Pなので音声データは2台間で直接送受信されます。着信とSDP/ICEの交換は認証済みSpring Boot WebSocketを使います。
- PushKit/CallKitでバックグラウンド・ロック画面着信を扱います。APNs設定・署名entitlementが不足している場合はVoIP pushが届かず、WebSocketによる前景着信のみになります。
- 開発用ngrok URLが変わった場合、iOSの`baseURL`を更新して両端末のアプリを再ビルドしてください。
- 任意のネットワーク間での接続を保証するにはTURNリレーを追加する必要があります。TURNを置ける公開サーバーがない場合、純粋なローカル構成で接続成功率を保証することはできません。
