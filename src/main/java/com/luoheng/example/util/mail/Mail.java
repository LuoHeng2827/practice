package com.luoheng.example.util.mail;

public class Mail {
    private String from;
    private String to;
    private String subject;
    private String content;
    private String passwords;
    public Mail(String from,String to,String subject,String content,String passwords){
        this.from=from;
        this.to=to;
        this.subject=subject;
        this.content=content;
        this.passwords=passwords;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPasswords() {
        return passwords;
    }

    public void setPasswords(String passwords) {
        this.passwords = passwords;
    }

    @Override
    public String toString() {
        return "Mail{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", passwords='" + passwords + '\'' +
                '}';
    }
}