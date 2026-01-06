package org.gruposplit.github.offluisera.splitstore;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.gruposplit.github.offluisera.splitstore.listerner.PlayerListener;
import org.gruposplit.github.offluisera.splitstore.managers.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * ============================================
 * SPLITSTORE - PLUGIN MINECRAFT
 * ============================================
 * Compatível: Minecraft 1.8 - 1.20
 * Autor: SplitStore Team
 * Versão: 1.0.0
 */
public class SplitStore extends JavaPlugin {

    private String apiKey;
    private String apiSecret;
    private String apiUrl;
    private boolean isConnected = false;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Banner de inicialização
        sendConsoleBanner();

        // Carregar configuração
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Carregar credenciais
        apiKey = getConfig().getString("api-key", "");
        apiSecret = getConfig().getString("api-secret", "");
        apiUrl = getConfig().getString("api-url", "https://splitstore.com.br/api");

        // Validar credenciais
        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            getLogger().severe("═══════════════════════════════════════════");
            getLogger().severe("⚠ CREDENCIAIS NÃO CONFIGURADAS!");
            getLogger().severe("Configure api-key e api-secret no config.yml");
            getLogger().severe("═══════════════════════════════════════════");
            return;
        }

        // Testar conexão
        testConnection();

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("✓ SplitStore Plugin carregado com sucesso!");
        getLogger().info("═══════════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("✓ SplitStore Plugin descarregado!");
        getLogger().info("═══════════════════════════════════════════");
    }

    private void sendConsoleBanner() {
        getLogger().info(" ");
        getLogger().info("§c███████╗██████╗ ██╗     ██╗████████╗");
        getLogger().info("§c██╔════╝██╔══██╗██║     ██║╚══██╔══╝");
        getLogger().info("§c███████╗██████╔╝██║     ██║   ██║   ");
        getLogger().info("§c╚════██║██╔═══╝ ██║     ██║   ██║   ");
        getLogger().info("§c███████║██║     ███████╗██║   ██║   ");
        getLogger().info("§c╚══════╝╚═╝     ╚══════╝╚═╝   ╚═╝   ");
        getLogger().info(" ");
        getLogger().info("§eSplitStore Plugin v1.0.0");
        getLogger().info("§7Minecraft 1.8 - 1.20");
        getLogger().info(" ");
    }

    private void testConnection() {
        getLogger().info("Testando conexão com SplitStore API...");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(apiUrl + "/plugin/verify");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setRequestProperty("X-API-Secret", apiSecret);
                conn.setDoOutput(true);

                String jsonInput = String.format(
                        "{\"server\":\"minecraft\",\"version\":\"%s\"}",
                        Bukkit.getVersion()
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    isConnected = true;
                    getLogger().info("═══════════════════════════════════════════");
                    getLogger().info("§a✓ Conexão estabelecida com sucesso!");
                    getLogger().info("§7Status: ONLINE");
                    getLogger().info("═══════════════════════════════════════════");
                } else {
                    getLogger().warning("═══════════════════════════════════════════");
                    getLogger().warning("§c⚠ Falha na conexão (Code: " + responseCode + ")");
                    getLogger().warning("§7Verifique suas credenciais");
                    getLogger().warning("═══════════════════════════════════════════");
                }

                conn.disconnect();

            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Erro ao testar conexão:", e);
                getLogger().warning("═══════════════════════════════════════════");
                getLogger().warning("§c⚠ Erro ao conectar com SplitStore");
                getLogger().warning("§7Verifique sua conexão com a internet");
                getLogger().warning("═══════════════════════════════════════════");
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("splitstore")) {

            if (args.length == 0) {
                sendHelpMessage(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "status":
                    sendStatusMessage(sender);
                    break;

                case "reload":
                    if (!sender.hasPermission("splitstore.admin")) {
                        sender.sendMessage(ChatColor.RED + "Você não tem permissão!");
                        return true;
                    }
                    reloadConfig();
                    apiKey = getConfig().getString("api-key", "");
                    apiSecret = getConfig().getString("api-secret", "");
                    apiUrl = getConfig().getString("api-url", "https://splitstore.com.br/api");
                    testConnection();
                    sender.sendMessage(ChatColor.GREEN + "✓ Configuração recarregada!");
                    break;

                case "test":
                    if (!sender.hasPermission("splitstore.admin")) {
                        sender.sendMessage(ChatColor.RED + "Você não tem permissão!");
                        return true;
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Testando conexão...");
                    testConnection();
                    break;

                case "info":
                    sendInfoMessage(sender);
                    break;

                default:
                    sendHelpMessage(sender);
                    break;
            }

            return true;
        }
        return false;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "SPLITSTORE" + ChatColor.GRAY + " - Comandos");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.WHITE + "/splitstore status " + ChatColor.GRAY + "- Ver status da conexão");
        sender.sendMessage(ChatColor.WHITE + "/splitstore info " + ChatColor.GRAY + "- Informações do plugin");
        sender.sendMessage(ChatColor.WHITE + "/splitstore reload " + ChatColor.GRAY + "- Recarregar config");
        sender.sendMessage(ChatColor.WHITE + "/splitstore test " + ChatColor.GRAY + "- Testar conexão");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
    }

    private void sendStatusMessage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "SPLITSTORE" + ChatColor.GRAY + " - Status");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.WHITE + "Conexão: " + (isConnected ? ChatColor.GREEN + "ONLINE ✓" : ChatColor.RED + "OFFLINE ✗"));
        sender.sendMessage(ChatColor.WHITE + "API URL: " + ChatColor.GRAY + apiUrl);
        sender.sendMessage(ChatColor.WHITE + "API Key: " + ChatColor.GRAY + (apiKey.isEmpty() ? "Não configurada" : apiKey.substring(0, Math.min(10, apiKey.length())) + "..."));
        sender.sendMessage(ChatColor.WHITE + "Versão: " + ChatColor.GRAY + "1.0.0");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
    }

    private void sendInfoMessage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "SPLITSTORE" + ChatColor.GRAY + " - Informações");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.WHITE + "Plugin: " + ChatColor.GRAY + "SplitStore v1.0.0");
        sender.sendMessage(ChatColor.WHITE + "Servidor: " + ChatColor.GRAY + Bukkit.getVersion());
        sender.sendMessage(ChatColor.WHITE + "Players: " + ChatColor.GRAY + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sender.sendMessage(ChatColor.WHITE + "Compatibilidade: " + ChatColor.GRAY + "MC 1.8 - 1.20");
        sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
    }

    // Getters
    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public boolean isConnected() {
        return isConnected;
    }
}