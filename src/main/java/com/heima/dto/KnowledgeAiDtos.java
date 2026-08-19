package com.heima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class KnowledgeAiDtos {

    private KnowledgeAiDtos() {
    }

    @Schema(description = "综合知识 AI 辅导请求（流式）")
    public record KnowledgeTutorRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "科目 ID，作为 AgentScope userId", example = "architect")
            String subjectId,
            @Schema(description = "笔记文件名，作为 AgentScope sessionId；未保存可用草稿 key")
            String fileName,
            @Schema(description = "当前笔记标题")
            String title,
            @Schema(description = "当前笔记正文")
            String noteText,
            @Schema(description = "考生问题；空则按笔记主题做系统辅导")
            String question,
            @Schema(description = "true=同一会话连续追问，用户 prompt 从简")
            Boolean followUp
    ) {
    }
}
