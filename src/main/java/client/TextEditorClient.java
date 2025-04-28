package client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.websocket.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

@ClientEndpoint
public class TextEditorClient {

    private static Session session;
    private static JTextArea textArea; // 텍스트 에디터 컴포넌트 전역 변수
    private static String mySessionId;

    public static void main(String[] args) {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        String serverUri = "ws://192.168.106.117:8080/ws/text-editor"; // 서버 주소

        try {
            session = container.connectToServer(TextEditorClient.class, URI.create(serverUri));
            System.out.println("Connected to server!");

            // 서버 연결 성공 시 Swing GUI 띄우기
            SwingUtilities.invokeLater(TextEditorClient::createAndShowGUI);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("[Client] Server says: " + message);

        try {
            // JSON 파싱
            org.json.JSONObject json = new org.json.JSONObject(message);
            String senderId = json.getString("senderId");
            String text = json.getString("text");

            // 내 자신이 보낸 거면 무시
            if (senderId.equals(mySessionId)) {
                System.out.println("[Client] Ignored my own message.");
                return;
            }

            // 다른 클라이언트가 보낸 거면 화면 업데이트
            SwingUtilities.invokeLater(() -> {
                if (textArea != null) {
                    textArea.setText(text);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        System.out.println("[Client] Session opened: " + session.getId());
        mySessionId = session.getId(); // 내 세션ID 저장
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("[Client] Session closed: " + session.getId() + " Reason: " + closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[Client] Error occurred: " + throwable.getMessage());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Shared Text Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        frame.add(scrollPane, BorderLayout.CENTER);

        // 텍스트 변경 감지
        textArea.getDocument().addDocumentListener(new DocumentListener() {
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
                // 보통 스타일 변경일 때 호출되는데 JTextArea는 거의 안 씀
            }
        });

        // 창 닫을 때 세션 끊기
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (session != null && session.isOpen()) {
                    try {
                        session.close();
                        System.out.println("Disconnected from server (GUI closed).");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        frame.setVisible(true);
    }

    // 서버로 텍스트 보내는 메소드
    private static void sendTextToServer() {
        if (session != null && session.isOpen()) {
            try {
                String currentText = textArea.getText();
                String message = "{\"senderId\":\"" + mySessionId + "\",\"text\":\"" + currentText + "\"}";
                session.getBasicRemote().sendText(message);
                System.out.println("[Client] Sent updated text to server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
