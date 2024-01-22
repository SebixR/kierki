package com.example.kierki;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The Server class is responsible for accepting clients, assigning them unique IDs, and creating
 * new instances of the ClientHandler class, which then handler all client-server communication.
 */
public class Server {
    private final ServerSocket serverSocket;
    private int clientIds = 1;

    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    /**
     * The server is constantly listening for new clients. When a client appears, it creates
     * a new instance of the ClientHandler class, responsible for all client-server communication.
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
     * Shuts down the server.
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
