package com.example.kierki;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class ClientHandler implements Runnable {
    private static final List<ClientHandler> clientHandlers = new ArrayList<>();
    private static final HashMap<Integer, Room> rooms = new HashMap<>();
    private static int serverRoomId = 1;
    private Socket socket;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private int clientId; //given by the server
    private boolean isInGame = false;
    private static final int CARDS_IN_DECK = 52;

    public ClientHandler(Socket socket, int clientId){

        try {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.clientId = clientId;
            clientHandlers.add(this);
        } catch(IOException e) {
            closeEverything(socket, in, out);
        }
    }

    /**
     * Handles all client-server communication
     */
    @Override
    public void run() {
        try {
            out.writeInt(clientId);
            out.writeObject(rooms);
        } catch (IOException e) {
            closeEverything(socket, in, out);
        }

        while (socket.isConnected()){
            try {
                Request request = (Request) in.readObject();

                if (request == Request.CREATE_ROOM)
                {
                    Room room = new Room(clientId, serverRoomId);
                    isInGame = true;
                    addRoom(serverRoomId, room);
                    serverRoomId++;

                    out.reset();
                    out.writeObject(Response.ROOM_CREATED);
                    out.writeObject(room);
                    out.flush();

                    broadcastRooms(room);

                }
                else if (request == Request.INVITE_PLAYER)
                {
                    int inviteId = in.readInt(); //someone else's ID
                    if (this.clientId != inviteId)
                    {
                        for (ClientHandler handler : clientHandlers) {
                            if (handler.clientId == inviteId && !handler.isInGame)
                            {
                                out.reset();
                                handler.out.writeObject(Response.INVITATION);
                                int foundRoomId = findRoom(this.clientId).getRoomId();
                                Invitation invitation = new Invitation(this.clientId, foundRoomId);
                                handler.out.writeObject(invitation);
                                handler.out.flush();
                                break;
                            }
                        }
                    }
                }
                else if (request == Request.JOIN_ROOM)
                {
                    int roomId = in.readInt();

                    if (!rooms.get(roomId).isFull()){
                        updateRoom(roomId, this.clientId);
                        Room room = rooms.get(roomId);
                        broadcastRooms(room);
                        isInGame = true;

                        out.reset();
                        out.writeObject(Response.JOINED_ROOM);
                        out.writeObject(room);
                        out.flush();
                    }
                }
                else if (request == Request.START_GAME) {
                    int roomId = (int) in.readObject();

                    dealCards(roomId);
                    System.out.println("Dealt cards to player: " + this.clientId + " in room " + roomId);

                    out.reset();
                    out.writeObject(Response.DEALT_CARDS);
                    for (int i = 0; i < CARDS_IN_DECK; i++)
                    {
                        if (rooms.get(roomId).getCards().get(i).getClientId() == this.clientId) {
                            out.writeObject(rooms.get(roomId).getCards().get(i));
                        }
                    }
                    out.flush();
                }

            } catch (Exception e) {
                closeEverything(socket, in, out);
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Broadcasts a newly created, or updated room to all clients
     * @param room the new room
     */
    public synchronized void broadcastRooms(Room room){
        for (ClientHandler handler : clientHandlers){
            try {
                if (handler != this)
                {
                    handler.out.reset();
                    handler.out.writeObject(Response.ROOMS_UPDATE);
                    handler.out.writeObject(room);
                    out.flush();
                }
            } catch (IOException e){
                closeEverything(socket, in, out);
            }
        }
    }

    /**
     * Updates all room parameters
     * @param roomId the room being updated
     * @param clientId the client being added to the room
     */
    public synchronized void updateRoom(int roomId, int clientId) {
        if (!rooms.get(roomId).isFull())
        {
            rooms.get(roomId).addPlayer(clientId);
            if (rooms.get(roomId).getPlayerAmount() >= 4) rooms.get(roomId).setFull();
        }
    }

    /**
     * Find room based on its host's ID
     * @param hostId the ID of the host whose room we're looking for
     * @return the found room or null
     */
    public Room findRoom(int hostId) {
        for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
            if (entry.getValue().getHostId() == hostId) return entry.getValue();
        }
        return null;
    }

    /**
     * Add a new room, created by a client
     * @param room the newly created room
     */
    public synchronized void addRoom(int roomId, Room room) {
        rooms.put(roomId, room);
    }

    /**
     * Chooses which cards to give to the client, while making sure there are no duplicates
     * @param roomId the player's room
     */
    public synchronized void dealCards(int roomId) {
        Random random = new Random();

        System.out.println("Dealt cards : ");

        for (int i = 0; i < CARDS_IN_DECK / 4; i++)
        {
            int cardNumber = random.nextInt(CARDS_IN_DECK); //from 0 to 51
            if (rooms.get(roomId).getCards().get(cardNumber).getClientId() != 0) {
                while (rooms.get(roomId).getCards().get(cardNumber).getClientId() != 0) {
                    cardNumber = random.nextInt(CARDS_IN_DECK);
                }
            }
            rooms.get(roomId).getCards().get(cardNumber).setClientId(this.clientId);
            System.out.println("Card: " + rooms.get(roomId).getCards().get(cardNumber).getValue() + " suit: " + rooms.get(roomId).getCards().get(cardNumber).getSuit());
        }

        System.out.println("to player: " + this.clientId);
    }

    public void removeClientHandler(){
        clientHandlers.remove(this);
    }

    private void closeEverything(Socket socket, ObjectInputStream in, ObjectOutputStream out){
        removeClientHandler();
        try {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
