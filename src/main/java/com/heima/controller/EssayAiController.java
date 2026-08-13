package com.heima.controller;

import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.dto.EssayAiDtos.EssayPolishRequest;
import com.heima.dto.EssayAiDtos.EssayPolishResponse;
import com.heima.dto.EssayAiDtos.EssayScoreRequest;
import com.heima.dto.EssayAiDtos.EssayScoreResponse;
import com.heima.service.EssayAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "论文 AI", description = "软考论文润色与评分相关接口")
@RestController
@RequestMapping("/api/ai/essay")
public class EssayAiController {

    private final EssayAiService essayAiService;

    public EssayAiController(EssayAiService essayAiService) {
        this.essayAiService = essayAiService;
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
    public EssayPolishResponse polish(@RequestBody EssayPolishRequest request) {
        return essayAiService.polish(request);
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
    public EssayScoreResponse score(@RequestBody EssayScoreRequest request) {
        return essayAiService.score(request);
    }

    @Operation(summary = "健康检查", description = "用于确认 AI 服务进程可用")
    @ApiResponse(responseCode = "200", description = "服务正常",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/health")
    public ResponseEntity<ApiError> health() {
        return ResponseEntity.ok(new ApiError("ok"));
    }
}
