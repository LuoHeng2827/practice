package com.luoheng.example.util.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Date;
import java.util.Properties;

public class LMailer {
    /** 邮件发送协议*/
    private static final String PROTOCOL_SMTP = "smtp";

    /** SMTP邮件服务器*/
    private static final String SMTP_HOST = "smtp.163.com";

    // SMTP邮件服务器默认端口
    private static final String SMTP_PORT_25 = "25";
    private static final String SMTP_PORT_465 = "465";

    // 是否要求身份认证
    private static final String IS_AUTH = "true";
    // 是否启用调试模式（启用调试模式可打印客户端与服务器交互过程时一问一答的响应消息）
    private static final String ENABLED_DEBUG_MOD = "true";
    private static final Logger logger=LogManager.getLogger(LMailer.class);
    private static Properties sessionProps;

    public static Properties buildSessionProperties(){
        Properties sessionProps=new Properties();
        sessionProps.setProperty("mail.transport.protocol",PROTOCOL_SMTP);
        sessionProps.setProperty("mail.smtp.host",SMTP_HOST);
        sessionProps.setProperty("mail.smtp.port", SMTP_PORT_25);
        sessionProps.setProperty("mail.smtp.auth",IS_AUTH);
        sessionProps.setProperty("mail.debug", ENABLED_DEBUG_MOD);
        return sessionProps;
    }

    public  static Properties buildSSLSessionProperties(){
        Properties sessionProps=new Properties();
        sessionProps.put("mail.smtp.ssl.enable", true);
        sessionProps.setProperty("mail.transport.protocol",PROTOCOL_SMTP);
        sessionProps.setProperty("mail.smtp.host",SMTP_HOST);
        sessionProps.setProperty("mail.smtp.port", SMTP_PORT_465);
        sessionProps.setProperty("mail.smtp.auth",IS_AUTH);
        sessionProps.setProperty("mail.debug", ENABLED_DEBUG_MOD);
        return sessionProps;
    }

    public static void sendMail(Mail mail){
        sendEmail(mail,false);
    }

    public static void sendEmail(Mail mail,boolean isSSL){
        try{
            if(isSSL)
                sessionProps=buildSSLSessionProperties();
            else
                sessionProps=buildSessionProperties();
            Session session=Session.getDefaultInstance(sessionProps);
            MimeMessage mimeMessage=new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(mail.getFrom()));
            mimeMessage.setSubject(mail.getSubject());
            mimeMessage.setRecipient(Message.RecipientType.TO,new InternetAddress(mail.getTo()));
            mimeMessage.setSentDate(new Date());
            mimeMessage.setText(mail.getContent());
            mimeMessage.saveChanges();
            Transport transport=session.getTransport();
            transport.connect(mail.getFrom(),mail.getPasswords());
            transport.sendMessage(mimeMessage,mimeMessage.getAllRecipients());
            transport.close();
            logger.debug("the mail send success"+"from: "+mail.getFrom()+" "
                    +"to: "+mail.getTo());
        }catch(MessagingException e){
            logger.debug("failed to send mail!");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {

    }
}
