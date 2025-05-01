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
    private static DocumentListener documentListener;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextEditorClient::showLogInGUI);
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("[Client] Session opened: " + session.getId());
        mySessionId = session.getId(); // 내 세션 ID 저장
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("[Client] Session closed: " + session.getId() + " Reason: " + closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[Client] Error occurred: " + throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String message) {
        //System.out.println("[Client] Received message: " + message);

        try {
            JSONObject json = new JSONObject(message);

            String type = json.optString("type", "text"); // 기본값 text

            if (type.equals("system")) {
                // 시스템 메시지 처리
                String event = json.getString("event");
                String clientId = json.getString("clientId");

                if (event.equals("connected")) {
                    System.out.println("[Client] Client connected: " + clientId);
                } else if (event.equals("disconnected")) {
                    System.out.println("[Client] Client disconnected: " + clientId);
                }
            } else {
                // 텍스트 메시지 처리
                String senderId = json.getString("senderId");
                String text = json.getString("text");

                // 내가 보낸 메시지는 무시
                if (senderId.equals(mySessionId)) {
                    System.out.println("[Client] Ignored my own message.");
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    if (textArea != null) {
                        try {
                            // 👇 문서 리스너 일시 제거
                            textArea.getDocument().removeDocumentListener(documentListener);
                            textArea.setText(text);
                        } finally {
                            // 👇 문서 리스너 다시 추가
                            textArea.getDocument().addDocumentListener(documentListener);
                        }
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showLogInGUI() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 150);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        JTextField userIdField = new JTextField();
        JButton connectButton = new JButton("접속");

        panel.add(new JLabel("사용자 ID: "), BorderLayout.WEST);
        panel.add(userIdField, BorderLayout.CENTER);
        loginFrame.add(panel, BorderLayout.NORTH);
        loginFrame.add(connectButton, BorderLayout.SOUTH);

        Runnable connectToServer = () -> {
            String userId = userIdField.getText().trim();
            if (userId.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "ID를 입력하세요.");
                return;
            }

            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                URI uri = new URI("ws://192.168.219.71:8080/ws/text-editor?userId=" + userId); // 사용자 ID 쿼리로 전달
                session = container.connectToServer(TextEditorClient.class, uri);
                System.out.println("[Client] Connected to server!");

                loginFrame.dispose(); // 로그인 창 닫고
                showTextEditorGUI(); // 에디터 창 열기
            } catch (Exception e) {
                JOptionPane.showMessageDialog(loginFrame, "서버에 접속할 수 없습니다.\n" + e.getMessage());
                System.err.println("[Client] 서버 접속 실패: " + e.getMessage());
            }
        };

        connectButton.addActionListener(e -> connectToServer.run());
        userIdField.addActionListener(e -> connectToServer.run());

        loginFrame.setLocationRelativeTo(null); // 중앙 정렬
        loginFrame.setVisible(true);
    }


    private static void showTextEditorGUI() {
        JFrame frame = new JFrame("Shared Text Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        frame.add(scrollPane, BorderLayout.CENTER);

        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                sendTextToServer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sendTextToServer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // 보통 스타일 변경
            }
        };
        textArea.getDocument().addDocumentListener(documentListener);


        // 창 닫을 때 세션 끊기
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                        System.out.println("[Client] Disconnected from server (GUI closed).");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

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
                System.out.println("[Client] Sent updated text to server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}