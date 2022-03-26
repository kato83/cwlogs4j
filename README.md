### フロントエンドのログを AWS の CloudWatch Logs にログを挟むための実装サンプル

起動方法、当リポジトリを clone した後、以下コマンドを叩く。

```shell
$ ./gradlew tomcatRun --no-daemon
```

http://localhost:8080 で tomcat サーバーが起動する。  
プロセスはそのまま、以下 URL を curl なりで叩く。

```shell
$ curl 'http://localhost:8080/api/v1/logger.jsp?level=INFO&messages=サンプル&messages=テスト送信'
```

※ tomcat プラグインの都合で IntelliJ IDEA の Gradle パネルからの起動だと正常に動作しなさそう。

`cloud-watch-logs.properties` の以下プロパティに適切な値を入れると動作します。

- `aws.cloudwatch.logs.region`
    - AWS の CloudWatch を使用しているリージョン
- `aws.cloudwatch.logs.group`
    - CloudWatch Logs のグループ名
- `aws.cloudwatch.logs.stream`
    - CloudWatch Logs のストリーム名
- `aws.cloudwatch.logs.accessKey`
    - AWS IAM アクセスキー
- `aws.cloudwatch.logs.secretKey`
    - AWS IAM シークレットキー

IAM アカウントについては `CloudWatchLogsFullAccess` の権限を付与しておけば、ひとまず動作していそう。