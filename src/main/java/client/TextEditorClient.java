package client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.websocket.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

import org.json.JSONObject;

@ClientEndpoint
public class TextEditorClient {
    private static Session session;
    private static JTextArea textArea;
    private static String mySessionId;
    private static String myUsername;
    private static DocumentListener documentListener;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextEditorClient::showLoginGUI);
    }

    private static void showLoginGUI() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 120);
        loginFrame.setLayout(new FlowLayout());

        JTextField userIdField = new JTextField(20);
        JButton loginButton = new JButton("Connect");

        loginFrame.add(new JLabel("Enter your name:"));
        loginFrame.add(userIdField);
        loginFrame.add(loginButton);

        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);

        loginButton.addActionListener(e -> {
            String userId = userIdField.getText().trim();
            if (!userId.isEmpty()) {
                myUsername = userId;
                try {
                    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                    URI uri = new URI("ws://192.168.235.66:8080/ws/text-editor?userId=" + userId);
                    session = container.connectToServer(TextEditorClient.class, uri);
                    loginFrame.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @OnOpen
    public void onOpen(Session session) {
        mySessionId = session.getId();
        SwingUtilities.invokeLater(TextEditorClient::createAndShowGUI);
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "text");

            if (type.equals("text")) {
                String senderId = json.getString("senderId");
                if (senderId.equals(mySessionId)) return;

                String text = json.getString("text");

                SwingUtilities.invokeLater(() -> {
                    try {
                        int caret = textArea.getCaretPosition();
                        textArea.getDocument().removeDocumentListener(documentListener);
                        textArea.setText(text);
                        textArea.setCaretPosition(Math.min(caret, text.length()));
                    } finally {
                        textArea.getDocument().addDocumentListener(documentListener);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Shared Text Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        documentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { sendTextToServer(); }
            public void removeUpdate(DocumentEvent e) { sendTextToServer(); }
            public void changedUpdate(DocumentEvent e) {}
        };
        textArea.getDocument().addDocumentListener(documentListener);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static void sendTextToServer() {
        if (session != null && session.isOpen()) {
            try {
                String currentText = textArea.getText();
                JSONObject json = new JSONObject();
                json.put("type", "text");
                json.put("senderId", mySessionId);
                json.put("text", currentText);
                session.getBasicRemote().sendText(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}