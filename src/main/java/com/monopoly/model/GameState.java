package com.monopoly.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    private Map<String, Player> players = new HashMap<>();
    private Map<Integer, Property> properties = new HashMap<>();
    private String latestMessage;
    private int dice1;
    private int dice2;
    
    // --- QUẢN LÝ LƯỢT & NỔ HŨ ---
    private List<String> turnOrder = new ArrayList<>();
    private String currentTurnId;
    private boolean hasRolledThisTurn;
    private int jackpotPool = 100;

    public Map<String, Player> getPlayers() { return players; }
    public void setPlayers(Map<String, Player> players) { this.players = players; }

    public Map<Integer, Property> getProperties() { return properties; }
    public void setProperties(Map<Integer, Property> properties) { this.properties = properties; }

    public String getLatestMessage() { return latestMessage; }
    public void setLatestMessage(String latestMessage) { this.latestMessage = latestMessage; }

    public int getDice1() { return dice1; }
    public void setDice1(int dice1) { this.dice1 = dice1; }

    public int getDice2() { return dice2; }
    public void setDice2(int dice2) { this.dice2 = dice2; }

    public List<String> getTurnOrder() { return turnOrder; }
    public void setTurnOrder(List<String> turnOrder) { this.turnOrder = turnOrder; }

    public String getCurrentTurnId() { return currentTurnId; }
    public void setCurrentTurnId(String currentTurnId) { this.currentTurnId = currentTurnId; }

    public boolean isHasRolledThisTurn() { return hasRolledThisTurn; }
    public void setHasRolledThisTurn(boolean hasRolledThisTurn) { this.hasRolledThisTurn = hasRolledThisTurn; }

    public int getJackpotPool() { return jackpotPool; }
    public void setJackpotPool(int jackpotPool) { this.jackpotPool = jackpotPool; }
}