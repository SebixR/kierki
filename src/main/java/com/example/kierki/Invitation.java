package com.example.kierki;

import java.io.Serializable;

public class Invitation implements Serializable {

    private final int roomId;
    private final int inviterId;
    private final String inviterName;

    public Invitation(int inviterId, int roomId, String inviterName) {
        this.inviterId = inviterId;
        this.roomId = roomId;
        this.inviterName = inviterName;
    }

    public int getInviterId() {
        return inviterId;
    }

    public int getRoomId() {
        return roomId;
    }
    public String getInviterName() {
        return this.inviterName;
    }
}
