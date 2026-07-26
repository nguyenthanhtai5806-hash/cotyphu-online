package com.hoiquangaming.cotyphu.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monopoly.model.GameState;
import com.monopoly.model.Player;
import com.monopoly.model.Property;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new ArrayList<>();
    private static final GameState gameState = new GameState();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] PLAYER_COLORS = {"#ff4d94", "#2ecfa1", "#5b6cf0", "#f0b429", "#ff6b4a", "#9b51e0"};
    private static int currentTurnIndex = 0;

    private void nextTurn() {
        gameState.setHasRolledThisTurn(false);
        List<String> turnOrder = gameState.getTurnOrder();
        if (turnOrder.isEmpty()) return;
        
        int attempts = 0;
        do {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
            String nextId = turnOrder.get(currentTurnIndex);
            Player p = gameState.getPlayers().get(nextId);
            if (p != null && !p.isBankrupt()) {
                if (p.isSkipTurn()) {
                    p.setSkipTurn(false);
                    gameState.setLatestMessage("⏳ " + p.getName() + " bị Cấm Túc mất 1 lượt! Chuyển quyền cho người tiếp theo.");
                    continue; 
                }
                gameState.setCurrentTurnId(nextId);
                break; 
            }
            attempts++;
        } while (attempts < turnOrder.size());
    }

    private int getPropertyPrice(int pos) {
        if (pos == 0 || pos == 14 || pos == 28 || pos == 42) return 0; 
        if (pos == 7 || pos == 21 || pos == 35 || pos == 49) return 200; 
        if (pos == 19 || pos == 40) return 150; 
        int[] nonBuyable = {4, 10, 12, 18, 24, 32, 38, 46, 52}; 
        for (int n : nonBuyable) { if (pos == n) return 0; }
        return 80 + (pos * 8); 
    }

    private int calculateRent(int cellId, Player owner, int totalDice) {
        if (owner.isInJail()) return 0;
        Property p = gameState.getProperties().get(cellId);
        if (p != null && p.isMortgaged()) return 0; 

        int price = getPropertyPrice(cellId);
        if (cellId == 7 || cellId == 21 || cellId == 35 || cellId == 49) {
            int count = 0;
            for (int s : new int[]{7, 21, 35, 49}) {
                Property prop = gameState.getProperties().get(s);
                if (prop != null && prop.getOwnerSessionId().equals(owner.getSessionId()) && !prop.isMortgaged()) count++;
            }
            return count == 0 ? 0 : 20 * (int)Math.pow(2, count - 1); 
        }
        if (cellId == 19 || cellId == 40) {
            int count = 0;
            for (int u : new int[]{19, 40}) {
                Property prop = gameState.getProperties().get(u);
                if (prop != null && prop.getOwnerSessionId().equals(owner.getSessionId()) && !prop.isMortgaged()) count++;
            }
            return count == 0 ? 0 : totalDice * (count == 1 ? 5 : 10);
        }
        int baseRent = (int) (price * 0.1);
        if (p != null) {
            int h = p.getHouses();
            if (h == 1) return baseRent * 2; else if (h == 2) return baseRent * 4;
            else if (h == 3) return baseRent * 6; else if (h == 4) return baseRent * 10; 
        }
        return baseRent;
    }

    private int calculateNetWorth(Player player) {
        int netWorth = player.getMoney();
        for (Property prop : gameState.getProperties().values()) {
            if (prop.getOwnerSessionId().equals(player.getSessionId()) && !prop.isMortgaged()) {
                int mortgageVal = (int) (getPropertyPrice(prop.getId()) * 0.5);
                int h = prop.getHouses();
                if (h == 1) mortgageVal += 100; else if (h == 2) mortgageVal += 200; else if (h == 3) mortgageVal += 300; else if (h == 4) mortgageVal += 500;
                netWorth += mortgageVal;
            }
        }
        return netWorth;
    }

    private String drawChanceCard(Player player) {
        int rand = (int) (Math.random() * 12);
        List<Player> others = new ArrayList<>();
        for (Player p : gameState.getPlayers().values()) {
            if (!p.getSessionId().equals(player.getSessionId()) && !p.isBankrupt()) others.add(p);
        }
        switch (rand) {
            case 0: player.setMoney(player.getMoney() + 150); return " 🃏 [CƠ HỘI]: Bug ngân hàng lỡ tay chuyển nhầm, húp giải an ủi 150$!";
            case 1: player.setMoney(player.getMoney() + 100); return " 🃏 [CƠ HỘI]: Báo cáo đồ án mượt như Sunsilk, nhận 100$!";
            case 2: player.setMoney(player.getMoney() - 50); gameState.setJackpotPool(gameState.getJackpotPool() + 50); return " 🃏 [CƠ HỘI]: Rớt ổ gà, sửa xe 50$ (Vào Nổ Hũ)!";
            case 3: player.setMoney(player.getMoney() + 100); return " 🃏 [CƠ HỘI]: Giao tiếp tiếng Anh IT mượt, khách bo 100$!";
            case 4: player.setMoney(player.getMoney() - 100); gameState.setJackpotPool(gameState.getJackpotPool() + 100); return " 🃏 [CƠ HỘI]: Đú mua iPhone 16 trả góp, ngân hàng siết nợ 100$ (Vào Nổ Hũ)!";
            case 5: int gain = others.size() * 30; for(Player p : others) p.setMoney(p.getMoney() - 30); player.setMoney(player.getMoney() + gain); return " 🃏 [CƠ HỘI]: Kêu gọi Startup lùa gà! Nhận 30$ từ MỖI NGƯỜI CHƠI!";
            case 6: int loss = others.size() * 30; for(Player p : others) p.setMoney(p.getMoney() + 30); player.setMoney(player.getMoney() - loss); return " 🃏 [CƠ HỘI]: Bao cả nhóm đi nhậu! Trừ ví chia 30$ cho MỖI NGƯỜI CHƠI!";
            case 7: player.setPosition(0); player.setMoney(player.getMoney() + 200); player.setLapCount(player.getLapCount() + 1); return " 🃏 [CƠ HỘI]: Bay về XUẤT PHÁT, lĩnh 200$ lương (Hoàn thành vòng " + player.getLapCount() + ")!";
            case 8: player.setMoney(player.getMoney() - 50); gameState.setJackpotPool(gameState.getJackpotPool() + 50); return " 🃏 [CƠ HỘI]: Nộp phạt đỗ xe mua xôi 50$ (Vào Nổ Hũ)!";
            case 9: player.setFreeRentCard(true); return " 🃏 [CƠ HỘI]: Nhận 1 thẻ 🛡️ QUÝ NHÂN (Miễn trả tiền thuê đất 1 lần)!";
            case 10: player.setMoney(player.getMoney() + 250); player.setInJail(true); player.setPosition(14); gameState.setHasRolledThisTurn(true); return " 💀 [BONUS]: Bán tools lậu thu 250$ nhưng bị bế VÀO TÙ NGAY!";
            case 11: player.setMoney(player.getMoney() - 100); player.setSkipTurn(true); gameState.setJackpotPool(gameState.getJackpotPool() + 100); return " 💀 [BONUS]: Trầm cảm! Đóng 100$ viện phí (vào Nổ Hũ) và MẤT 1 LƯỢT ĐI kế tiếp!";
            default: return "";
        }
    }

    private String drawChestCard(Player player) {
        int rand = (int) (Math.random() * 12);
        List<Player> others = new ArrayList<>();
        for (Player p : gameState.getPlayers().values()) {
            if (!p.getSessionId().equals(player.getSessionId()) && !p.isBankrupt()) others.add(p);
        }
        Player target = others.isEmpty() ? null : others.get((int)(Math.random() * others.size()));
        switch (rand) {
            case 0: player.setMoney(player.getMoney() + 100); return " 🎁 [KHÍ VẬN]: Bạn cũ báo mộng trả nợ 100$!";
            case 1: player.setMoney(player.getMoney() - 100); gameState.setJackpotPool(gameState.getJackpotPool() + 100); return " 🎁 [KHÍ VẬN]: Xài Win lậu nộp phạt 100$ (Sung vào Nổ Hũ)!";
            case 2: int roll = (int)(Math.random() * 6) + 1; if(roll <= 3) { player.setMoney(player.getMoney() - 100); gameState.setJackpotPool(gameState.getJackpotPool() + 100); return " 🎁 [KHÍ VẬN]: All-in coin đổ ra " + roll + ". Lỗ mất 100$ (Vào Nổ Hũ)!"; } else { player.setMoney(player.getMoney() + 200); return " 🎁 [KHÍ VẬN]: All-in coin đổ ra " + roll + ". Lãi 200$!"; }
            case 3: int houses = 0; for(Property p : gameState.getProperties().values()) { if(p.getOwnerSessionId().equals(player.getSessionId())) houses += p.getHouses(); } int penalty = houses * 50; player.setMoney(player.getMoney() - penalty); gameState.setJackpotPool(gameState.getJackpotPool() + penalty); return " 🎁 [KHÍ VẬN]: Thiên tai! Đóng 50$/nhà. Tổng: " + penalty + "$ (Vào Nổ Hũ)!";
            case 4: player.setMoney(player.getMoney() + 150); gameState.setHasRolledThisTurn(false); return " 🎁 [KHÍ VẬN]: Web lên Render thành công! Trúng 150$! ĐƯỢC ĐỔ XÚC XẮC THÊM LƯỢT NỮA!";
            case 5: player.setMoney(player.getMoney() - 80); gameState.setJackpotPool(gameState.getJackpotPool() + 80); return " 🎁 [KHÍ VẬN]: Rớt ví mất 80$ (Người nhặt nộp vào Nổ Hũ)!";
            case 6: if (target != null) { target.setMoney(target.getMoney() - 100); player.setMoney(player.getMoney() + 100); return " 🎁 [KHÍ VẬN]: Quẹt Tinder trúng Chủ tịch " + target.getName() + " tài trợ 100$!"; } else return " 🎁 [KHÍ VẬN]: Móm!";
            case 7: player.setMoney(player.getMoney() + 200); return " 🎁 [KHÍ VẬN]: Đất quê giải tỏa đền bù 200$!";
            case 8: if (target != null) { player.setMoney(player.getMoney() - 100); target.setMoney(target.getMoney() + 100); return " 🎁 [KHÍ VẬN]: Quên kiến thức Hóa Sinh làm nổ bình! Đền 100$ cho " + target.getName() + "!"; } else { player.setMoney(player.getMoney() - 100); gameState.setJackpotPool(gameState.getJackpotPool() + 100); return " 🎁 [KHÍ VẬN]: Phạt xả rác 100$ (Vào Nổ Hũ)!"; }
            case 9: int jump = (int)(Math.random() * 56); player.setPosition(jump); return " 🎁 [KHÍ VẬN]: Trúng vé du lịch đáp thẳng xuống ô số " + jump + "!";
            case 10: if (target != null) { target.setMoney(target.getMoney() - 100); player.setMoney(player.getMoney() + 100); return " 💀 [BONUS]: Hack trót lọt 100$ từ ví " + target.getName() + "!"; } else return " 💀 [BONUS]: Server vắng tanh!";
            case 11: if (target != null) { int m = player.getMoney(); player.setMoney(target.getMoney()); target.setMoney(m); return " 💀 [BONUS]: LẬT KÈO! Tráo đổi TOÀN BỘ tiền mặt với " + target.getName() + "!"; } else return " 💀 [BONUS]: Bốc bài chơi với ma!";
            default: return "";
        }
    }

    private void processMovement(Player player, int steps, String msgPrefix) {
        player.setLastBuiltPosition(-1); 
        int oldPosition = player.getPosition();
        int newPosition = oldPosition + steps;

        if (newPosition > 55) {
            newPosition = newPosition - 56;
            player.setMoney(player.getMoney() + 200);
            player.setLapCount(player.getLapCount() + 1);
            msgPrefix += " 💰 Nhận 200$ qua Xuất Phát (Vòng " + player.getLapCount() + ")!";
        }
        
        player.setPosition(newPosition);
        String logMsg = msgPrefix + " Tiến đến ô " + newPosition + ".";
        
        if (newPosition == 42) {
            player.setPosition(14); 
            player.setInJail(true); 
            gameState.setHasRolledThisTurn(true); 
            logMsg += " 🚓 TÍ TÒ TE! Bị bế thẳng vào Tù!";
            gameState.setLatestMessage(logMsg);
            broadcastGameState();
            return; 
        }

        if (newPosition == 12) {
            player.setMoney(player.getMoney() - 200);
            gameState.setJackpotPool(gameState.getJackpotPool() + 200);
            logMsg += " 💸 Trốn thuế nộp phạt 200$ (Vào Nổ Hũ)!";
        }
        else if (newPosition == 28) {
            if (gameState.getJackpotPool() > 0) {
                if (player.getLapCount() >= 3) {
                    player.setMoney(player.getMoney() + gameState.getJackpotPool());
                    logMsg += " 🎰 JACKPOT NỔ!!! " + player.getName() + " cày đủ 3 vòng hốt trọn " + gameState.getJackpotPool() + "$! Đổi đời!";
                    gameState.setJackpotPool(0); player.setLapCount(0); 
                } else {
                    logMsg += " 🎰 Đạp trúng NỔ HŨ " + gameState.getJackpotPool() + "$ nhưng mới cày được " + player.getLapCount() + "/3 vòng! Nhịn!";
                }
            } else logMsg += " 🎰 NỔ HŨ rỗng tuếch! Chờ anh em đóng họ!";
        }
        else if (newPosition == 4 || newPosition == 18 || newPosition == 32 || newPosition == 46) {
            logMsg += drawChanceCard(player);
        }
        else if (newPosition == 10 || newPosition == 24 || newPosition == 38 || newPosition == 52) {
            logMsg += drawChestCard(player);
        }
        else {
            int price = getPropertyPrice(newPosition);
            Property prop = gameState.getProperties().get(newPosition);
            if (price > 0 && prop != null) {
                if (!prop.getOwnerSessionId().equals(player.getSessionId())) {
                    Player owner = gameState.getPlayers().get(prop.getOwnerSessionId());
                    if (owner != null) {
                        if (prop.isMortgaged()) logMsg += " 🏦 Đất cắm Ngân hàng, lủi qua miễn phí!";
                        else if (owner.isInJail()) logMsg += " 🔒 Chủ đang bóc lịch, đi qua miễn phí!"; 
                        else if (player.isFreeRentCard()) {
                            player.setFreeRentCard(false);
                            logMsg += " 🛡️ Xài thẻ QUÝ NHÂN! Lách luật không đóng tiền tô!";
                        } else {
                            int rent = calculateRent(newPosition, owner, steps);
                            player.setMoney(player.getMoney() - rent);
                            owner.setMoney(owner.getMoney() + rent);
                            logMsg += " 💸 Bị " + owner.getName() + " lột sạch " + rent + "$ tiền tô!";
                        }
                    }
                } else logMsg += " 🏠 Đất của tao tao đứng!";
            }
        }

        if (player.getMoney() < 0) {
            int totalNetWorth = calculateNetWorth(player);
            if (totalNetWorth < 0) {
                player.setBankrupt(true);
                List<Integer> propsToWipe = new ArrayList<>();
                for (Property p : gameState.getProperties().values()) {
                    if (p.getOwnerSessionId().equals(player.getSessionId())) propsToWipe.add(p.getId());
                }
                for (int id : propsToWipe) gameState.getProperties().remove(id);
                logMsg += " 💀 VỠ NỢ CÒN CÁI NỊT! " + player.getName() + " PHÁ SẢN!";
            } else logMsg += " 🚨 Tài khoản ÂM TIỀN, cắm sổ đỏ gấp!";
        }
        gameState.setLatestMessage(logMsg);
        broadcastGameState();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        gameState.getTurnOrder().add(session.getId());
        
        String playerName = "Player_" + session.getId().substring(0, 4);
        String color = PLAYER_COLORS[sessions.size() % PLAYER_COLORS.length];
        Player newPlayer = new Player(session.getId(), playerName, color);
        gameState.getPlayers().put(session.getId(), newPlayer);
        
        if (gameState.getTurnOrder().size() == 1) {
            gameState.setCurrentTurnId(session.getId());
            gameState.setHasRolledThisTurn(false);
        }
        
        session.sendMessage(new TextMessage("YOUR_ID:" + session.getId()));
        gameState.setLatestMessage("🟢 " + playerName + " đã tham gia sòng bài!");
        broadcastGameState();
    }

   @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        
        // 1. Nhận diện kẻ bỏ trốn
        boolean wasCurrentTurn = session.getId().equals(gameState.getCurrentTurnId());
        int oldIndex = gameState.getTurnOrder().indexOf(session.getId());
        
        // 2. Xóa sổ hộ khẩu và thu hồi tài sản
        gameState.getTurnOrder().remove(session.getId());
        Player p = gameState.getPlayers().remove(session.getId());

        if (p != null) {
            List<Integer> propsToWipe = new ArrayList<>();
            for (Property prop : gameState.getProperties().values()) {
                if (prop.getOwnerSessionId().equals(session.getId())) propsToWipe.add(prop.getId());
            }
            for (int id : propsToWipe) gameState.getProperties().remove(id);
            gameState.setLatestMessage("🔴 " + p.getName() + " rớt mạng! Đất đai bị thu hồi!");
        }

        // 3. Xử lý chia lại lượt thông minh
        if (gameState.getTurnOrder().isEmpty()) {
            // Phòng trống -> Reset
            gameState.setCurrentTurnId("");
            currentTurnIndex = 0;
        } else if (wasCurrentTurn) {
            // Nếu người trốn đang cầm lượt -> Chuyển ngay cho người kế tiếp
            if (currentTurnIndex >= gameState.getTurnOrder().size()) {
                currentTurnIndex = 0;
            }
            gameState.setCurrentTurnId(gameState.getTurnOrder().get(currentTurnIndex));
            gameState.setHasRolledThisTurn(false);
            gameState.setLatestMessage("⏩ Kẻ gian sủi mất tăm! Chuyển quyền đổ xúc xắc cho người tiếp theo!");
        } else {
            // Nếu người trốn chưa tới lượt -> Cập nhật lại bộ đếm để không bị lệch pha
            if (oldIndex < currentTurnIndex) {
                currentTurnIndex--;
            }
            if (currentTurnIndex >= gameState.getTurnOrder().size()) {
                currentTurnIndex = 0;
            }
        }
        broadcastGameState();
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Player player = gameState.getPlayers().get(session.getId());
        if (player == null || player.isBankrupt()) return;

        boolean isMyTurn = session.getId().equals(gameState.getCurrentTurnId());

        if ("END_TURN".equals(payload)) {
            if (!isMyTurn) return;
            if (!gameState.isHasRolledThisTurn()) {
                gameState.setLatestMessage("❌ Chưa đổ xúc xắc mà đòi qua lượt?");
                broadcastGameState(); return;
            }
            if (player.getMoney() < 0) {
                gameState.setLatestMessage("🚨 Đang NỢ tiền! Cầm Cố đất đai để trả nợ trước khi qua lượt!");
                broadcastGameState(); return;
            }
            gameState.setLatestMessage("⏩ " + player.getName() + " kết thúc lượt!");
            nextTurn();
            broadcastGameState();
            return;
        }

        if ("ROLL_DICE".equals(payload)) {
            if (!isMyTurn || gameState.isHasRolledThisTurn()) return;
            if (player.getMoney() < 0) return;

            int dice1 = (int) (Math.random() * 6) + 1;
            int dice2 = (int) (Math.random() * 6) + 1;
            int total = dice1 + dice2;

            gameState.setDice1(dice1); gameState.setDice2(dice2);
            gameState.setHasRolledThisTurn(true); 

            if (player.isInJail()) {
                if (dice1 == dice2) {
                    player.setInJail(false); 
                    gameState.setHasRolledThisTurn(false); 
                    gameState.setLatestMessage("🔓 " + player.getName() + " đổ ĐÔI " + dice1 + ", hối lộ ra tù thành công!");
                    broadcastGameState();
                } else {
                    gameState.setLatestMessage("🔒 " + player.getName() + " đổ " + total + " đéo ra đôi. Nằm Tù tiếp!");
                    broadcastGameState();
                }
                return; 
            }

            if (dice1 == dice2) {
                gameState.setLatestMessage("🎲 " + player.getName() + " đổ ĐÔI " + dice1 + "! Chọn chiến thuật đi cưng!");
                broadcastGameState();
                // KÍCH HOẠT MODAL CHỌN ĐI NỬA BƯỚC HAY FULL BƯỚC
                session.sendMessage(new TextMessage("ASK_DOUBLE:" + dice1));
                return;
            }
            processMovement(player, total, "🎲 " + player.getName() + " đổ " + total + " nút.");
        }
        
        else if (payload.startsWith("MOVE_DOUBLE:")) {
            if (!isMyTurn) return;
            int steps = Integer.parseInt(payload.split(":")[1]);
            gameState.setHasRolledThisTurn(false); // Cho đổ thêm 1 phát nữa vì hồi nãy ra đôi
            processMovement(player, steps, "🏃‍♂️ " + player.getName() + " lướt " + steps + " ô! (Được đổ thêm 1 lượt)");
        }
        
        else if (payload.startsWith("MANUAL_BUY:")) {
            if (!isMyTurn || player.getMoney() < 0 || player.isInJail()) return;
            int cellId = Integer.parseInt(payload.split(":")[1]);
            int price = getPropertyPrice(cellId);
            Property prop = gameState.getProperties().get(cellId);
            if (prop == null && player.getPosition() == cellId && player.getMoney() >= price) {
                player.setMoney(player.getMoney() - price); 
                Property newProp = new Property(cellId);
                newProp.setOwnerSessionId(player.getSessionId());
                gameState.getProperties().put(cellId, newProp); 
                gameState.setLatestMessage("🎉 " + player.getName() + " hốt lô " + cellId + "!");
                broadcastGameState();
            }
        }
        
        else if (payload.startsWith("BUILD_HOUSE:")) {
            if (!isMyTurn || player.getMoney() < 0 || player.isInJail()) return;
            int cellId = Integer.parseInt(payload.split(":")[1]);
            Property prop = gameState.getProperties().get(cellId);
            if (prop != null && prop.getOwnerSessionId().equals(player.getSessionId()) && !prop.isMortgaged()) {
                if (player.getPosition() == cellId && player.getLastBuiltPosition() != cellId) {
                    int buildCost = (int) (getPropertyPrice(cellId) * 0.5); 
                    if (prop.getHouses() < 4 && player.getMoney() >= buildCost) {
                        player.setMoney(player.getMoney() - buildCost); 
                        prop.setHouses(prop.getHouses() + 1); 
                        player.setLastBuiltPosition(cellId); 
                        gameState.setLatestMessage("🏗️ " + player.getName() + " cất nhà lô " + cellId);
                        broadcastGameState();
                    }
                }
            }
        }

        else if (payload.startsWith("MORTGAGE:")) {
            if (!isMyTurn) return;
            int cellId = Integer.parseInt(payload.split(":")[1]);
            Property prop = gameState.getProperties().get(cellId);
            if (prop != null && prop.getOwnerSessionId().equals(player.getSessionId()) && !prop.isMortgaged()) {
                int mortgageVal = (int) (getPropertyPrice(cellId) * 0.5);
                int h = prop.getHouses();
                if (h == 1) mortgageVal += 100; else if (h == 2) mortgageVal += 200; else if (h == 3) mortgageVal += 300; else if (h == 4) mortgageVal += 500; 
                player.setMoney(player.getMoney() + mortgageVal); 
                prop.setMortgaged(true); 
                gameState.setLatestMessage("🏦 Đói quá! " + player.getName() + " cắm Bank lô " + cellId + " lấy " + mortgageVal + "$!");
                broadcastGameState();
            }
        }

        else if (payload.startsWith("REDEEM:")) {
            if (!isMyTurn) return;
            int cellId = Integer.parseInt(payload.split(":")[1]);
            Property prop = gameState.getProperties().get(cellId);
            if (prop != null && prop.getOwnerSessionId().equals(player.getSessionId()) && prop.isMortgaged() && player.getPosition() == cellId) {
                int redeemVal = (int) (getPropertyPrice(cellId) * 0.5);
                int h = prop.getHouses();
                if (h == 1) redeemVal += 100; else if (h == 2) redeemVal += 200; else if (h == 3) redeemVal += 300; else if (h == 4) redeemVal += 500;
                if (player.getMoney() >= redeemVal) {
                    player.setMoney(player.getMoney() - redeemVal);
                    prop.setMortgaged(false); 
                    gameState.setLatestMessage("✨ " + player.getName() + " bung " + redeemVal + "$ chuộc lô " + cellId + "!");
                    broadcastGameState();
                }
            }
        }

        else if (payload.startsWith("FORECLOSE:")) {
            if (!isMyTurn) return;
            int cellId = Integer.parseInt(payload.split(":")[1]);
            Property prop = gameState.getProperties().get(cellId);
            if (prop != null && prop.isMortgaged() && !prop.getOwnerSessionId().equals(player.getSessionId()) && player.getPosition() == cellId) {
                int basePrice = getPropertyPrice(cellId);
                int buildCost = (int) (basePrice * 0.5);
                int foreclosePrice = basePrice + (prop.getHouses() * buildCost);
                if (player.getMoney() >= foreclosePrice) {
                    player.setMoney(player.getMoney() - foreclosePrice);
                    prop.setOwnerSessionId(player.getSessionId()); 
                    prop.setMortgaged(false); 
                    gameState.setLatestMessage("🦈 CÁ MẬP! " + player.getName() + " đập " + foreclosePrice + "$ thâu tóm lô " + cellId + "!");
                    broadcastGameState();
                }
            }
        }
    }

    private void broadcastGameState() {
        try {
            String jsonMessage = objectMapper.writeValueAsString(gameState);
            TextMessage textMessage = new TextMessage(jsonMessage);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
