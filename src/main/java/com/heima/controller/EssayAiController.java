package com.heima.controller;

import com.heima.dto.EssayAiDtos.ApiError;
import com.heima.dto.EssayAiDtos.EssayPolishRequest;
import com.heima.dto.EssayAiDtos.EssayPolishResponse;
import com.heima.dto.EssayAiDtos.EssayScoreRequest;
import com.heima.dto.EssayAiDtos.EssayScoreResponse;
import com.heima.service.EssayAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/essay")
public class EssayAiController {

    private final EssayAiService essayAiService;

    public EssayAiController(EssayAiService essayAiService) {
        this.essayAiService = essayAiService;
    }

    @PostMapping("/polish")
    public EssayPolishResponse polish(@RequestBody EssayPolishRequest request) {
        return essayAiService.polish(request);
    }

    @PostMapping("/score")
    public EssayScoreResponse score(@RequestBody EssayScoreRequest request) {
        return essayAiService.score(request);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiError> health() {
        return ResponseEntity.ok(new ApiError("ok"));
    }
}
