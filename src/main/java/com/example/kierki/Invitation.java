package com.example.kierki;

import java.io.Serializable;

public class Invitation implements Serializable {

    private int roomId;
    private int inviterId;

    public Invitation(int inviterId, int roomId) {
        this.inviterId = inviterId;
        this.roomId = roomId;
    }

    public int getInviterId() {
        return inviterId;
    }

    public int getRoomId() {
        return roomId;
    }
}
