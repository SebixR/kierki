package com.example.kierki;

import java.io.Serializable;

/**
 * The Invitation class is used when one user requests to invite another.
 * It contains all the information the invited player needs, namely:
 * the ID of the room to which they are invited, the username of the player
 * who invited them.
 */
public class Invitation implements Serializable {

    private final int roomId;
    private final String inviterName;

    public Invitation(int roomId, String inviterName) {
        this.roomId = roomId;
        this.inviterName = inviterName;
    }

    public int getRoomId() {
        return roomId;
    }
    public String getInviterName() {
        return this.inviterName;
    }
}
