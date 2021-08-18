package com.sirionlabs.utils.EmailUtils;

import com.sirionlabs.helper.api.TestAPIBase;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.naming.*;

public class EmailUtils extends TestAPIBase {

    public static void SendEmail(String msgSubject, String msgRecipient, String msgTxt) throws NamingException, MessagingException {

        InitialContext ic = new InitialContext();
        String snName = "java:comp/env/mail/MyMailSession";
        Session session = (Session)ic.lookup(snName);

        Properties props = session.getProperties();
        props.put("mail.from", "user2@mailserver.com");

        Message msg = new MimeMessage(session);
        msg.setSubject(msgSubject);
        msg.setSentDate(new Date());
        msg.setFrom();
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(msgRecipient, false));
        msg.setText(msgTxt);
        Transport.send(msg);
    }

    public static void ReadEmail(String host, String user, String password,String foldername) throws NamingException, MessagingException {

        InitialContext ic = new InitialContext();
        String snName = "java:comp/env/mail/MyMailSession";
        Session session = (javax.mail.Session)ic.lookup(snName);

        /*Properties props = session.getProperties();
        props.put("mail.from", "user2@mailserver.com");*/

        Store store = session.getStore();
        store.connect(host, user, password);

        Folder folder = store.getFolder(foldername);

        Message[] messages = folder.getMessages();

    }

    public static String checkEmails(String host, String foldername, String user, String password) throws MessagingException, IOException {
        String Emailpassword = null;
        try {

                //create properties field
                Properties properties = new Properties();
                properties.put("mail.imap.host", host);
                properties.put("mail.imap.port", "587");
                properties.put("mail.imap.starttls.enable", "true");
                Session emailSession = Session.getDefaultInstance(properties);

                //create the POP3 store object and connect with the pop server
                Store store = emailSession.getStore("imaps");

                store.connect(host, user, password);

                //create the folder object and open it
                Folder emailFolder = store.getFolder(foldername);
                if (!emailFolder.isOpen()) {
                    emailFolder.open(Folder.READ_WRITE);
                }
                //emailFolder.open(Folder.READ_ONLY);

                // retrieve the messages from the folder in an array and print it
                Message[] messages = emailFolder.getMessages();
                System.out.println("messages.length---" + messages.length);

                for (int i = 0, n = messages.length; i < n; i++) {
                    Message message = messages[i];
                    System.out.println("---------------------------------");
                    System.out.println("Email Number " + (i + 1));
                    System.out.println("Subject: " + message.getSubject());
                    System.out.println("From: " + message.getFrom()[0]);
                    System.out.println("Text: " + message.getContent().toString());
                    final String input = message.getContent().toString();
                    final String regex = "<strong>([a-zA-Z]+([0-9]+[a-zA-Z]+)+)<\\/strong>";
                    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                    final Matcher matcher = pattern.matcher(input);

                    while (matcher.find()) {
                        Emailpassword = matcher.group(1);
                    }
                }

                //close the store and folder objects
                emailFolder.close(false);
                store.close();

            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                    e.printStackTrace();
            }
            return Emailpassword;
        }



    }
