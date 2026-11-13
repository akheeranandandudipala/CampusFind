package com.campusfind.app.utils;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Handles email notifications for CampusFind.
 * Supports manual sending via Android Email Intent (recommended & secure)
 * as well as background SMTP.
 */
public class EmailSender {

    private static final String TAG = "EmailSender";

    /**
     * Creates an Android ACTION_SENDTO intent pre-populated with recipient, subject, and body.
     * Encodes subject and body into the mailto: query string AND sets Intent extras to ensure
     * full compatibility with all email clients (Gmail, Outlook, Yahoo, Samsung Mail).
     */
    public static Intent createEmailIntent(String toEmail, String subject, String body) {
        String cleanEmail = toEmail != null ? toEmail.trim() : "";
        String cleanSubject = subject != null ? subject : "";
        String cleanBody = body != null ? body : "";

        // Standard mailto URI with query params
        String mailtoUri = "mailto:" + Uri.encode(cleanEmail)
                + "?subject=" + Uri.encode(cleanSubject)
                + "&body=" + Uri.encode(cleanBody);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse(mailtoUri));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{cleanEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, cleanSubject);
        intent.putExtra(Intent.EXTRA_TEXT, cleanBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    // ── Fill in your dedicated app Gmail address and App Password ─────────
    private static final String SENDER_EMAIL    = "campusfind.noreply@gmail.com";
    private static final String SENDER_PASSWORD = "your_16char_app_password_here";
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sends an email in a background thread. Safe to call from the UI thread.
     * Failures are logged but never crash the app.
     *
     * @param toEmail  Recipient email address
     * @param subject  Email subject
     * @param body     Plain-text email body
     */
    public static void sendAsync(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            Log.w(TAG, "sendAsync: recipient email is empty, skipping.");
            return;
        }
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host",                   "smtp.gmail.com");
                props.put("mail.smtp.port",                   "465");
                props.put("mail.smtp.auth",                   "true");
                props.put("mail.smtp.socketFactory.port",     "465");
                props.put("mail.smtp.socketFactory.class",    "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "CampusFind App"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(toEmail.trim()));
                message.setSubject(subject);
                message.setText(body);
                Transport.send(message);
                Log.d(TAG, "Email sent successfully to " + toEmail);

            } catch (Exception e) {
                Log.e(TAG, "Failed to send email to " + toEmail + " : " + e.getMessage());
            }
        }).start();
    }
}
