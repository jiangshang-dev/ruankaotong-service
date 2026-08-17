package com.heima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class CaseAiDtos {

    private CaseAiDtos() {
    }

    @Schema(description = "案例分析题目截图（base64）")
    public record CaseImage(
            @Schema(description = "MIME，如 image/png、image/jpeg", example = "image/png")
            String mimeType,
            @Schema(description = "图片 base64，可带或不带 data: 前缀")
            String base64
    ) {
    }

    @Schema(description = "案例分析 AI 解答请求")
    public record CaseSolveRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "考生自拟标题，可空")
            String title,
            @Schema(description = "题目区附加文字（图片之外的说明）")
            String topicText,
            @Schema(description = "题目截图，至少 1 张")
            List<CaseImage> images
    ) {
    }

    @Schema(description = "单问参考答案")
    public record CaseQuestionAnswer(
            @Schema(description = "题号", example = "问题1")
            String questionNo,
            @Schema(description = "识别到的题干摘要")
            String stem,
            @Schema(description = "参考答案（采分点式）")
            String answer
    ) {
    }

    @Schema(description = "案例分析 AI 解答响应")
    public record CaseSolveResponse(
            @Schema(description = "识别出的试题标题")
            String title,
            @Schema(description = "按题号整理的完整参考答案，可直接写入答案框")
            String answerText,
            @Schema(description = "分问参考答案")
            List<CaseQuestionAnswer> questions,
            @Schema(description = "模型原始返回")
            String raw
    ) {
    }

    @Schema(description = "案例分析 AI 评分请求")
    public record CaseScoreRequest(
            @Schema(description = "考试科目", example = "系统架构设计师")
            String subject,
            @Schema(description = "考生自拟标题，可空")
            String title,
            @Schema(description = "题目区附加文字")
            String topicText,
            @Schema(description = "考生作答，只需标清题号")
            String answerText,
            @Schema(description = "题目截图")
            List<CaseImage> images
    ) {
    }

    @Schema(description = "案例分析评分响应（满分 75；≥45 合格）")
    public record CaseScoreResponse(
            @Schema(description = "总分（0-75）", example = "52")
            int totalScore,
            @Schema(description = "等级：合格 或 不及格", example = "合格",
                    allowableValues = {"合格", "不及格"})
            String level,
            @Schema(description = "各问得分（作评分维度展示）")
            List<EssayAiDtos.ScoreDimension> dimensions,
            @Schema(description = "总体评价")
            String summary,
            @Schema(description = "优点列表")
            List<String> strengths,
            @Schema(description = "改进建议列表")
            List<String> improvements,
            @Schema(description = "模型原始返回")
            String raw
    ) {
    }
}
