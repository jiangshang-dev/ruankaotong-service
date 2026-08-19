package com.heima.controller;

import com.heima.entity.RkSubject;
import com.heima.service.SubjectAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学科", description = "桌面客户端拉取已启用学科，无需登录")
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectAdminService subjectAdminService;

    public SubjectController(SubjectAdminService subjectAdminService) {
        this.subjectAdminService = subjectAdminService;
    }

    @Operation(summary = "已启用学科列表")
    @GetMapping
    public List<RkSubject> list() {
        return subjectAdminService.listEnabled();
    }
}
