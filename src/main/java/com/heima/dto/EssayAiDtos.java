package com.heima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class EssayAiDtos {

    private EssayAiDtos() {
    }

    @Schema(description = "论文润色请求")
    public record EssayPolishRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "论文题目与题目描述（首行为题目名称）")
            String topic,
            @Schema(description = "润色范围：abstract=摘要，body=正文，all=全部", example = "all",
                    allowableValues = {"abstract", "body", "all"})
            String part,
            @Schema(description = "当前摘要原文")
            String abstractText,
            @Schema(description = "当前正文原文")
            String bodyText
    ) {
    }

    @Schema(description = "论文润色响应")
    public record EssayPolishResponse(
            @Schema(description = "实际润色范围", example = "all")
            String part,
            @Schema(description = "润色后的摘要（未润色摘要时可能原样返回）")
            String abstractText,
            @Schema(description = "润色后的正文（未润色正文时可能原样返回）")
            String bodyText,
            @Schema(description = "模型原始返回文本")
            String raw) {
    }

    @Schema(description = "论文评分请求")
    public record EssayScoreRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "论文题目与题目描述")
            String topic,
            @Schema(description = "摘要")
            String abstractText,
            @Schema(description = "正文")
            String bodyText
    ) {
    }

    @Schema(description = "评分维度明细")
    public record ScoreDimension(
            @Schema(description = "维度名称", example = "切题与审题")
            String name,
            @Schema(description = "该维得分", example = "16")
            int score,
            @Schema(description = "该维满分", example = "20")
            int max,
            @Schema(description = "维度评语")
            String comment
    ) {
    }

    @Schema(description = "论文评分响应（满分 75；≥45 合格）")
    public record EssayScoreResponse(
            @Schema(description = "总分（0-75）", example = "52")
            int totalScore,
            @Schema(description = "等级：合格 或 不及格", example = "合格",
                    allowableValues = {"合格", "不及格"})
            String level,
            @Schema(description = "各评分维度")
            List<ScoreDimension> dimensions,
            @Schema(description = "总体评价")
            String summary,
            @Schema(description = "优点列表")
            List<String> strengths,
            @Schema(description = "改进建议列表")
            List<String> improvements,
            @Schema(description = "模型原始返回文本")
            String raw
    ) {
    }

    @Schema(description = "通用错误或健康检查消息")
    public record ApiError(
            @Schema(description = "消息内容", example = "ok")
            String message
    ) {
    }
}
