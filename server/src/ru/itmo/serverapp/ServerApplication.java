package ru.itmo.serverapp;

public class ServerApplication {
    public static void main(String[] args) {
        //new Server(8900).start();
        Server.getServer(8910).start();
    }
}


