package client;

import javax.websocket.*;
import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class TextEditorClient {

    private static Session session;

    public static void main(String[] args) {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        String serverUri = "ws://localhost:8080/ws/text-editor"; // 서버 주소
        try {
            session = container.connectToServer(TextEditorClient.class, URI.create(serverUri));
            System.out.println("Connected to server!");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Press Enter to disconnect...");
            scanner.nextLine(); // 엔터 입력 대기

            session.close();
            System.out.println("Disconnected from server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Server says: " + message);
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
}

