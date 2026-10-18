package com.rcpky;

import com.rcpky.enums.CardStatus;

import java.time.LocalDate;

public class Card {
    private final String cardNumber;
    private final String linkedAccountId;
    private final LocalDate expiryDate;
    private CardStatus cardStatus;

    public Card(String cardNumber, String linkedAccountId, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.linkedAccountId = linkedAccountId;
        this.expiryDate = expiryDate;
        this.cardStatus = CardStatus.ACTIVE;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return cardStatus == CardStatus.ACTIVE && !isExpired();
    }

    public void block() {
        this.cardStatus = CardStatus.BLOCKED;
    }

    public String getCardNumber()      { return cardNumber; }
    public String getLinkedAccountId() { return linkedAccountId; }
    public CardStatus getCardStatus()  { return cardStatus; }
    public LocalDate getExpiryDate()   { return expiryDate; }

    @Override
    public String toString() {
        return "Card{number=" + cardNumber + ", status=" + cardStatus + ", expiry=" + expiryDate + "}";
    }
}
