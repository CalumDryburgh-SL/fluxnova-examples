package com.fluxnova.scottlogic.mcpclientchatbot.configuration;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot.mcp.tools")
public class McpToolAllowlistProperties {

    private List<String> allowlist = List.of();

    public List<String> getAllowlist() {
        return allowlist;
    }

    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist;
    }

    public Set<String> allowedToolNames() {
        if (allowlist == null) {
            return Set.of();
        }
        return allowlist.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
