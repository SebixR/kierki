package com.example.kierki;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class HeartsApp extends Application {
    private Client client;

    @Override
    public void start(Stage primaryStage) throws IOException, ClassNotFoundException {
        FXMLLoader usernameLoader = new FXMLLoader(getClass().getResource("username.fxml"));
        Scene usernameScene = new Scene(usernameLoader.load());
        UsernameController usernameController = usernameLoader.getController();

        FXMLLoader roomsLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        roomsLoader.load();
        RoomsController roomsController = roomsLoader.getController();

        FXMLLoader waitingLoader = new FXMLLoader(getClass().getResource("waiting.fxml"));
        waitingLoader.load();
        WaitingController waitingController = waitingLoader.getController();

        FXMLLoader gameLoader = new FXMLLoader(getClass().getResource("game.fxml"));
        gameLoader.load();
        GameController gameController = gameLoader.getController();

        Socket socket = new Socket("127.0.0.1", 6666);
        client = new Client(socket, primaryStage, usernameController, roomsController, roomsLoader, waitingController, waitingLoader, gameController, gameLoader);
        client.receiveID();
        System.out.println("Connected client " + client.getClientId());
        client.listen();

        usernameController.setClient(client);
        roomsController.setClient(client);
        waitingController.setClient(client);
        gameController.setClient(client);

        usernameController.hideErrorLabel();

        primaryStage.setScene(usernameScene);
        primaryStage.setTitle("Hearts");
        primaryStage.show();
    }

    @Override
    public void stop() {
        try {
            client.disconnectClient();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to notify the server when disconnecting");
            System.exit(2);
        }
        client.closeEverything();
    }

    public static void main(String[] args) {
        try {
            launch(args);

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the server");
            System.exit(2);
        }

    }
}
