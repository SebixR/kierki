package com.example.kierki;

import java.io.*;
import java.net.Socket;
import java.util.*;


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
    private static final int CARDS_IN_HAND = 13;

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
                else if (request == Request.DEAL_CARDS) {
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
                else if (request == Request.PLAY_CARD) {
                    Card card = (Card) in.readObject();
                    System.out.println("Client: " + this.clientId + " requested to play card: " + card.getValue() + " " + card.getSuit());
                    int roomId = (int) in.readObject();

                    if (validateMove(card, roomId)) {
                        int cardIndex = playCard(roomId, card);

                        out.reset();
                        out.writeObject(Response.PLAYED_CARD);
                        out.writeObject(rooms.get(roomId).getCards().get(cardIndex));
                        out.writeObject(rooms.get(roomId));
                        out.flush();

                        broadcastPlay(rooms.get(roomId), card);

                        if (rooms.get(roomId).getCardsOnTable().size() == 4) {
                            out.reset();

                            int oldestCardValue = 2;
                            int takerClientId = rooms.get(roomId).getCardsOnTable().get(this.clientId).getClientId();
                            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                                if (entry.getValue().getValue() > oldestCardValue && entry.getValue().getSuit() == rooms.get(roomId).getFirstCardOnTable().getSuit()) {
                                    oldestCardValue = entry.getValue().getValue();
                                    takerClientId = entry.getValue().getClientId();
                                }
                            }

                            endTurn(roomId, takerClientId, 20);

                            broadcastPoints(20, takerClientId, rooms.get(roomId));
                            out.flush();
                        }
                    }
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
     * Broadcasts the changes caused by a player playing a card
     * @param room room with updated deck and turn
     * @param playedCard the played card, for making updating the GUI easier
     */
    public synchronized void broadcastPlay(Room room, Card playedCard){
        for (ClientHandler handler : clientHandlers){
            try {
                if (handler != this)
                {
                    handler.out.reset();
                    handler.out.writeObject(Response.CARDS_UPDATE);
                    handler.out.writeObject(playedCard);
                    handler.out.writeObject(room);
                    out.flush();
                }
            } catch (IOException e){
                closeEverything(socket, in, out);
            }
        }
    }

    public synchronized void broadcastPoints(int points, int clientId, Room room) {
        for (ClientHandler handler : clientHandlers){
            try {
                handler.out.reset();
                handler.out.writeObject(Response.TURN_OVER);
                handler.out.writeObject(clientId);
                handler.out.writeObject(points);
                handler.out.writeObject(room);
                handler.out.flush();
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
            rooms.get(roomId).addPlayer(clientId); //already sets the isFull field accordingly
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
        System.out.println("Dealt cards : ");

        Collections.shuffle(rooms.get(roomId).getCards());

        int counter = 0;
        for (int i = 0; i < CARDS_IN_DECK && counter < CARDS_IN_HAND; i++)
        {
            if (rooms.get(roomId).getCards().get(i).getClientId() == 0) {
                rooms.get(roomId).getCards().get(i).setClientId(this.clientId);
                System.out.println("Card: " + rooms.get(roomId).getCards().get(i).getValue() + " suit: " + rooms.get(roomId).getCards().get(i).getSuit());
                counter++;
            }
        }

        System.out.println("to player: " + this.clientId);
    }

    /**
     * Sets the card as played, and updates whose turn it is to play
     * @param roomId room in which the card was played
     * @param playedCard card played
     * @return the index of the played card, so that the updated card can be sent to the client
     */
    public synchronized int playCard(int roomId, Card playedCard) {

        int cardIndex = 0;
        for (int i = 0; i < CARDS_IN_DECK; i++){
            if (rooms.get(roomId).getCards().get(i).getSuit() == playedCard.getSuit()
                    && rooms.get(roomId).getCards().get(i).getValue() == playedCard.getValue()){
                cardIndex = i;
                break;
            }
        }
        rooms.get(roomId).getCards().get(cardIndex).setInHand(false);
        rooms.get(roomId).changeTurn();
        if (rooms.get(roomId).getCardsOnTable().size() == 0) rooms.get(roomId).setFirstCardOnTable(rooms.get(roomId).getCards().get(cardIndex));
        rooms.get(roomId).getCardsOnTable().put(this.clientId, rooms.get(roomId).getCards().get(cardIndex)); //not playedCard, because we want to keep the updated inHand field
        return cardIndex;
    }

    public boolean validateMove(Card card, int roomId) {

        if (rooms.get(roomId).getCurrentRound() == 1) {
            return handleRound1(card, roomId);
        }

        return false;
    }

    public boolean handleRound1(Card card, int roomId) {
        if (rooms.get(roomId).getCardsOnTable().size() == 0) {
            return true;
        }
        else {
            Suit currentSuit = rooms.get(roomId).getFirstCardOnTable().getSuit();
            if (card.getSuit() == currentSuit) return true;

            boolean hasMatchingColor = false;
            for (int i = 0; i < CARDS_IN_DECK; i++) {
                if (rooms.get(roomId).getCards().get(i).getClientId() == this.clientId
                        && rooms.get(roomId).getCards().get(i).getSuit() == currentSuit){
                    hasMatchingColor = true;
                }
            }
            return !hasMatchingColor;
        }
    }

    public synchronized void endTurn(int roomId, int clientId, int points) {
        rooms.get(roomId).getCardsOnTable().clear();
        rooms.get(roomId).setFirstCardOnTable(null);
        rooms.get(roomId).setCurrentTurn(clientId);
        rooms.get(roomId).getPlayerPoints().put(clientId, points);
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
