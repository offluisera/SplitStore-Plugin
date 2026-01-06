package org.gruposplit.github.offluisera.splitstore.listerner;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.gruposplit.github.offluisera.splitstore.SplitStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Listener para eventos de jogadores
 */
public class PlayerListener implements Listener {

    private final SplitStore plugin;

    public PlayerListener(SplitStore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Verificar compras pendentes de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            checkPendingPurchases(player);
        });

        // Notificar admins sobre status da conexão
        if (player.hasPermission("splitstore.admin")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.isConnected()) {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "SplitStore: " + ChatColor.WHITE + "Sistema online");
                    player.sendMessage("");
                } else {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.RED + "✗ " + ChatColor.GRAY + "SplitStore: " + ChatColor.WHITE + "Sistema offline - Verifique configurações");
                    player.sendMessage("");
                }
            }, 40L); // 2 segundos após join
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Notificar sistema sobre logout (assíncrono)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            notifyPlayerLogout(player);
        });
    }

    /**
     * Verifica se há compras pendentes para entregar ao jogador
     */
    private void checkPendingPurchases(Player player) {
        if (!plugin.isConnected()) {
            return;
        }

        try {
            URL url = new URL(plugin.getApiUrl() + "/plugin/purchases/pending");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-API-Key", plugin.getApiKey());
            conn.setRequestProperty("X-API-Secret", plugin.getApiSecret());
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String jsonInput = String.format(
                    "{\"player_uuid\":\"%s\",\"player_name\":\"%s\"}",
                    player.getUniqueId().toString(),
                    player.getName()
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Processar compras pendentes
                processPurchases(player, response.toString());
            }

            conn.disconnect();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao verificar compras pendentes para " + player.getName(), e);
        }
    }

    /**
     * Processa e entrega as compras ao jogador
     */
    private void processPurchases(Player player, String jsonResponse) {
        // Aqui você implementaria a lógica para parsear o JSON e entregar os itens
        // Por simplicidade, apenas logamos
        if (jsonResponse.contains("\"purchases\":[]")) {
            return; // Sem compras pendentes
        }

        // Notificar jogador na thread principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Você tem " + ChatColor.WHITE + "compras pendentes" + ChatColor.GRAY + " para receber!");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/splitstore claim" + ChatColor.GRAY + " para coletar");
            player.sendMessage("");
        });

        plugin.getLogger().info("Compras pendentes encontradas para: " + player.getName());
    }

    /**
     * Notifica o sistema sobre logout do jogador
     */
    private void notifyPlayerLogout(Player player) {
        if (!plugin.isConnected()) {
            return;
        }

        try {
            URL url = new URL(plugin.getApiUrl() + "/plugin/player/logout");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-API-Key", plugin.getApiKey());
            conn.setRequestProperty("X-API-Secret", plugin.getApiSecret());
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            String jsonInput = String.format(
                    "{\"player_uuid\":\"%s\",\"player_name\":\"%s\",\"timestamp\":%d}",
                    player.getUniqueId().toString(),
                    player.getName(),
                    System.currentTimeMillis()
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            conn.getResponseCode(); // Apenas para garantir que a requisição foi enviada
            conn.disconnect();

        } catch (Exception e) {
            // Silenciosamente falhar - não é crítico
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().log(Level.FINE, "Erro ao notificar logout de " + player.getName(), e);
            }
        }
    }
}