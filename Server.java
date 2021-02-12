package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = ConsoleHelper.readInt();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            ConsoleHelper.writeMessage("Cервер запущен");

            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            serverSocket.close();
            e.printStackTrace();
        }
    }

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Map.Entry entry: connectionMap.entrySet()) {
                ((Connection) entry.getValue()).send(message);
            }

        } catch (IOException e) {
            System.out.println("Невозможно отправить сообщение.");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String userName = "";
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST, "Введите имя."));
                Message message = connection.receive();
                if (message.getType() == MessageType.USER_NAME && message.getData() != null &&
                        !message.getData().equals("")) {
                    if (!connectionMap.containsKey(message.getData())) {
                        connectionMap.put(message.getData(), connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED, "Имя принято."));
                        userName = message.getData();
                        break;
                    }
                }
            }
            return userName;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry entry: connectionMap.entrySet()) {
                if (!entry.getKey().equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, (String) entry.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else { ConsoleHelper.writeMessage("Ошибка."); }
            }
        }

        public void run() {
            String userName = "";
            try {
                ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: " +
                    socket.getRemoteSocketAddress());
                Connection connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
                connection.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Произошла ошибка при обмене данными с удаленным адресом.");
            }
            if (userName != null && !userName.equals("")) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");

        }
    }
}
