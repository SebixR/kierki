package com.example.kierki;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * The ClientHandler class is responsible for all client-server communication.
 * Each time the server encounters a new client, it creates a new instance of
 * ClientHandler, which then establishes a connection with the client.
 * <p>
 *     The class holds a static list of all its other instances, so that it can broadcast information to all
 *     other clients, as well as a static HashMap of all he open rooms, where the room's ID is the key,
 *     and the room itself is the value. The Room object is where most game related information is stored,
 *     but the ClientHandler class additionally knows whether the client is logged in, and currently
 *     in game or not.
 * </p>
 * <p>
 *     The property usernames is used to store client's usernames. The keys are clientIDs,
 *     and the values are the usernames.
 * </p>
 * <p>
 *     Each client's ID is given to them by the Server application.
 * </p>
 */
public class ClientHandler implements Runnable {
    private static final List<ClientHandler> clientHandlers = new ArrayList<>();
    private static final HashMap<Integer, Room> rooms = new HashMap<>();
    private static int serverRoomId = 1;
    private Socket socket;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private int clientId; //given by the server
    private static final HashMap<Integer, String> usernames = new HashMap<>();
    private boolean loggedIn = false;
    private boolean isInGame = false;
    private static final int CARDS_IN_DECK = 52;
    private static final int CARDS_IN_HAND = 13;

