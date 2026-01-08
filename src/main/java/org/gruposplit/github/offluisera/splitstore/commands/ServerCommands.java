package org.gruposplit.github.offluisera.splitstore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.gruposplit.github.offluisera.splitstore.SplitStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Level;

/**
 * ============================================
 * COMANDOS DE SERVIDOR
 * ============================================
 * genserver - Gera Server ID único
 * verify - Verifica servidor com código
 */
public class ServerCommands {

    private final SplitStore plugin;

    public ServerCommands(SplitStore plugin) {
        this.plugin = plugin;
    }

    /**
     * ============================================
     * GERAR SERVER ID
     * ============================================
     */
    public void handleGenServer(CommandSender sender, String[] args) {

        if (!sender.hasPermission("splitstore.admin")) {
            sender.sendMessage(ChatColor.RED + "✗ Você não tem permissão para usar este comando!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "✗ Uso: /splitstore genserver <nome>");
            sender.sendMessage(ChatColor.GRAY + "Exemplo: /splitstore genserver Survival");
            return;
        }

        String serverName = args[1];

        // Valida nome do servidor
        if (!serverName.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            sender.sendMessage(ChatColor.RED + "✗ Nome inválido!");
            sender.sendMessage(ChatColor.GRAY + "Use apenas letras, números, _ e - (3-20 caracteres)");
            return;
        }

        try {
            // Gera Server ID único
            String serverId = generateServerId(serverName);

            // Exibe resultado formatado
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "════════════════════════════════════");
            sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ SERVER ID GERADO COM SUCESSO!");
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "════════════════════════════════════");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Nome do Servidor: " + ChatColor.WHITE + serverName);
            sender.sendMessage(ChatColor.YELLOW + "Server ID: " + ChatColor.AQUA + ChatColor.BOLD + serverId);
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "PRÓXIMOS PASSOS:");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.WHITE + "1. " + ChatColor.GRAY + "Acesse o painel web:");
            sender.sendMessage(ChatColor.AQUA + "   https://splitstore.com.br/client/servers.php");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.WHITE + "2. " + ChatColor.GRAY + "Clique em " + ChatColor.RED + "'Novo Servidor'");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.WHITE + "3. " + ChatColor.GRAY + "Cole este Server ID no formulário");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.WHITE + "4. " + ChatColor.GRAY + "Após adicionar, você receberá um " + ChatColor.YELLOW + "código");
            sender.sendMessage(ChatColor.GRAY + "   de verificação para executar aqui");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "════════════════════════════════════");
            sender.sendMessage("");

            // Salva no config para referência
            plugin.getConfig().set("last-generated-server-id", serverId);
            plugin.getConfig().set("last-generated-server-name", serverName);
            plugin.getConfig().set("last-generated-at", System.currentTimeMillis());
            plugin.saveConfig();

            plugin.getLogger().info("Server ID gerado: " + serverId + " para: " + serverName);

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✗ Erro ao gerar Server ID!");
            plugin.getLogger().log(Level.SEVERE, "Erro ao gerar server ID:", e);
        }
    }

    /**
     * ============================================
     * VERIFICAR SERVIDOR
     * ============================================
     */
    public void handleVerify(CommandSender sender, String[] args) {

        if (!sender.hasPermission("splitstore.admin")) {
            sender.sendMessage(ChatColor.RED + "✗ Você não tem permissão!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "✗ Uso: /splitstore verify <código>");
            sender.sendMessage(ChatColor.GRAY + "Cole o código recebido no painel web");
            return;
        }

        String verificationCode = args[1];

        sender.sendMessage(ChatColor.YELLOW + "⏳ Verificando servidor com SplitStore...");

        // Executar verificação de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success = verifyServer(verificationCode);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "════════════════════════════════════");
                        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ SERVIDOR VERIFICADO COM SUCESSO!");
                        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "════════════════════════════════════");
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.WHITE + "Seu servidor está agora conectado ao SplitStore!");
                        sender.sendMessage(ChatColor.GRAY + "Compras serão entregues automaticamente.");
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "════════════════════════════════════");
                        sender.sendMessage("");

                        // Salva status verificado
                        plugin.getConfig().set("server-verified", true);
                        plugin.getConfig().set("verified-at", System.currentTimeMillis());
                        plugin.saveConfig();

                    } else {
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.RED + "✗ Falha na verificação!");
                        sender.sendMessage(ChatColor.GRAY + "Verifique se o código está correto.");
                        sender.sendMessage("");
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "✗ Erro ao verificar servidor!");
                    plugin.getLogger().log(Level.SEVERE, "Erro na verificação:", e);
                });
            }
        });
    }

    /**
     * ============================================
     * GERA UM SERVER ID ÚNICO
     * ============================================
     */
    private String generateServerId(String serverName) {
        try {
            // Combina nome + timestamp + random para garantir unicidade
            String base = serverName + "_" + System.currentTimeMillis() + "_" + Math.random();

            // Gera hash SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(base.getBytes(StandardCharsets.UTF_8));

            // Converte para hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // Retorna formato: SV_[hash32chars]_[nome]
            String shortHash = hexString.toString().substring(0, 32);
            return "SV_" + shortHash + "_" + serverName;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao gerar server ID:", e);
            // Fallback para método simples
            return "SV_" + serverName + "_" + System.currentTimeMillis();
        }
    }

    /**
     * ============================================
     * VERIFICA O SERVIDOR COM O CÓDIGO
     * ============================================
     */
    private boolean verifyServer(String verificationCode) {
        try {
            String apiUrl = plugin.getApiUrl();
            String apiKey = plugin.getApiKey();
            String apiSecret = plugin.getApiSecret();

            URL url = new URL(apiUrl + "/plugin/server/verify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-API-Key", apiKey);
            conn.setRequestProperty("X-API-Secret", apiSecret);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Dados para enviar
            String jsonInput = String.format(
                    "{\"verification_code\":\"%s\",\"server_version\":\"%s\",\"plugin_version\":\"1.0.0\",\"online_players\":%d,\"max_players\":%d}",
                    verificationCode,
                    Bukkit.getVersion(),
                    Bukkit.getOnlinePlayers().size(),
                    Bukkit.getMaxPlayers()
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                // Lê resposta
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                plugin.getLogger().info("Servidor verificado com sucesso!");
                plugin.getLogger().info("Resposta: " + response.toString());

                return true;
            } else {
                plugin.getLogger().warning("Falha na verificação. Código: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao verificar servidor:", e);
            return false;
        }
    }
}