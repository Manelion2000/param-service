package com.bakouan.app.service;


import com.bakouan.app.utils.BaUtils;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

/**
 * Service for sending emails.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BaMailService {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final SpringTemplateEngine templateEngine;

    @Value(value = "${spring.mail.username}")
    private String emailFrom;

    private static final String SENDER_NAME = "Categorie-App";

    /**
     * Envois de mail.
     *
     * @param to           destinataire
     * @param subject      objet
     * @param content      contenu
     * @param isMultipart
     * @param isHtml       format html ou brut
     * @param receiverName nom du destinataire
     * @param attachements pieces jointes.
     */
    @Async(value = "taskExecutor")
    public void sendEmail(final String to,
                          final String subject,
                          final String content,
                          final boolean isMultipart,
                          final boolean isHtml, final String receiverName,
                          final File... attachements) {
        log.debug(
                "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
                isMultipart,
                isHtml,
                to,
                subject,
                content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            if (BaUtils.isEmpty(receiverName)) {
                message.setTo(to);
            } else {
                message.setTo(new InternetAddress(to, receiverName));
            }
            message.setValidateAddresses(Boolean.TRUE);
            message.setFrom(new InternetAddress(emailFrom, SENDER_NAME));
            message.setReplyTo(new InternetAddress(emailFrom, "Ne repondez pas"));
            message.setSubject(subject);
            message.setText(content, isHtml);

            if (attachements != null) {
                for (File attachement : attachements) {
                    if (attachement.length() > 0) {
                        message.addAttachment(attachement.getName(), attachement);
                    }
                }
            }

            javaMailSender.send(mimeMessage);
            storeSentMessage(message);
            log.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    /**
     * Envoyer un mail à partir d'un fichier template.
     *
     * @param receiverName nom du destinataire
     * @param email        email
     * @param datas        : Données à injecter dans le template
     * @param templateName : le nom du fichier template
     * @param subject      : L'objet du mail
     * @param attachements pieces jointes
     */
    @Async(value = "taskExecutor")
    public void sendEmailFromTemplate(final String email,
                                      final HashMap<String, Object> datas,
                                      final String templateName,
                                      final String subject, final String receiverName,
                                      final File... attachements) {
        if (BaUtils.isEmpty(email)) {
            log.warn("Email doesn't exist for user.");
            return;
        }
        Locale locale = Locale.FRENCH;
        final Context context = new Context(locale);
        datas.keySet().forEach(k -> context.setVariable(k, datas.get(k)));
        String content = templateEngine.process(templateName, context);
        sendEmail(email, subject, content, true, true, receiverName, attachements);
    }

    /**
     * Envoyer un mail formaté.
     *
     * @param destEmail
     * @param destName
     * @param htmlMsg
     * @param objet
     */
    @Async(value = "taskExecutor")
    public void sendMessage(final String destEmail,
                            final String destName,
                            final String htmlMsg,
                            final String objet) {
        log.debug("Sending activation email to '{}'", destEmail);
        HashMap<String, Object> ds = new HashMap<>();
        ds.put("name", destName);
        ds.put("msg", htmlMsg);
        sendEmailFromTemplate(destEmail, ds, "mail/message.html", objet, destName);
    }

    private void storeSentMessage(final MimeMessageHelper message) throws MessagingException {
        // Saving mails
        Properties props = new Properties();
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.starttls.enable", true);
        Session session = Session.getInstance(props);

        Store store = session.getStore("imaps");
        store.connect(mailProperties.getHost(), Integer.parseInt("993"),
                mailProperties.getUsername(),
                mailProperties.getPassword());
        Folder folder = store.getFolder("Sent");
        if (!folder.exists()) {
            folder.create(Folder.HOLDS_MESSAGES);
        }
        folder.open(Folder.READ_WRITE);
        log.debug("Appending email to folder...");
        try {
            message.setPriority(Integer.parseInt("5"));
            final MimeMessage mimeMsg = message.getMimeMessage();
            folder.appendMessages(new MimeMessage[]{mimeMsg});
            mimeMsg.setFlag(Flags.Flag.SEEN, true);
        } catch (Exception ex) {
            log.warn("Failed to save email", ex);
        } finally {
            store.close();
            if (folder.isOpen()) {
                folder.close(false);
            }
        }
    }

}
