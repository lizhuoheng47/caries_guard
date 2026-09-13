package com.cariesguard.boot.mail;

import com.cariesguard.system.config.PasswordResetProperties;
import com.cariesguard.system.domain.service.PasswordResetCodeSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
        prefix = "caries.password-reset",
        name = "delivery-mode",
        havingValue = "SMTP")
public class SmtpPasswordResetCodeSender implements PasswordResetCodeSender {

    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

    public SmtpPasswordResetCodeSender(JavaMailSender mailSender,
                                       PasswordResetProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String recipient,
                     String username,
                     String verificationCode,
                     int expiresInMinutes) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalStateException("Password reset recipient email is empty");
        }
        if (!StringUtils.hasText(properties.getMailFrom())) {
            throw new IllegalStateException("Password reset sender email is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(recipient);
        message.setSubject("CariesGuard 密码重置验证码");
        message.setText("您好，" + username + "：\n\n"
                + "您的密码重置验证码是：" + verificationCode + "\n"
                + "验证码将在 " + expiresInMinutes + " 分钟后失效，请勿转发给他人。\n\n"
                + "如果不是您本人发起，请忽略此邮件。");
        mailSender.send(message);
    }
}
