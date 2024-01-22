package com.example.kierki;

/**
 * Enum containing all the types of requests a client can make to the server.
 */
public enum Request {
    REQUEST_USERNAME,
    CREATE_ROOM,
    INVITE_PLAYER,
    JOIN_ROOM,
    DEAL_CARDS,
    PLAY_CARD,
    EXIT_GAME,
    DISCONNECT
}
