package com.example.clockedin.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
    private static final String TAG = "EmailSender";
    
    private static final String EMAIL = "lumapacryzeil@gmail.com";
    private static final String PASSWORD = "ecexuxxwfmisrikm"; 

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendEmail(String recipientEmail, String subject, String message, EmailCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    Properties props = new Properties();
                    // Use Gmail SMTP server since it's a Google Workspace account
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.socketFactory.port", "465");
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.port", "465");
                    props.put("mail.smtp.ssl.enable", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.debug", "true");

                    Log.d(TAG, "Creating mail session...");
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(EMAIL, PASSWORD);
                        }
                    });

                    Log.d(TAG, "Creating message...");
                    Message mimeMessage = new MimeMessage(session);
                    mimeMessage.setFrom(new InternetAddress(EMAIL));
                    mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    mimeMessage.setSubject(subject);
                    mimeMessage.setText(message);

                    Log.d(TAG, "Sending message...");
                    Transport.send(mimeMessage);
                    Log.d(TAG, "Message sent successfully!");
                    return true;
                } catch (MessagingException e) {
                    Log.e(TAG, "Error sending email", e);
                    errorMessage = "Error: " + e.getMessage();
                    return false;
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                    errorMessage = "Unexpected error: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Log.d(TAG, "Email sent successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to send email: " + errorMessage);
                    callback.onFailure(errorMessage);
                }
            }
        }.execute();
    }
} 