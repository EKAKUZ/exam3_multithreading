package ru.itmo.lib;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection implements AutoCloseable{
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        output = new ObjectOutputStream(this.socket.getOutputStream());
        input = new ObjectInputStream(this.socket.getInputStream());
    }

    public void sendMessage(SimpleMessage message) throws IOException {
        message.setDateTime();
        output.writeObject(message);
        output.flush();
    }

    public SimpleMessage readMessage() throws IOException, ClassNotFoundException {
        return (SimpleMessage) input.readObject();
    }


    @Override
    public void close() throws Exception {
        input.close();
        output.close();
        socket.close();
    }

    public void sendMessage(String сервер, String s) {
    }
}
