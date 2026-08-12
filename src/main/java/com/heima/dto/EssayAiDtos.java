package com.heima.dto;

import java.util.List;

public final class EssayAiDtos {

    private EssayAiDtos() {}

    public record EssayPolishRequest(
            String subject,
            String topic,
            /** abstract | body | all */
            String part,
            String abstractText,
            String bodyText
    ) {}

    public record EssayPolishResponse(
            String part,
            String abstractText,
            String bodyText,
            String raw
    ) {}

    public record EssayScoreRequest(
            String subject,
            String topic,
            String abstractText,
            String bodyText
    ) {}

    public record ScoreDimension(
            String name,
            int score,
            int max,
            String comment
    ) {}

    public record EssayScoreResponse(
            int totalScore,
            String level,
            List<ScoreDimension> dimensions,
            String summary,
            List<String> strengths,
            List<String> improvements,
            String raw
    ) {}

    public record ApiError(String message) {}
}
