package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response from the AI assistant")
public class AiChatResponseDTO {

    private String reply;
    private String source;
    private String toolUsed;
    private Object data;
    private String supportPhone;
    private String actionTab;
    private String actionLabel;
    private List<String> suggestions;

    public AiChatResponseDTO() {}

    public AiChatResponseDTO(String reply, String source, String toolUsed, Object data) {
        this.reply = reply;
        this.source = source;
        this.toolUsed = toolUsed;
        this.data = data;
    }

    public AiChatResponseDTO(String reply, String source, String toolUsed, Object data,
                             String supportPhone, String actionTab, String actionLabel, List<String> suggestions) {
        this.reply = reply;
        this.source = source;
        this.toolUsed = toolUsed;
        this.data = data;
        this.supportPhone = supportPhone;
        this.actionTab = actionTab;
        this.actionLabel = actionLabel;
        this.suggestions = suggestions;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getToolUsed() { return toolUsed; }
    public void setToolUsed(String toolUsed) { this.toolUsed = toolUsed; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public String getActionTab() { return actionTab; }
    public void setActionTab(String actionTab) { this.actionTab = actionTab; }

    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}

