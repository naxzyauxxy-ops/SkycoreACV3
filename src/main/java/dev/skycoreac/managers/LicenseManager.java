package dev.skycoreac.managers;

import dev.skycoreac.SkyCoreAC;
import org.bukkit.Bukkit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;

public class LicenseManager {

    private static final String SECRET = "EclipseAC-Private-Secret-2025-ChangeMe";

    private final SkyCoreAC plugin;
    private boolean valid = false;

    public LicenseManager(SkyCoreAC plugin) {
        this.plugin = plugin;
    }

    public boolean validate() {
        String key = plugin.getConfig().getString("license.key", "");

        if (key.isEmpty() || key.equalsIgnoreCase("YOUR-LICENSE-KEY-HERE")) {
            printBanner(key, "No key set");
            return false;
        }

        if (!isValidKey(key)) {
            printBanner(key, "Invalid");
            return false;
        }

        String currentIp = getServerIp();
        String lockedIp  = getLockedIp(key);

        if (lockedIp == null) {
            saveLockedIp(key, currentIp);
            printBanner(key, "Valid");
            valid = true;
            return true;
        }

        if (!lockedIp.equals(currentIp)) {
            printBanner(key, "Invalid (IP mismatch)");
            return false;
        }

        printBanner(key, "Valid");
        valid = true;
        return true;
    }

    private void printBanner(String key, String status) {
        String k = key.isEmpty() ? "None" : key;
        plugin.getLogger().info("                                                          ");
        plugin.getLogger().info("  _____     _ _                     _   ___              ");
        plugin.getLogger().info(" | ____|___| (_)_ __  ___  ___     / \\ / __|             ");
        plugin.getLogger().info(" |  _| / __| | | '_ \\/ __|/ _ \\   / _ \\ C |             ");
        plugin.getLogger().info(" | |__| (__| | | |_) \\__ \\  __/  / ___ \\  _|            ");
        plugin.getLogger().info(" |_____\\___|_|_| .__/|___/\\___| /_/   \\_\\_|             ");
        plugin.getLogger().info("               |_|                                       ");
        plugin.getLogger().info("                                                          ");
        plugin.getLogger().info(" License: " + k                                          );
        plugin.getLogger().info(" Status:  " + status                                     );
        plugin.getLogger().info("                                                          ");
    }

    private boolean isValidKey(String key) {
        String[] parts = key.split("-");
        if (parts.length != 5) return false;
        if (!parts[0].equals("ECLIPSE")) return false;
        String id       = parts[1] + "-" + parts[2] + "-" + parts[3];
        String sig      = parts[4];
        String expected = hmac(id, SECRET);
        return expected.equalsIgnoreCase(sig);
    }

    private String getServerIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (java.net.InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
    }

    private File getLockFile() {
        return new File(plugin.getDataFolder(), ".license_lock");
    }

    private String getLockedIp(String key) {
        try {
            File f = getLockFile();
            if (!f.exists()) return null;
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            return p.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveLockedIp(String key, String ip) {
        try {
            plugin.getDataFolder().mkdirs();
            File f = getLockFile();
            Properties p = new Properties();
            if (f.exists()) p.load(new FileInputStream(f));
            p.setProperty(key, ip);
            p.store(new FileOutputStream(f), null);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkyCoreAC] Could not save license lock: " + e.getMessage());
        }
    }

    private static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X", raw[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC failed", e);
        }
    }

    public boolean isValid() { return valid; }
}
