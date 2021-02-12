package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера.");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите номер порта сервера.");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя.");
        return ConsoleHelper.readString();
    }
    
    protected boolean shouldSendTextFromConsole() { return true; }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            this.connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            e.printStackTrace();
            this.clientConnected = false;
        }
    }

    public void run() {
        SocketThread thread = getSocketThread();
        thread.setDaemon(true);
        thread.start();
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException exception) {
                ConsoleHelper.writeMessage("Сокетный поток прерван.");
            }
        }
        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено.\nДля выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if (text.equals("exit")) break;
            if (shouldSendTextFromConsole()) sendTextMessage(text);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }


    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник с именем " + userName + " присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник с именем " + userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    String userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) processIncomingMessage(message.getData());
                else if (message.getType() == MessageType.USER_ADDED) informAboutAddingNewUser(message.getData());
                else if (message.getType() == MessageType.USER_REMOVED) informAboutDeletingNewUser(message.getData());
                else throw new IOException("Unexpected MessageType");
            }
        }

        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try(Socket socket = new Socket(serverAddress, serverPort)) {
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException exception) {
                notifyConnectionStatusChanged(false);
            }
        }


    }
}