    /**
     * The class constructor.
     * @param socket the socket accepted by the server.
     * @param clientId a unique ID given by the server.
     */
    public ClientHandler(Socket socket, int clientId) {
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
     * Handles most of client-server communication by responding accordingly based
     * on the received Request.
     */
    @Override
    public void run() {
        try {
            out.writeObject(clientId);
        } catch (IOException e) {
            closeEverything(socket, in, out);
        }

        while (socket.isConnected()){
            try {
                Request request = (Request) in.readObject();

                if (request == Request.REQUEST_USERNAME) {
                    handleRequestUsername();
                }
                else if (request == Request.CREATE_ROOM)
                {
                    handleCreateRoom();
                }
                else if (request == Request.INVITE_PLAYER)
                {
                    handleInvitePlayer();
                }
                else if (request == Request.JOIN_ROOM)
                {
                    handleJoinRoom();
                }
                else if (request == Request.DEAL_CARDS) {
                    handleDealCards();
                }
                else if (request == Request.PLAY_CARD) {
                    handlePlayCard();
                }
                else if (request == Request.EXIT_GAME) {
                    isInGame = false;
                }
                else if (request == Request.DISCONNECT) {
                    handleDisconnect();
                }

            } catch (Exception e) {
                closeEverything(socket, in, out);
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Handles a client request to set a username. Looks through the list of connected clients
     * to check if the username is taken, if it is ends "false", if not "true" and then the username.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleRequestUsername() throws IOException, ClassNotFoundException {
        String username = (String) in.readObject();

        out.reset();
        if (!usernames.containsValue(username)) {
            addUsername(username);
            this.loggedIn = true;
            out.writeObject(Response.SET_USERNAME);
            out.writeObject(true);
            out.writeObject(username);
            out.writeObject(rooms);
        }
        else
        {
            out.writeObject(Response.SET_USERNAME);
            out.writeObject(false);
        }
        out.flush();
    }

    /**
     * Handles the creation of a new room. After creating the room and setting all the necessary
     * parameters it broadcasts the newly created room to all players except for the host.
     * @throws IOException in/out communication exception
     */
    private void handleCreateRoom() throws IOException {
        Room room = new Room(clientId, serverRoomId, usernames.get(this.clientId));
        isInGame = true;
        addRoom(serverRoomId, room);
        serverRoomId++;

        out.reset();
        out.writeObject(Response.ROOM_CREATED);
        out.writeObject(room);
        out.flush();

        broadcastRooms(room);
    }

    /**
     * Handles invitations. When a client requests to invite another player, this function looks
     * for the player in question, and forwards the invitation to the, in the form of the
     * Invitation object.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    public void handleInvitePlayer() throws IOException, ClassNotFoundException {
        String inviteName = (String) in.readObject(); //someone else's ID
        int inviteId = 0;
        for (Map.Entry<Integer, String> entry : usernames.entrySet()) {
            if (entry.getValue().equals(inviteName)) inviteId = entry.getKey();
        }

        if (this.clientId != inviteId)
        {
            for (ClientHandler handler : clientHandlers) {
                if (handler.clientId == inviteId && !handler.isInGame)
                {
                    out.reset();
                    handler.out.writeObject(Response.INVITATION);
                    int foundRoomId = findRoom(this.clientId).getRoomId();
                    Invitation invitation = new Invitation(foundRoomId, usernames.get(clientId));
                    handler.out.writeObject(invitation);
                    handler.out.flush();
                    break;
                }
            }
        }
    }

    /**
     * Called when a player requests to join a room. If the room in question isn't full, adds the player
     * to it, informs it that the request has been approved, and tells all the clients about the change in
     * rooms.
     * @throws IOException in/out communication exception
     */
    private void handleJoinRoom() throws IOException {
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

    /**
     * Deals cards to the client connected to this instance of the class. To ensure the cards are dealt
     * at random, the list of cards is first shuffled.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleDealCards() throws IOException, ClassNotFoundException {
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

    /**
     * Handles all the events that may occur when a player plays a card. First, the method validates
     * the move. If the player is allowed to play the card, it then broadcasts the newly played card to
     * all the players in the room. If the played card is the fourth one on the table, the turn ends,
     * and another one starts. If none of the cards in the deck are in game, it starts a new round. And
     * if this was the final, 7th, round, it ends the game.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handlePlayCard() throws IOException, ClassNotFoundException {
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
                int points = calculatePoints(roomId);
                int takerClientId = pickLoosingPlayer(roomId);

                endTurn(roomId, takerClientId, points);

                broadcastPoints(points, takerClientId, rooms.get(roomId));

                int remainingCards = 0;
                for (int i = 0; i < CARDS_IN_DECK; i++) {
                    if (rooms.get(roomId).getCards().get(i).isInHand()) remainingCards++;
                }

                if (remainingCards == 0) {

                    if (rooms.get(roomId).getCurrentRound() == 7) {
                        int winnerId = findWinner(roomId);
                        String winnerName = usernames.get(winnerId);
                        endGame(roomId);
                        broadcastVictory(rooms.get(roomId), winnerName);
                    }
                    else {
                        changeRound(roomId);
                        broadcastRoundChange(rooms.get(roomId));
                    }
                }
            }
        }
    }

    /**
     * Called when a client closes their application. If a game was currently in progress,
     * a winner is chosen based on the current scores and the game ends.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleDisconnect() throws IOException, ClassNotFoundException {
        int roomId = (int) in.readObject();
        usernames.remove(this.clientId);

        if (rooms.get(roomId) == null) {
            closeEverything(socket, in, out);
        }

        if (!rooms.get(roomId).getGameOver()) {
            int winnerId = findWinner(roomId);
            String winnerName = usernames.get(winnerId);
            endGame(roomId);

            closeEverything(socket, in, out);
            broadcastVictory(rooms.get(roomId), winnerName);
            removeRoom(roomId);
        }
    }

    /**
     * Adds a new user to a HashMap containing all the connected user's ID's and usernames.
     * @param username the username this client chose, and the server accepted.
     */
    public synchronized void addUsername(String username) {
        usernames.put(this.clientId, username);
    }

    /**
     * Broadcasts a newly created, or updated room to all clients
     * @param room the new room
     */
    public synchronized void broadcastRooms(Room room){
        for (ClientHandler handler : clientHandlers){
            try {
                if (handler != this && handler.loggedIn)
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
     * Broadcasts the changes caused by a player playing a card, to all players in the rooom
     * @param room room with updated deck and turn
     * @param playedCard the played card, for making updating the GUI easier
     */
    public synchronized void broadcastPlay(Room room, Card playedCard){
        for (ClientHandler handler : clientHandlers){
            try {
                if (handler != this && room.getConnectedPlayers().contains(handler.clientId))
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

    /**
     * Broadcasts information at the end of a turn, to all players in the room
     * @param points the amount of points received by one player
     * @param clientId the player who receives the points
     * @param room the updated room, ready for the next turn
     */
    public synchronized void broadcastPoints(int points, int clientId, Room room) {
        for (ClientHandler handler : clientHandlers ){
            try {
                if (room.getConnectedPlayers().contains(handler.clientId)) {
                    handler.out.reset();
                    handler.out.writeObject(Response.TURN_OVER);
                    handler.out.writeObject(clientId);
                    handler.out.writeObject(points);
                    handler.out.writeObject(room);
                    handler.out.flush();
                }
            } catch (IOException e){
                closeEverything(socket, in, out);
            }
        }
    }

    /**
     * Informs all the players in the room that the round is over, and thus a new one started
     * @param room the updated room, ready for the next round
     */
    public synchronized void broadcastRoundChange(Room room) {
        for (ClientHandler handler : clientHandlers){
            try {
                if (room.getConnectedPlayers().contains(handler.clientId)) {
                    handler.out.reset();
                    handler.out.writeObject(Response.ROUND_OVER);
                    handler.out.writeObject(room);
                    handler.out.flush();
                }
            } catch (IOException e){
                closeEverything(socket, in, out);
            }
        }
    }

    /**
     * Informs all the players in the room that the game ended, and who is the winner
     * @param room the room in which the game ended
     * @param winnerName the winner
     */
    public synchronized void broadcastVictory(Room room, String winnerName) { //TODO username not ID
        for (ClientHandler handler : clientHandlers){
            try {
                if (room.getConnectedPlayers().contains(handler.clientId)) {
                    handler.out.reset();
                    handler.out.writeObject(Response.GAME_OVER);
                    handler.out.writeObject(winnerName);
                    handler.out.writeObject(room);
                    handler.out.flush();
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
            rooms.get(roomId).addPlayer(clientId, usernames.get(clientId)); //already sets the isFull field accordingly
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
                //System.out.println("Card: " + rooms.get(roomId).getCards().get(i).getValue() + " suit: " + rooms.get(roomId).getCards().get(i).getSuit());
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

    /**
     * Decides whether the player is allowed to play the card they chose
     * @param card the card the player wants to play
     * @param roomId the room where the game's taking place
     * @return true if the player is allowed to play the card, false otherwise
     */
    public boolean validateMove(Card card, int roomId) {

        if (rooms.get(roomId).getCurrentRound() == 1
                || rooms.get(roomId).getCurrentRound() == 3
                || rooms.get(roomId).getCurrentRound() == 4
                || rooms.get(roomId).getCurrentRound() == 6) {
            return handleRound1346(card, roomId);
        }
        else {
            return handleRound257(card, roomId);
        }
    }

    /**
     * Validates a player's move in rounds 1, 3, 4 and 6
     * @param card the card the player wants to play
     * @param roomId the room where the game's taking place
     * @return true if they are allowed to play the card, false otherwise
     */
    public boolean handleRound1346(Card card, int roomId) {
        if (rooms.get(roomId).getCardsOnTable().size() == 0) {
            return true;
        }
        else {
            Suit currentSuit = rooms.get(roomId).getFirstCardOnTable().getSuit();
            if (card.getSuit() == currentSuit) return true;

            return hasMatchingColor(currentSuit, roomId);
        }
    }

    /**
     * Validates a player's move in rounds 2, 5 and 7
     * @param card the card the player wants to play
     * @param roomId the room where the game's taking place
     * @return true if they are allowed to play the card, false otherwise
     */
    public boolean handleRound257(Card card, int roomId) {
        if (rooms.get(roomId).getCardsOnTable().size() == 0) {
            if (card.getSuit() != Suit.HEART) return true;
            else { // the card is a heart
                boolean hasOtherThanHeart = false;
                for (int i = 0; i < CARDS_IN_DECK; i++) {
                    if (rooms.get(roomId).getCards().get(i).getClientId() == this.clientId
                            && rooms.get(roomId).getCards().get(i).getSuit() != Suit.HEART
                            && rooms.get(roomId).getCards().get(i).isInHand()){
                        hasOtherThanHeart = true;
                    }
                }

                return !hasOtherThanHeart;
            }
        }
        else {
            Suit currentSuit = rooms.get(roomId).getFirstCardOnTable().getSuit();
            if (currentSuit == card.getSuit()) return true;

            return hasMatchingColor(currentSuit, roomId);
        }
    }

    /**
     * Checks it the player has a card that matchers the color of the first card
     * @param currentSuit the suit of the first card that was played in this turn
     * @param roomId the room where the game's taking place
     * @return false if the player has a matching card, true when they don't, and thus are allowed to play any card
     */
    public boolean hasMatchingColor(Suit currentSuit, int roomId){
        boolean hasMatchingColor = false;
        for (int i = 0; i < CARDS_IN_DECK; i++) {
            if (rooms.get(roomId).getCards().get(i).getClientId() == this.clientId
                    && rooms.get(roomId).getCards().get(i).getSuit() == currentSuit
                    && rooms.get(roomId).getCards().get(i).isInHand()){
                hasMatchingColor = true;
            }
        }
        return !hasMatchingColor;
    }

    /**
     * Prepares all the fields in Room for the next turn
     * @param roomId the room in which the turn ended
     * @param clientId the client taking the cards
     * @param points the amount of points the client receives
     */
    public synchronized void endTurn(int roomId, int clientId, int points) {
        rooms.get(roomId).getCardsOnTable().clear();
        rooms.get(roomId).setFirstCardOnTable(null);
        rooms.get(roomId).setCurrentTurn(clientId);
        rooms.get(roomId).givePoints(clientId, points); //playerPoints is filled with 0's when the fourth player joins
        rooms.get(roomId).incrementTurnCounter(); //necessary for the 6th round
    }

    /**
     * Increments the current round
     * @param roomId the room in which the round changes
     */
    public synchronized void changeRound(int roomId) {
        for (int i = 0; i < CARDS_IN_DECK; i++) {
            rooms.get(roomId).getCards().get(i).resetCard();
        }

        rooms.get(roomId).setCurrentTurn(rooms.get(roomId).getHostId());
        rooms.get(roomId).changeTurn();
        rooms.get(roomId).resetTurnCounter();
        rooms.get(roomId).incrementCurrentRound();
    }

    /**
     * Calculated the amount of points a player will gain based on the round
     * @param roomId the room in which the game's taking place
     * @return the amount of points the player will gain
     */
    public int calculatePoints(int roomId) {
        int points = 0;

        if (rooms.get(roomId).getCurrentRound() == 1) {
            points = 20; //someone is guaranteed to get points every turn
        }
        else if (rooms.get(roomId).getCurrentRound() == 2) {
            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (entry.getValue().getSuit() == Suit.HEART) points += 20; //for every heart
            }
        }
        else if (rooms.get(roomId).getCurrentRound() == 3) {
            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (entry.getValue().getValue() == 12) points += 60; //for every queen
            }
        }
        else if (rooms.get(roomId).getCurrentRound() == 4) {
            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (entry.getValue().getValue() == 11 || entry.getValue().getValue() == 13) points += 30; //for every sir
            }
        }
        else if (rooms.get(roomId).getCurrentRound() == 5) {
            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (entry.getValue().getValue() == 13 && entry.getValue().getSuit() == Suit.HEART) points += 150; //for every king of hearts
            }
        }
        else if (rooms.get(roomId).getCurrentRound() == 6) {
            for (Map.Entry<Integer, Card> ignored : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (rooms.get(roomId).getTurnCounter() == 7 || rooms.get(roomId).getTurnCounter() == 13) points += 75; //for the 7th and last turn
            }
        }
        else {
            points += 20; //someone is guaranteed to get points every turn
            for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
                if (entry.getValue().getSuit() == Suit.HEART) points += 20;
                if (entry.getValue().getValue() == 12) points += 60;
                if (entry.getValue().getValue() == 11 || entry.getValue().getValue() == 13) points += 30;
                if (entry.getValue().getValue() == 13 && entry.getValue().getSuit() == Suit.HEART) points += 150;
                if (rooms.get(roomId).getTurnCounter() == 7 || rooms.get(roomId).getTurnCounter() == 13) points += 75;
            }
        }

        return points;
    }

    /**
     * Picks the player who will gain points this turn, based on the cards on the table
     * @param roomId the room where the game takes place
     * @return the loosing client's ID
     */
    public int pickLoosingPlayer(int roomId) {
        int oldestCardValue = 1;
        int takerClientId = 0;
        for (Map.Entry<Integer, Card> entry : rooms.get(roomId).getCardsOnTable().entrySet()) {
            if (entry.getValue().getValue() > oldestCardValue
                    && entry.getValue().getSuit() == rooms.get(roomId).getFirstCardOnTable().getSuit()) {
                oldestCardValue = entry.getValue().getValue();
                takerClientId = entry.getValue().getClientId();
            }
        }
        return takerClientId;
    }

    /**
     * Finds the player with the minimum amount of points.
     * @param roomId the room where the game just took place
     * @return the ID of the client who won the game.
     */
    public int findWinner(int roomId) {
        int winnerId = 0;
        int minimumPoints = Integer.MAX_VALUE;
        for (Integer client : rooms.get(roomId).getConnectedPlayers()) {
            if (rooms.get(roomId).getPlayerPoints().get(client) <= minimumPoints) {
                minimumPoints = rooms.get(roomId).getPlayerPoints().get(client);
                winnerId = client;
            }
        }
        return winnerId;
    }

    public synchronized void endGame(int roomId) {
        rooms.get(roomId).toggleGameOver();
    }

    /**
     * Removes a room after a game has ended.
     * @param roomId the ID of the room being removed
     */
    public synchronized void removeRoom(int roomId) {
        rooms.remove(roomId);
    }

    /**
     * Removes the clientHandler when the client disconnects
     */
    public void removeClientHandler(){
        clientHandlers.remove(this);
    }

    /**
     * Severs all client-server connection.
     * @param socket the socket accepted by the server.
     * @param in input stream.
     * @param out output stream.
     */
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
