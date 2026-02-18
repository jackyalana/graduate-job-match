package com.lanrecruitment.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    @Bean
    public JavaMailSender javaMailSender(EmailProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setUsername(props.getUsername());
        sender.setPassword(props.getPassword());

        boolean ssl = props.getSsl() != null ? props.getSsl() : true;
        boolean starttls = props.getStarttls() != null ? props.getStarttls() : false;
        int port = props.getPort() != null ? props.getPort() : (ssl ? 465 : 25);
        sender.setPort(port);

        Properties javaMailProps = new Properties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.connectiontimeout", "8000");
        javaMailProps.put("mail.smtp.timeout", "8000");
        javaMailProps.put("mail.smtp.writetimeout", "8000");
        javaMailProps.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        javaMailProps.put("mail.smtp.starttls.required", String.valueOf(starttls));
        sender.setJavaMailProperties(javaMailProps);

        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return sender;
    }
}
