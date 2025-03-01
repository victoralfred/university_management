package com.vickezi.messaging.queue;

import com.vickezi.globals.model.RegistrationMessage;
import com.vickezi.messaging.queue.exception.EmailSendingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
@PropertySource({"classpath:env.properties"})
public class EmailServiceImpl {
    @Value("${email.password}")// SMTP username
    private String password; // SMTP password
    private final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    public void sendEmail(RegistrationMessage message){
        processMessage(message.messageId(), ()->sender(message));

    }
    /**
     * Generic method to process messages.
     *
     * @param <T>              The type of the message.
     * @param message          The message to process.
     * @param messageProcessor The processing function.
     */
    private <T> void processMessage(T message, Runnable messageProcessor) {
        try{
            messageProcessor.run();
        }catch (Exception e){
            logger.error("Error processing message {}: {}", message, e.getMessage(), e);
        }
    }
    private void sender(RegistrationMessage registrationMessage) {
        // Email configuration
        String to = registrationMessage.email(); // Recipient's email address
        String from = "d.i.dabbas@gmail.com"; // Sender's email address
        String host = "smtp.gmail.com"; // SMTP server host
        String username = "d.i.dabbas@gmail.com";

        // Set properties for the SMTP server
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587"); // Use 465 for SSL
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Enable TLS

        // Dynamic properties to inject into the template
        Map<String, String> templateProperties = new HashMap<>();
        final String URL = String.format("http://localhost:9000/api/v1/registration?token=%s&messageId=%s",
                registrationMessage.token(),registrationMessage.messageId());
        templateProperties.put("emailLink", URL);

        // Read the HTML template file
        String templatePath = "email-message-template.html"; // File in src/main/resources
        InputStream inputStream = EmailServiceImpl.class.getClassLoader().getResourceAsStream(templatePath);
        try{
            // Create a session with authentication
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            if (inputStream == null) {
                throw new FileNotFoundException("Template file not found: " + templatePath);
            }
            String emailContent = loadTemplate(inputStream, templateProperties);
            // Create a MimeMessage object
            MimeMessage message = new MimeMessage(session);

            // Set the sender and recipient
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set the email subject and body
            message.setSubject("Test Email with Template");
            message.setContent(emailContent, "text/html"); // Set content as HTML

            // Send the email
            Transport.send(message);
        } catch (IOException | MessagingException e) {
            throw new EmailSendingException(e);
        }
    }

    /**
     * Loads the HTML template and replaces placeholders with dynamic properties.
     *
     * @param inputStream Template input stream.
     * @param properties   A map of placeholder keys and their replacement values.
     * @return The processed HTML content as a String.
     */
    private static String loadTemplate(InputStream inputStream, Map<String, String> properties) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        }
        // Replace placeholders with actual values
        String templateContent = contentBuilder.toString();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            templateContent = templateContent.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return templateContent;
    }

}
