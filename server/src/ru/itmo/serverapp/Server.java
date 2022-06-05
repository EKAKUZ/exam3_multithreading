package ru.itmo.serverapp;

import ru.itmo.lib.Connection;
import ru.itmo.lib.SimpleMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private int port;
    private ConcurrentHashMap<Connection, Integer> connections;
    private ArrayBlockingQueue<String> messages;
    private static Server server;
    private int connectionLastNumber = 0;

    private Server(int port) {
        this.port = port;
        connections = new ConcurrentHashMap<>();
        messages = new ArrayBlockingQueue<>(10, true);
    }

    public static Server getServer(int port) {
        if (server == null) {
            server = new Server(port);
        }
        return server;
    }

    public void start() {

        serverSendMessage();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен");

            while (true) {
                Socket newClient = serverSocket.accept();
                Connection connection = new Connection(newClient);
                connectionLastNumber ++;
                connections.put(connection, connectionLastNumber);
                serverReadMessage(connection);
            }

        } catch (IOException e) {
            System.out.println("Ошибка сервера");
        }
    }

    private void serverReadMessage(Connection connection)  {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimpleMessage message = connection.readMessage();
                    String messageString;

                    messageString = formatter.format(message.getDateTime()) + ": " + message.getSender() + "--> " + message.getText();

                    System.out.println(messageString);

                    messages.put(connections.get(connection) + ";" + messageString);

                    if ("exit".equalsIgnoreCase(message.getText())) Thread.currentThread().interrupt();

                } catch (InterruptedException e) {
                    System.out.println("Ошибка записи сообщения в очередь сообщений"); //мб не совсем корректно - прерывание в момент ожидания сообщения для записи в очередь
                } catch (IOException e) {
                    System.out.println("Ошибка получения сообщения / соединение разорвано");
                    Thread.currentThread().interrupt(); //так как изменяю свойство внутри потока, исключения InterruptedException не будет
                    connections.remove(connection);
                    try {
                        connection.close();
                    } catch (Exception ex) {
                        throw new RuntimeException("Ошибка закрытия соединения"); // здесь остановится поток
                        // считаю, что остановка потока только в том случае, когда клиент не корректно отключился
                        // и поток в любом случае нужно остановить
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Ошибка чтения сообщения");
                }
            }
        }).start();
    }

    private void serverSendMessage()  {

        new Thread(() -> {
            while (true) {
                try {
                    String message = messages.take();

                    String[] arr = message.split(";");

                    for (Map.Entry<Connection, Integer> pair : connections.entrySet()) {

                        if ("exit".equalsIgnoreCase(arr[1].substring(arr[1].length() - 4))) {
                            if (pair.getValue().toString().equalsIgnoreCase(arr[0])) {
                                pair.getKey().sendMessage(SimpleMessage.getMessage("от сервера", arr[1]));
                                connections.remove(pair.getKey());
                            }
                        } else {
                            if (!pair.getValue().toString().equalsIgnoreCase(arr[0]))
                                pair.getKey().sendMessage(SimpleMessage.getMessage("от сервера", arr[1]));
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Ошибка получения сообщения из очереди сообщений"); //мб не совсем корректно - прерывание в момент ожидания сообщения
                }catch (IOException e) {
                    System.out.println("Ошибка отправки сообщения");
                }

            }
        }).start();

    }

}
