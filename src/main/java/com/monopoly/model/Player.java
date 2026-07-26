package com.monopoly.model;

public class Player {
    private String sessionId; 
    private String name;      
    private int position;     
    private int money;        
    private boolean inJail;
    private String color;           
    private int lastBuiltPosition;  
    private boolean bankrupt; 
    
    private boolean freeRentCard; 
    private boolean skipTurn;     
    private int lapCount; // Đếm số vòng để ăn Nổ Hũ

    public Player(String sessionId, String name, String color) {
        this.sessionId = sessionId;
        this.name = name;
        this.color = color;
        this.position = 0;    
        this.money = 400;    
        this.inJail = false;
        this.lastBuiltPosition = -1; 
        this.bankrupt = false;
        this.freeRentCard = false;
        this.skipTurn = false;
        this.lapCount = 0; 
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
    public boolean isInJail() { return inJail; }
    public void setInJail(boolean inJail) { this.inJail = inJail; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getLastBuiltPosition() { return lastBuiltPosition; }
    public void setLastBuiltPosition(int lastBuiltPosition) { this.lastBuiltPosition = lastBuiltPosition; }
    public boolean isBankrupt() { return bankrupt; }
    public void setBankrupt(boolean bankrupt) { this.bankrupt = bankrupt; }

    public boolean isFreeRentCard() { return freeRentCard; }
    public void setFreeRentCard(boolean freeRentCard) { this.freeRentCard = freeRentCard; }
    public boolean isSkipTurn() { return skipTurn; }
    public void setSkipTurn(boolean skipTurn) { this.skipTurn = skipTurn; }
    
    public int getLapCount() { return lapCount; }
    public void setLapCount(int lapCount) { this.lapCount = lapCount; }
}