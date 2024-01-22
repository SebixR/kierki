package com.example.kierki;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Class representing a playing card.
 * <p>
 *     Each card has a suit (diamond, club, heart or spade),
 *     a value going from 2 to 14 (11 being the jack, 13 th king etc.),
 *     the card's owner's ID, or zero if the card hasn't been dealt yet,
 *     and the inHand parameter, telling us if the card has already been
 *     played or not.
 * </p>
 */
public class Card implements Serializable {

    private final Suit suit;
    private final int value;
    private int clientId = 0;
    private boolean inHand;

    /**
     * Card class constructor.
     * @param suit the card's suit (diamond, club, heart or spade)
     * @param value the card's value (2-14)
     */
    public Card (Suit suit, int value) {
        this.suit = suit;
        this.value = value;
        this.inHand = true;
    }

    /**
     * Resets the card so that it can be dealt again.
     */
    public void resetCard() {
        this.inHand = true;
        this.clientId = 0;
    }

    /**
     * Comparator for sorting cards.
     */
    public static class CardComparator implements Comparator<Card> {
        public int compare(Card a, Card b) {
            if (a.suit == b.suit) {
                return a.value - b.value;
            }
            else {
                return a.suit.compareTo(b.suit);
            }
        }
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

    /**
     * Getter for the boolean parameter inHand.
     * @return false if it has been played already, true otherwise.
     */
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
