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

    @Schema(description = "论文题目截图")
    public record EssayImage(
            @Schema(description = "MIME，如 image/png", example = "image/png")
            String mimeType,
            @Schema(description = "图片 base64，可带或不带 data: 前缀")
            String base64
    ) {
    }

    @Schema(description = "论文指导请求")
    public record EssayGuideRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "论文题目与要求（文字，可与截图同时给）")
            String topic,
            @Schema(description = "当前摘要，可空")
            String abstractText,
            @Schema(description = "当前正文，可空")
            String bodyText,
            @Schema(description = "题目截图，可空")
            List<EssayImage> images,
            @Schema(description = "科目 ID，作为 AgentScope userId", example = "architect")
            String subjectId,
            @Schema(description = "论文文件名，作为 AgentScope sessionId；未保存可用草稿 key")
            String fileName
    ) {
    }

    @Schema(description = "论文指导流式事件")
    public record EssayGuideStreamEvent(
            @Schema(description = "think_delta=思考增量，delta=正文增量，done=结束，error=失败")
            String type,
            @Schema(description = "本次增量文本；error 时为错误信息")
            String delta,
            @Schema(description = "已生成的指导正文 Markdown")
            String markdown,
            @Schema(description = "已生成的思考过程 Markdown")
            String thinking,
            @Schema(description = "本条指导 ID")
            String id,
            @Schema(description = "创建时间戳")
            Long createdAt
    ) {
    }

    @Schema(description = "论文指导历史记录")
    public record EssayGuideHistoryRecord(
            String id,
            long createdAt,
            String subjectId,
            String fileName,
            String topic,
            String markdown,
            String thinking,
            @Schema(description = "user 或 assistant，默认 assistant")
            String role
    ) {
    }

    @Schema(description = "论文指导历史列表")
    public record EssayGuideHistoryResponse(
            String subjectId,
            String fileName,
            List<EssayGuideHistoryRecord> records
    ) {
    }

    @Schema(description = "论文框架一段")
    public record EssayGuideSection(
            @Schema(description = "段落名称", example = "摘要")
            String name,
            @Schema(description = "建议字数", example = "280-300")
            String words,
            @Schema(description = "本段应写内容")
            String content
    ) {
    }

    @Schema(description = "为考生杜撰的大型项目举例")
    public record EssayProjectExample(
            String name,
            String industry,
            String company,
            String role,
            String period,
            String background,
            String modules,
            String techChoice,
            String effects,
            String story
    ) {
    }

    @Schema(description = "论文指导响应")
    public record EssayGuideResponse(
            @Schema(description = "识读出的论文标题")
            String recognizedTopic,
            @Schema(description = "题目各小问")
            List<String> subQuestions,
            @Schema(description = "对应各小问的核心论点")
            List<String> coreArguments,
            @Schema(description = "120 分钟时间分配")
            String timePlan,
            @Schema(description = "写作框架")
            List<EssayGuideSection> framework,
            @Schema(description = "针对本题的技巧与意见")
            List<String> tips,
            @Schema(description = "易踩的坑")
            List<String> pitfalls,
            @Schema(description = "大型项目举例")
            EssayProjectExample project,
            @Schema(description = "摘要草稿")
            String abstractDraft,
            @Schema(description = "正文提纲")
            String bodyOutline,
            @Schema(description = "模型原始返回")
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
