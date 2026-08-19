package com.heima.controller;

import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.dto.KnowledgeAiDtos.KnowledgeTutorRequest;
import com.heima.service.KnowledgeAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "综合知识 AI", description = "软考综合知识辅导：讲解、追问、复习方案、思维导图与例题")
@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeAiController {

    private final KnowledgeAiService knowledgeAiService;

    public KnowledgeAiController(KnowledgeAiService knowledgeAiService) {
        this.knowledgeAiService = knowledgeAiService;
    }

    @Operation(
            summary = "综合知识辅导（流式）",
            description = "按当前笔记讲解并可连续追问。直接返回 Flux；禁止 SseEmitter。"
    )
    @PostMapping(value = "/tutor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EssayGuideStreamEvent> tutorStream(
            @RequestBody KnowledgeTutorRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return knowledgeAiService.tutorStream(request);
    }

    @Operation(summary = "综合知识辅导历史", description = "读取 AgentScope 为该笔记会话落盘的 agent_state")
    @GetMapping("/tutor/history")
    public EssayGuideHistoryResponse tutorHistory(
            @RequestParam(required = false, defaultValue = "") String subjectId,
            @RequestParam(required = false, defaultValue = "") String fileName) {
        return knowledgeAiService.listTutorHistory(subjectId, fileName);
    }

    @Operation(summary = "健康检查", description = "用于确认综合知识 AI 服务可用")
    @ApiResponse(responseCode = "200", description = "服务正常",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/health")
    public ResponseEntity<ApiError> health() {
        return ResponseEntity.ok(new ApiError("ok"));
    }
}
