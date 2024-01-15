package com.example.kierki;

import java.io.Serializable;

public class Card implements Serializable {

    private final Suit suit;
    private final int value; //from 2 to 10, and then 11, 12, 13, and 14 for the special ones
    private int clientId = 0; //the client the card belongs to
    private boolean inHand; //true by default, changes to false when the card is played

    public Card (Suit suit, int value) {
        this.suit = suit;
        this.value = value;
        this.inHand = true;
    }

    public void resetCard() {
        this.inHand = true;
        this.clientId = 0;
    }

    public int getValue() {
        return value;
    }

    public Suit getSuit() {
        return suit;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isInHand() {
        return inHand;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setInHand(boolean inHand) {
        this.inHand = inHand;
    }
}
