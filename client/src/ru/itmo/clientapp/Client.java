package ru.itmo.clientapp;

import ru.itmo.lib.Connection;
import ru.itmo.lib.SimpleMessage;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


public class Client {
    private String ip;
    private int port;
    private Scanner scanner;
    private String userName;
    private Connection connection;


    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Введите имя");
        userName = scanner.nextLine();
        try {
            connection = new Connection(new Socket(ip, port));
            sendMessage();
            printMessage();
        } catch (IOException e) {
            System.out.println("Ошибка установки соединения");
        }
    }

    private void sendMessage() {
        System.out.println("Соединение установлено.Введите exit для выхода");
        Thread sendThread = new Thread(() -> {
            while (true) {

                String text = scanner.nextLine(); // если сервер отключился этот поток ждет пока введут сообщение, поэтому делаю его демоном

                try {
                    connection.sendMessage(SimpleMessage.getMessage(userName, text));

                } catch (IOException e) {
                    System.out.println("Ошибка отправки сообщения");
                }
            }
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void printMessage() {
        Thread readThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                SimpleMessage fromServer = null;
                try {
                    fromServer = connection.readMessage();
                    System.out.println("от сервера: " + fromServer.getText());


                    if ("exit".equalsIgnoreCase(fromServer.getText().substring(fromServer.getText().length() - 4))) {

                        Thread.currentThread().interrupt();

                        try {
                            connection.close();
                        } catch (Exception e) {
                            throw new RuntimeException("Ошибка закрытия соединения");
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Ошибка получения сообщения / соединение разорвано");
                    Thread.currentThread().interrupt();

                    try {
                        connection.close();
                    } catch (Exception ex) {
                        throw new RuntimeException("Ошибка закрытия соединения");
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Ошибка чтения сообщения");
                }

            }
        });
        readThread.start();

    }

}
