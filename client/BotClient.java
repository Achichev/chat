package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class BotClient extends Client {
    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, " +
                    "год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (!message.contains(":")) return;
            else {
                String[] reply = message.split(": ");
                if (reply.length == 2) {
                    SimpleDateFormat sdf = null;
                    switch (reply[1]) {
                        case "дата":
                            sdf = new SimpleDateFormat("d.MM.YYYY");
                            break;
                        case "день":
                            sdf = new SimpleDateFormat("d");
                            break;
                        case "месяц":
                            sdf = new SimpleDateFormat("MMMM");
                            break;
                        case "год":
                            sdf = new SimpleDateFormat("YYYY");
                            break;
                        case "время":
                            sdf = new SimpleDateFormat("H:mm:ss");
                            break;
                        case "час":
                            sdf = new SimpleDateFormat("H");
                            break;
                        case "минуты":
                            sdf = new SimpleDateFormat("m");
                            break;
                        case "секунды":
                            sdf = new SimpleDateFormat("s");
                            break;
                    }
                    if (sdf != null) sendTextMessage("Информация для " + reply[0] + ": " + sdf.format(Calendar.getInstance().getTime()));
                }

            }
        }
    }
}
