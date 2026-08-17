package com.heima.controller;

import com.heima.dto.CaseAiDtos.CaseExplainRequest;
import com.heima.dto.CaseAiDtos.CaseScoreRequest;
import com.heima.dto.CaseAiDtos.CaseScoreResponse;
import com.heima.dto.CaseAiDtos.CaseSolveRequest;
import com.heima.dto.CaseAiDtos.CaseSolveResponse;
import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.service.CaseAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "案例分析 AI", description = "软考案例分析识图解答与评分（多模态）")
@RestController
@RequestMapping("/api/ai/case")
public class CaseAiController {

    private final CaseAiService caseAiService;

    public CaseAiController(CaseAiService caseAiService) {
        this.caseAiService = caseAiService;
    }

    @Operation(
            summary = "识图解答",
            description = "根据题目截图（多模态）识别试题并给出按题号排列的参考答案。每次请求新建 Agent，不保存会话。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解答成功",
                    content = @Content(schema = @Schema(implementation = CaseSolveResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误（如未提供截图）",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "模型或服务异常",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/solve")
    public CaseSolveResponse solve(@RequestBody CaseSolveRequest request) {
        return caseAiService.solve(request);
    }

    @Operation(
            summary = "AI 评分",
            description = "对照题目截图与考生作答评分。考生只需标清题号。满分 75，≥45 合格。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "评分成功",
                    content = @Content(schema = @Schema(implementation = CaseScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误（如答案为空）",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "模型或服务异常",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/score")
    public CaseScoreResponse score(@RequestBody CaseScoreRequest request) {
        return caseAiService.score(request);
    }

    @Operation(
            summary = "案例分析讲解（流式）",
            description = "讲解解题技巧并给出参考答案。直接返回 Flux；禁止 SseEmitter。"
    )
    @PostMapping(value = "/explain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EssayGuideStreamEvent> explainStream(
            @RequestBody CaseExplainRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return caseAiService.explainStream(request);
    }

    @Operation(summary = "案例分析讲解历史", description = "读取 AgentScope 为该案例会话落盘的 agent_state")
    @GetMapping("/explain/history")
    public EssayGuideHistoryResponse explainHistory(
            @RequestParam(required = false, defaultValue = "") String subjectId,
            @RequestParam(required = false, defaultValue = "") String fileName) {
        return caseAiService.listExplainHistory(subjectId, fileName);
    }

    @Operation(summary = "健康检查", description = "用于确认案例分析 AI 服务可用")
    @ApiResponse(responseCode = "200", description = "服务正常",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/health")
    public ResponseEntity<ApiError> health() {
        return ResponseEntity.ok(new ApiError("ok"));
    }
}
