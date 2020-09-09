package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String userName = null;
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message receiveMessage = connection.receive();
                if (receiveMessage.getType() == MessageType.USER_NAME) {
                    userName = receiveMessage.getData();
                    if (!userName.isEmpty() && !connectionMap.containsKey(userName)) {
                        break;
                    }
                } else continue;
            }
            connectionMap.put(userName, connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return userName;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (!(name.equals(userName))) connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            Message message;
            while(true) {
                message = connection.receive();
                if ((message != null && message.getType() == MessageType.TEXT)) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
                else {
                    ConsoleHelper.writeMessage("Ошибка!");
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = ConsoleHelper.readInt();
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("Сервер запущен на порту " + server.getLocalPort());
            while (true) {
                Handler handler = new Handler(server.accept());
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера.");
            try {
                server.close();
            } catch (IOException ioException) {
                System.out.println("Ошибка при закрытии серверного сокета.");
            }
        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> element : connectionMap.entrySet()) {
            try {
                element.getValue().send(message);
            } catch (IOException e) {
                System.out.println("Ошибка при отправке сообщения");
            }
        }
    }
}
