package com.example.kierki;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {
    private final ServerSocket serverSocket;
    private int clientIds = 1;

    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    /**
     * Serwer cały czas czeka na klientów.
     * Gdy klient się pojawi, tworzy połączenie oraz rozpoczyna dla niego nowy wątek za pomocą obiektu
     * ClientHandler().
     */
    public void startServer()
    {
        System.out.println("Server is running...");
        try{
            while(!serverSocket.isClosed()){

                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket, clientIds);

                Thread thread = new Thread(clientHandler);
                thread.start();

                clientIds++;
            }
        } catch(IOException e){
            closeServerSocket();
        }
    }

    /**
     * Zamknięcie serwera.
     */
    public void closeServerSocket(){
        try {
            if (serverSocket != null){
                serverSocket.close();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Wywołanie wszystkich potrzebnych metod
     */
    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(6666);
            Server server = new Server(serverSocket);
            server.startServer();
            server.closeServerSocket();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }

    }
}
