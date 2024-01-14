package com.example.kierki;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class HeartsApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException, ClassNotFoundException {
        FXMLLoader roomsLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene roomsScene = new Scene(roomsLoader.load());
        RoomsController roomsController = roomsLoader.getController();

        FXMLLoader waitingLoader = new FXMLLoader(getClass().getResource("waiting.fxml"));
        waitingLoader.load();
        WaitingController waitingController = waitingLoader.getController();

        FXMLLoader gameLoader = new FXMLLoader(getClass().getResource("game.fxml"));
        gameLoader.load();
        GameController gameController = gameLoader.getController();

        Socket socket = new Socket("127.0.0.1", 6666);
        Client client = new Client(socket, primaryStage, roomsController, waitingController, waitingLoader, gameController, gameLoader);
        client.receiveID();
        System.out.println("Connected client " + client.getClientId());

        client.receiveRooms();
        System.out.println("Available rooms:");

        for (Map.Entry<Integer, Room> entry : client.getRooms().entrySet()) {
            System.out.println("Id: " + entry.getValue().getRoomId() + " players: " + entry.getValue().getPlayerAmount() + "/4\n");
        }
        client.listen();

        roomsController.setClient(client);
        waitingController.setClient(client);
        gameController.setClient(client);

        primaryStage.setScene(roomsScene);
        primaryStage.setTitle("Hearts");
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            launch(args);

            //client.closeEverything();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the server");
            System.exit(2);
        }

    }
}
