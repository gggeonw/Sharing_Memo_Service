package client;

import javax.swing.*;
import javax.websocket.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

@ClientEndpoint
public class TextEditorClient {

    private static Session session;

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
        System.out.println("Server broadcast: " + message);
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Session opened: " + session.getId());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Session closed: " + session.getId() + " Reason: " + closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error occurred: " + throwable.getMessage());
    }

    // Swing GUI 생성 메소드
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Shared Text Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        frame.add(scrollPane, BorderLayout.CENTER);

        // 창을 닫을 때 WebSocket 세션도 같이 끊는다
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
}
