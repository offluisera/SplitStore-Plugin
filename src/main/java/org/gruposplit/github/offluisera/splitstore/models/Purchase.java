package org.gruposplit.github.offluisera.splitstore.models;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================
 * MODELO: PURCHASE (COMPRA)
 * ============================================
 * Representa uma compra/pedido pendente de entrega
 */
public class Purchase {

    private final int id;
    private final String productName;
    private final double amount;
    private final String playerUUID;
    private final String playerName;
    private final List<String> commands;
    private final long createdAt;
    private final String status;

    private Purchase(Builder builder) {
        this.id = builder.id;
        this.productName = builder.productName;
        this.amount = builder.amount;
        this.playerUUID = builder.playerUUID;
        this.playerName = builder.playerName;
        this.commands = builder.commands;
        this.createdAt = builder.createdAt;
        this.status = builder.status;
    }

    /**
     * Cria uma Purchase a partir de um JsonObject
     */
    public static Purchase fromJson(JsonObject json) {
        Builder builder = new Builder();

        if (json.has("id")) {
            builder.setId(json.get("id").getAsInt());
        }

        if (json.has("product_name")) {
            builder.setProductName(json.get("product_name").getAsString());
        }

        if (json.has("amount")) {
            builder.setAmount(json.get("amount").getAsDouble());
        }

        if (json.has("player_uuid")) {
            builder.setPlayerUUID(json.get("player_uuid").getAsString());
        }

        if (json.has("player_name")) {
            builder.setPlayerName(json.get("player_name").getAsString());
        }

        if (json.has("commands")) {
            String commandsStr = json.get("commands").getAsString();
            List<String> commandList = new ArrayList<>();

            // Divide por quebra de linha
            if (commandsStr != null && !commandsStr.isEmpty()) {
                String[] lines = commandsStr.split("\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        commandList.add(line);
                    }
                }
            }

            builder.setCommands(commandList);
        }

        if (json.has("created_at")) {
            builder.setCreatedAt(json.get("created_at").getAsLong());
        }

        if (json.has("status")) {
            builder.setStatus(json.get("status").getAsString());
        }

        return builder.build();
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public double getAmount() {
        return amount;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands); // Retorna cópia para segurança
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", amount=" + amount +
                ", playerUUID='" + playerUUID + '\'' +
                ", playerName='" + playerName + '\'' +
                ", commandsCount=" + commands.size() +
                ", status='" + status + '\'' +
                '}';
    }

    /**
     * ============================================
     * BUILDER PATTERN
     * ============================================
     */
    public static class Builder {
        private int id;
        private String productName;
        private double amount;
        private String playerUUID;
        private String playerName;
        private List<String> commands = new ArrayList<>();
        private long createdAt;
        private String status = "pending";

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setProductName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder setPlayerUUID(String playerUUID) {
            this.playerUUID = playerUUID;
            return this;
        }

        public Builder setPlayerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder setCommands(List<String> commands) {
            this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
            return this;
        }

        public Builder addCommand(String command) {
            if (command != null && !command.trim().isEmpty()) {
                this.commands.add(command.trim());
            }
            return this;
        }

        public Builder setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Purchase build() {
            return new Purchase(this);
        }
    }
}