package client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.websocket.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

@ClientEndpoint
public class TextEditorClient {
    private static Session session;
    private static JTextArea textArea;
    private static String mySessionId;
    private static String myUsername;
    private static DocumentListener documentListener;

    // 사용자별 커서 표시용 맵
    private static final Map<String, JLabel> userCursors = new HashMap<>();
    private static final Map<String, Integer> userCaretPos = new HashMap<>();
    private static final Map<String, String> userNames = new HashMap<>();

    private static JLayeredPane layeredPane;

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
                    URI uri = new URI("ws://localhost:8080/ws/text-editor?userId=" + userId);
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

        // 커서 위치 전송 주기 설정
        new Timer(300, e -> sendCaretPosition()).start();
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "text");

            if (type.equals("cursor")) {
                String senderId = json.getString("senderId");
                if (senderId.equals(mySessionId)) return;

                int caret = json.getInt("caret");
                String name = json.getString("username");

                userCaretPos.put(senderId, caret);
                userNames.put(senderId, name);

                SwingUtilities.invokeLater(() -> updateOtherCursors());

            } else if (type.equals("text")) {
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

    private static void sendCaretPosition() {
        if (session != null && session.isOpen()) {
            try {
                int caret = textArea.getCaretPosition();
                JSONObject json = new JSONObject();
                json.put("type", "cursor");
                json.put("senderId", mySessionId);
                json.put("username", myUsername);
                json.put("caret", caret);
                session.getBasicRemote().sendText(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Shared Text Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(0, 0, 580, 360);
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

        documentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { sendTextToServer(); }
            public void removeUpdate(DocumentEvent e) { sendTextToServer(); }
            public void changedUpdate(DocumentEvent e) {}
        };
        textArea.getDocument().addDocumentListener(documentListener);

        frame.add(layeredPane);
        frame.setVisible(true);
    }

    private static void updateOtherCursors() {
        // 기존 커서 제거
        for (JLabel label : userCursors.values()) layeredPane.remove(label);
        userCursors.clear();

        for (Map.Entry<String, Integer> entry : userCaretPos.entrySet()) {
            String senderId = entry.getKey();
            int caret = entry.getValue();
            String name = userNames.get(senderId);

            try {
                Rectangle rect = textArea.modelToView(caret);
                if (rect == null) continue;

                JLabel label = new JLabel("| " + name);
                label.setForeground(Color.MAGENTA);
                label.setBounds(rect.x + 5, rect.y, 100, rect.height);

                layeredPane.add(label, JLayeredPane.PALETTE_LAYER);
                userCursors.put(senderId, label);
            } catch (Exception ignored) {}
        }
        layeredPane.repaint();
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
