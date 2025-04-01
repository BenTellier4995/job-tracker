package com.example.jobmanager.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ListMessagesResponse;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class GmailService {

    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    public static final Path CREDENTIALS_FILE_PATH = Paths.get("src/main/resources/credentials.json");
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly");
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static Gmail getGmailService() throws IOException, GeneralSecurityException {
        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                getCredentials(GoogleNetHttpTransport.newTrustedTransport()))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Files.newInputStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8081).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static final String[] KEYWORDS = {
            "thank you for applying", "Thanks for applying", "thank you for applying", "We Got It: Thanks for applying",
            "we received your application", "we've received your application", "your application has been received",
            "should your qualifications match", "our hiring team will review",
            "we appreciate your interest in joining our team",

    };

    public List<Message> fetchEmails() throws IOException, GeneralSecurityException {
        Gmail service = getGmailService();
        String user = "me";
        List<Message> fullMessages = new ArrayList<>();

        for (String keyword : KEYWORDS) {
            String query = "subject:(" + keyword + ")";
            System.out.println("Running query for subject: " + query);

            ListMessagesResponse messagesResponse = service.users().messages().list(user)
                    .setQ(query)
                    .execute();

            String bodyQuery = keyword;
            System.out.println("Running query for body: " + bodyQuery);


            ListMessagesResponse bodyMessagesResponse = service.users().messages().list(user)
                    .setQ(bodyQuery)
                    .execute();

            List<Message> messages = messagesResponse.getMessages();
            List<Message> bodyMessages = bodyMessagesResponse.getMessages();


            if (messages != null) {
                for (Message message : messages) {
                    Message fullMessage = service.users().messages().get(user, message.getId()).execute();

                    if (!fullMessages.contains(fullMessage)) {
                        fullMessages.add(fullMessage);
                    }
                }
            }


            if (bodyMessages != null) {
                for (Message message : bodyMessages) {
                    Message fullMessage = service.users().messages().get(user, message.getId()).execute();
                    if (!fullMessages.contains(fullMessage)) {
                        fullMessages.add(fullMessage);
                    }
                }
            }
        }

        return fullMessages;
    }

    private boolean containsKeywords(String text) {
        if (text == null) return false;

        for (String keyword : KEYWORDS) {
            if (text.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
