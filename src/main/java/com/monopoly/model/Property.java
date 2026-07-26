package com.monopoly.model;

public class Property {
    private int id;
    private String ownerSessionId;
    private int houses;
    private boolean mortgaged; 

    public Property(int id) {
        this.id = id;
        this.houses = 0;
        this.mortgaged = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getOwnerSessionId() { return ownerSessionId; }
    public void setOwnerSessionId(String ownerSessionId) { this.ownerSessionId = ownerSessionId; }
    public int getHouses() { return houses; }
    public void setHouses(int houses) { this.houses = houses; }
    public boolean isMortgaged() { return mortgaged; }
    public void setMortgaged(boolean mortgaged) { this.mortgaged = mortgaged; }
}