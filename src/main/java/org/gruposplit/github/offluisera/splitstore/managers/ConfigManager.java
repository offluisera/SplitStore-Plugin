package org.gruposplit.github.offluisera.splitstore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.gruposplit.github.offluisera.splitstore.SplitStore;

/**
 * Gerenciador de configurações do plugin
 */
public class ConfigManager {

    private final SplitStore plugin;

    public ConfigManager(SplitStore plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();

        FileConfiguration config = plugin.getConfig();

        // Valores padrão
        config.addDefault("api-key", "");
        config.addDefault("api-secret", "");
        config.addDefault("api-url", "https://splitstore.com.br/api");
        config.addDefault("debug-mode", false);
        config.addDefault("auto-sync", true);
        config.addDefault("sync-interval", 300); // 5 minutos em segundos

        // Mensagens personalizáveis
        config.addDefault("messages.purchase-success", "&a✓ Compra aprovada! &7Aproveite seu item.");
        config.addDefault("messages.purchase-pending", "&e⏳ Processando compra... &7Aguarde.");
        config.addDefault("messages.purchase-failed", "&c✗ Erro ao processar compra. &7Contate o suporte.");
        config.addDefault("messages.insufficient-funds", "&c✗ Saldo insuficiente para esta compra.");
        config.addDefault("messages.player-not-found", "&c✗ Jogador não encontrado.");
        config.addDefault("messages.connection-error", "&c✗ Erro de conexão com SplitStore.");

        config.options().copyDefaults(true);
        plugin.saveConfig();

        plugin.getLogger().info("✓ Configuração carregada com sucesso!");
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "&cMensagem não configurada: " + key)
                .replace("&", "§");
    }

    public boolean isDebugMode() {
        return plugin.getConfig().getBoolean("debug-mode", false);
    }

    public boolean isAutoSync() {
        return plugin.getConfig().getBoolean("auto-sync", true);
    }

    public int getSyncInterval() {
        return plugin.getConfig().getInt("sync-interval", 300);
    }
}