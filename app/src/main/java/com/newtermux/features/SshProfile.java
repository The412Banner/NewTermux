package com.newtermux.features;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class SshProfile {
    public String id;
    public String nickname;
    public String host;
    public int port;
    public String username;
    public String keyPath; // empty = password auth (user types it)

    // Port forwarding / tunnel
    public boolean tunnelEnabled;
    public String tunnelType;       // "local" (-L) or "remote" (-R)
    public int tunnelLocalPort;
    public String tunnelRemoteHost;
    public int tunnelRemotePort;

    public SshProfile() {
        id = UUID.randomUUID().toString();
        port = 22;
        keyPath = "";
        tunnelType = "local";
        tunnelLocalPort = 8080;
        tunnelRemoteHost = "localhost";
        tunnelRemotePort = 8080;
    }

    public String buildCommand() {
        StringBuilder cmd = new StringBuilder("ssh");
        cmd.append(" -o StrictHostKeyChecking=accept-new");
        if (tunnelEnabled && tunnelRemoteHost != null && !tunnelRemoteHost.isEmpty()) {
            String flag = "remote".equals(tunnelType) ? "-R" : "-L";
            cmd.append(" ").append(flag).append(" ")
               .append(tunnelLocalPort).append(":").append(tunnelRemoteHost)
               .append(":").append(tunnelRemotePort);
        }
        if (port != 22) cmd.append(" -p ").append(port);
        if (keyPath != null && !keyPath.isEmpty()) cmd.append(" -i ").append(keyPath);
        cmd.append(" ").append(username).append("@").append(host);
        return cmd.toString();
    }

    public String displayLabel() {
        String base = username + "@" + host;
        return port != 22 ? base + ":" + port : base;
    }

    public String tunnelLabel() {
        if (!tunnelEnabled) return null;
        String arrow = "remote".equals(tunnelType) ? "R" : "L";
        return "-" + arrow + " " + tunnelLocalPort + ":" + tunnelRemoteHost + ":" + tunnelRemotePort;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("nickname", nickname != null ? nickname : "");
        o.put("host", host != null ? host : "");
        o.put("port", port);
        o.put("username", username != null ? username : "");
        o.put("keyPath", keyPath != null ? keyPath : "");
        o.put("tunnelEnabled", tunnelEnabled);
        o.put("tunnelType", tunnelType != null ? tunnelType : "local");
        o.put("tunnelLocalPort", tunnelLocalPort);
        o.put("tunnelRemoteHost", tunnelRemoteHost != null ? tunnelRemoteHost : "localhost");
        o.put("tunnelRemotePort", tunnelRemotePort);
        return o;
    }

    public static SshProfile fromJson(JSONObject o) throws JSONException {
        SshProfile p = new SshProfile();
        p.id = o.optString("id", UUID.randomUUID().toString());
        p.nickname = o.optString("nickname", "");
        p.host = o.optString("host", "");
        p.port = o.optInt("port", 22);
        p.username = o.optString("username", "");
        p.keyPath = o.optString("keyPath", "");
        p.tunnelEnabled = o.optBoolean("tunnelEnabled", false);
        p.tunnelType = o.optString("tunnelType", "local");
        p.tunnelLocalPort = o.optInt("tunnelLocalPort", 8080);
        p.tunnelRemoteHost = o.optString("tunnelRemoteHost", "localhost");
        p.tunnelRemotePort = o.optInt("tunnelRemotePort", 8080);
        return p;
    }
}
