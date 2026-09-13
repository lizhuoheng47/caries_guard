# 忘记密码与邮箱验证码配置

项目支持两种验证码投递模式：

- `LOCAL`：本地开发模式，验证码直接显示在重置密码弹窗中。
- `SMTP`：通过 QQ 邮箱、163 邮箱或企业邮箱的 SMTP 服务发送验证码。

验证码为 6 位数字，有效期默认 10 分钟，最多允许输错 5 次。数据库只保存带随机盐的验证码 HMAC，不保存验证码明文；新密码使用 BCrypt 保存。

## 1. 先用本地模式找回当前账号

本地配置默认已经启用 `LOCAL`。启动 Java 后端与前端，在登录页点击“忘记密码”，输入 `admin`，页面会显示本次验证码。

新密码要求至少 8 位，并且同时包含字母和数字。

首次使用邮箱发送功能前，在项目根目录执行下面的命令，让 Maven 下载邮件组件并重新编译后端：

```powershell
cd D:\caries_guard\backend-java
mvn -pl caries-boot -am -DskipTests package
```

## 2. 给账号绑定真实邮箱

`sys_user.email_enc` 保存 AES 加密后的真实邮箱。不要直接把明文邮箱写入数据库；应通过系统用户更新接口录入，现有业务代码会自动生成密文、哈希和脱敏值。

当前初始化的 `admin` 账号没有邮箱，因此在改为 SMTP 模式前必须先绑定一个可以收信的真实邮箱。

## 3. 获取 SMTP 授权码

以 QQ 邮箱为例：进入邮箱设置，开启 SMTP 服务并生成授权码。授权码用于程序发信，不是 QQ 邮箱登录密码。

163 邮箱或企业邮箱的操作类似，SMTP 主机和端口请使用对应邮件服务商提供的值。

## 4. 在启动 Java 后端的同一个 PowerShell 窗口设置变量

QQ 邮箱 465/SSL 示例：

```powershell
$env:CARIES_PASSWORD_RESET_ENABLED="true"
$env:CARIES_PASSWORD_RESET_DELIVERY_MODE="SMTP"
$env:CARIES_PASSWORD_RESET_DEV_CODE_ENABLED="false"
$env:CARIES_PASSWORD_RESET_MAIL_FROM="你的QQ邮箱@qq.com"

$env:SPRING_MAIL_HOST="smtp.qq.com"
$env:SPRING_MAIL_PORT="465"
$env:SPRING_MAIL_USERNAME="你的QQ邮箱@qq.com"
$env:SPRING_MAIL_PASSWORD="你的SMTP授权码"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE="true"
```

然后在这个窗口启动 Java 后端：

```powershell
cd D:\caries_guard\backend-java
$env:SPRING_PROFILES_ACTIVE="local"
mvn -pl caries-boot -am spring-boot:run
```

不要把授权码提交到 Git，也不要写入 `application-local.yml`。

## 5. 验证

在登录页点击“忘记密码”，输入已绑定邮箱的用户名并获取验证码。SMTP 模式下页面不会显示验证码，只显示脱敏邮箱；验证码会发送到真实邮箱。

如果没有收到邮件，依次检查：垃圾邮件目录、账号是否绑定真实邮箱、SMTP 服务是否开启、授权码是否正确，以及防火墙是否允许连接对应 SMTP 端口。

## 正式环境要求

- 保持 `CARIES_PASSWORD_RESET_DEV_CODE_ENABLED=false`。
- 使用环境变量或密钥管理服务保存 SMTP 授权码。
- 为验证码申请接口增加网关级 IP/账号限流。
- 邮件内容不得包含原密码或患者信息。
