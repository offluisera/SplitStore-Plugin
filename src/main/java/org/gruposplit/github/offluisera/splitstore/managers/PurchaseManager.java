package org.gruposplit.github.offluisera.splitstore.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.gruposplit.github.offluisera.splitstore.SplitStore;
import org.gruposplit.github.offluisera.splitstore.api.APIClient;
import org.gruposplit.github.offluisera.splitstore.models.Purchase;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador de compras pendentes
 */
public class PurchaseManager {

    private final SplitStore plugin;
    private final APIClient apiClient;

    public PurchaseManager(SplitStore plugin) {
        try {
            this.plugin = plugin;

            plugin.getLogger().info("Criando APIClient...");
            plugin.getLogger().info("API URL: " + plugin.getApiUrl());
            plugin.getLogger().info("API Key configurada: " + (!plugin.getApiKey().isEmpty()));

            this.apiClient = new APIClient(
                    plugin.getApiUrl(),
                    plugin.getApiKey(),
                    plugin.getApiSecret(),
                    plugin.getLogger()
            );

            plugin.getLogger().info("PurchaseManager construtor completo!");

        } catch (Exception e) {
            plugin.getLogger().severe("ERRO ao criar PurchaseManager:");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Resgata todas as compras pendentes de um jogador
     */
    public void claimPurchases(Player player) {
        try {
            plugin.getLogger().info("[CLAIM] Iniciando claim para: " + player.getName());
            plugin.getLogger().info("[CLAIM] UUID: " + player.getUniqueId());

            if (!plugin.isConnected()) {
                plugin.getLogger().warning("[CLAIM] Sistema não conectado!");
                player.sendMessage(ChatColor.RED + "✗ Sistema offline. Tente novamente mais tarde.");
                return;
            }

            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "⏳ Buscando compras pendentes...");
            player.sendMessage("");

            plugin.getLogger().info("[CLAIM] Agendando tarefa assíncrona...");

            // Executar de forma assíncrona
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getLogger().info("[CLAIM] Executando busca na API...");

                    // Buscar compras pendentes
                    APIClient.APIResponse response = apiClient.getPendingPurchases(
                            player.getUniqueId().toString(),
                            player.getName()
                    );

                    plugin.getLogger().info("[CLAIM] Resposta recebida: " + response.isSuccess());
                    plugin.getLogger().info("[CLAIM] Status code: " + response.getStatusCode());

                    if (!response.isSuccess()) {
                        plugin.getLogger().warning("[CLAIM] Erro na API: " + response.getErrorMessage());
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(ChatColor.RED + "✗ Erro ao buscar compras: " + response.getErrorMessage());
                        });
                        return;
                    }

                    // Processar resposta
                    JsonObject data = response.getJsonResponse();
                    int count = data.get("count").getAsInt();

                    plugin.getLogger().info("[CLAIM] Total de compras: " + count);

                    if (count == 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage("");
                            player.sendMessage(ChatColor.YELLOW + "⚠ Você não possui compras pendentes.");
                            player.sendMessage("");
                        });
                        return;
                    }

                    // Obter array de compras
                    JsonArray purchases = data.getAsJsonArray("purchases");
                    List<Purchase> purchaseList = new ArrayList<>();

                    for (int i = 0; i < purchases.size(); i++) {
                        JsonObject purchase = purchases.get(i).getAsJsonObject();
                        purchaseList.add(Purchase.fromJson(purchase));
                    }

                    plugin.getLogger().info("[CLAIM] Compras processadas: " + purchaseList.size());

                    // Processar cada compra na thread principal
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        int delivered = 0;
                        int failed = 0;

                        for (Purchase purchase : purchaseList) {
                            plugin.getLogger().info("[CLAIM] Entregando compra: " + purchase.getId());
                            if (deliverPurchase(player, purchase)) {
                                delivered++;
                            } else {
                                failed++;
                            }
                        }

                        // Feedback final
                        player.sendMessage("");
                        player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ RESGATE CONCLUÍDO!");
                        player.sendMessage("");
                        player.sendMessage(ChatColor.WHITE + "Entregues: " + ChatColor.GREEN + delivered);
                        if (failed > 0) {
                            player.sendMessage(ChatColor.WHITE + "Falharam: " + ChatColor.RED + failed);
                        }
                        player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("");

                        plugin.getLogger().info("[CLAIM] Resgate concluído! Sucesso: " + delivered + ", Falhas: " + failed);
                    });

                } catch (Exception e) {
                    plugin.getLogger().severe("[CLAIM] Erro ao processar compras de " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.RED + "✗ Erro ao processar compras. Contate a administração.");
                    });
                }
            });

        } catch (Exception e) {
            plugin.getLogger().severe("[CLAIM] Erro crítico: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "✗ Erro ao iniciar resgate.");
        }
    }

    /**
     * Entrega uma compra específica ao jogador
     */
    private boolean deliverPurchase(Player player, Purchase purchase) {
        try {
            plugin.getLogger().info("[DELIVERY] Entregando: " + purchase.getProductName() + " para " + player.getName());

            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "📦 " + purchase.getProductName());
            player.sendMessage(ChatColor.GRAY + "ID: #" + purchase.getId());
            player.sendMessage("");

            // Executar comandos
            List<String> commands = purchase.getCommands();
            int executed = 0;

            plugin.getLogger().info("[DELIVERY] Total de comandos: " + commands.size());

            for (String command : commands) {
                // Substituir placeholders
                command = command
                        .replace("{player}", player.getName())
                        .replace("{uuid}", player.getUniqueId().toString())
                        .replace("{amount}", String.valueOf(purchase.getAmount()));

                // Remover '/' se existir
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                // Executar comando
                try {
                    plugin.getLogger().info("[DELIVERY] Executando: " + command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    executed++;
                } catch (Exception e) {
                    plugin.getLogger().warning("[DELIVERY] Erro ao executar comando: " + command);
                    plugin.getLogger().warning("[DELIVERY] Erro: " + e.getMessage());
                }
            }

            player.sendMessage(ChatColor.GREEN + "✓ " + executed + " comando(s) executado(s)");
            player.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            plugin.getLogger().info("[DELIVERY] Comandos executados: " + executed + "/" + commands.size());

            // Confirmar entrega na API (assíncrono)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getLogger().info("[DELIVERY] Confirmando entrega na API...");

                    APIClient.APIResponse confirmResponse = apiClient.confirmDelivery(
                            String.valueOf(purchase.getId()),
                            player.getUniqueId().toString()
                    );

                    if (confirmResponse.isSuccess()) {
                        plugin.getLogger().info("[DELIVERY] Entrega confirmada com sucesso!");
                    } else {
                        plugin.getLogger().warning("[DELIVERY] Falha ao confirmar entrega #" + purchase.getId() + ": " + confirmResponse.getErrorMessage());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[DELIVERY] Erro ao confirmar entrega: " + e.getMessage());
                }
            });

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("[DELIVERY] Erro ao entregar compra #" + purchase.getId() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "✗ Erro ao entregar: " + purchase.getProductName());
            return false;
        }
    }
}