package com.heima.controller;

import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.dto.EssayAiDtos.EssayGuideHistoryResponse;
import com.heima.dto.EssayAiDtos.EssayGuideRequest;
import com.heima.dto.EssayAiDtos.EssayGuideResponse;
import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import com.heima.dto.EssayAiDtos.EssayPolishRequest;
import com.heima.dto.EssayAiDtos.EssayPolishResponse;
import com.heima.dto.EssayAiDtos.EssayScoreRequest;
import com.heima.dto.EssayAiDtos.EssayScoreResponse;
import com.heima.service.AiQaLogService;
import com.heima.service.AiQaStreamRecorder;
import com.heima.service.EssayAiService;
import com.heima.web.ClientAuthInterceptor;
import com.heima.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@Tag(name = "论文 AI", description = "软考论文润色、评分与写作指导")
@RestController
@RequestMapping("/api/ai/essay")
public class EssayAiController {

    private final EssayAiService essayAiService;
    private final AiQaLogService aiQaLogService;
    private final AiQaStreamRecorder aiQaStreamRecorder;

    public EssayAiController(
            EssayAiService essayAiService,
            AiQaLogService aiQaLogService,
            AiQaStreamRecorder aiQaStreamRecorder) {
        this.essayAiService = essayAiService;
        this.aiQaLogService = aiQaLogService;
        this.aiQaStreamRecorder = aiQaStreamRecorder;
    }

    @Operation(
            summary = "润色论文",
            description = "按 part 润色摘要、正文或全部。每次请求新建 Agent，不保存会话历史。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "润色成功",
                    content = @Content(schema = @Schema(implementation = EssayPolishResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "模型或服务异常",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/polish")
    public EssayPolishResponse polish(@RequestBody EssayPolishRequest request, HttpServletRequest http) {
        EssayPolishResponse res = essayAiService.polish(request);
        String part = request == null ? "" : request.part();
        aiQaLogService.record(
                ClientIp.from(http),
                "论文",
                "论文润色",
                request == null ? "" : request.subjectId(),
                request == null ? "" : request.subject(),
                request == null ? "" : request.fileName(),
                request == null ? "" : request.topic(),
                "润色范围：" + part,
                res == null ? "" : res.raw(),
                ClientAuthInterceptor.email(http));
        return res;
    }

    @Operation(
            summary = "AI 评分",
            description = "对论文摘要与正文评分并给出解读。满分 75，≥45 合格，<45 不及格。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "评分成功",
                    content = @Content(schema = @Schema(implementation = EssayScoreResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "参数错误（如摘要正文皆空）",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "模型或服务异常",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/score")
    public EssayScoreResponse score(@RequestBody EssayScoreRequest request, HttpServletRequest http) {
        EssayScoreResponse res = essayAiService.score(request);
        String answer = res == null ? "" : ("总分 " + res.totalScore() + "（" + res.level() + "）\n" + res.summary());
        aiQaLogService.record(
                ClientIp.from(http),
                "论文",
                "论文评分",
                request == null ? "" : request.subjectId(),
                request == null ? "" : request.subject(),
                request == null ? "" : request.fileName(),
                request == null ? "" : request.topic(),
                "请给当前论文评分",
                answer,
                ClientAuthInterceptor.email(http));
        return res;
    }

    @Operation(
            summary = "论文指导",
            description = "根据题目文字或截图（多模态）给出写作方案、技巧意见，并杜撰一个可写的大型项目举例。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "指导成功",
                    content = @Content(schema = @Schema(implementation = EssayGuideResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误（题目为空）",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "模型或服务异常",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/guide")
    public EssayGuideResponse guide(@RequestBody EssayGuideRequest request, HttpServletRequest http) {
        EssayGuideResponse res = essayAiService.guide(request);
        aiQaLogService.record(
                ClientIp.from(http),
                "论文",
                "论文指导",
                request == null ? "" : request.subjectId(),
                request == null ? "" : request.subject(),
                request == null ? "" : request.fileName(),
                request == null ? "" : request.topic(),
                "请按题目给出写作方案",
                res == null ? "" : res.raw(),
                ClientAuthInterceptor.email(http));
        return res;
    }

    @Operation(
            summary = "论文指导（流式）",
            description = "直接返回 Flux，边生成边推送；禁止 SseEmitter。"
    )
    @PostMapping(value = "/guide/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EssayGuideStreamEvent> guideStream(
            @RequestBody EssayGuideRequest request,
            HttpServletRequest http,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return aiQaStreamRecorder.tap(
                essayAiService.guideStream(request),
                ClientIp.from(http),
                "论文",
                "论文指导",
                request == null ? "" : request.subjectId(),
                request == null ? "" : request.subject(),
                request == null ? "" : request.fileName(),
                request == null ? "" : request.topic(),
                "请按题目帮我搭一个可写的大型项目。",
                ClientAuthInterceptor.email(http));
    }

    @Operation(summary = "论文指导历史", description = "读取 AgentScope 为该论文会话落盘的 agent_state")
    @GetMapping("/guide/history")
    public EssayGuideHistoryResponse guideHistory(
            @RequestParam(required = false, defaultValue = "") String subjectId,
            @RequestParam(required = false, defaultValue = "") String fileName) {
        return essayAiService.listGuideHistory(subjectId, fileName);
    }

    @Operation(summary = "健康检查", description = "用于确认 AI 服务进程可用")
    @ApiResponse(responseCode = "200", description = "服务正常",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/health")
    public ResponseEntity<ApiError> health() {
        return ResponseEntity.ok(new ApiError("ok"));
    }
}
