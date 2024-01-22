package com.example.kierki;

/**
 * Enum containing all the responses the server can send to the client.
 */
public enum Response {
    SET_USERNAME,
    ROOMS_UPDATE,
    INVITATION,
    JOINED_ROOM,
    ROOM_CREATED,
    DEALT_CARDS,
    PLAYED_CARD,
    CARDS_UPDATE,
    TURN_OVER,
    ROUND_OVER,
    GAME_OVER
}
