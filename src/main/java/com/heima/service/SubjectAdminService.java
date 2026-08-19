package com.heima.service;

import com.heima.dto.AdminDtos.SubjectSaveRequest;
import com.heima.entity.RkSubject;
import com.heima.mapper.RkSubjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SubjectAdminService {

    private final RkSubjectMapper subjectMapper;

    public SubjectAdminService(RkSubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    public List<RkSubject> listAll() {
        return subjectMapper.selectAll();
    }

    public List<RkSubject> listEnabled() {
        return subjectMapper.selectEnabled();
    }

    public RkSubject save(SubjectSaveRequest req, boolean create) {
        if (req == null || !StringUtils.hasText(req.id()) || !StringUtils.hasText(req.name())) {
            throw new IllegalArgumentException("请填写学科 ID 和名称");
        }
        String id = req.id().trim().toLowerCase(Locale.ROOT);
        RkSubject existing = subjectMapper.selectById(id);
        if (create && existing != null) {
            throw new IllegalArgumentException("学科 ID 已存在");
        }
        if (!create && existing == null) {
            throw new IllegalArgumentException("学科不存在");
        }
        RkSubject row = existing == null ? new RkSubject() : existing;
        row.setId(id);
        row.setName(req.name().trim());
        row.setShortName(StringUtils.hasText(req.shortName()) ? req.shortName().trim() : req.name().trim());
        row.setLevel(StringUtils.hasText(req.level()) ? req.level().trim() : "中级");
        row.setColor(StringUtils.hasText(req.color()) ? req.color().trim() : "#0f766e");
        row.setSortNo(req.sortNo() == null ? 99 : req.sortNo());
        row.setEnabled(Boolean.FALSE.equals(req.enabled()) ? 0 : 1);
        if (existing == null) {
            subjectMapper.insert(row);
        } else {
            subjectMapper.update(row);
        }
        return subjectMapper.selectById(id);
    }

    public RkSubject toggle(String id, Boolean enabled) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("缺少学科 ID");
        }
        RkSubject row = subjectMapper.selectById(id.trim());
        if (row == null) {
            throw new IllegalArgumentException("学科不存在");
        }
        if (enabled == null) {
            row.setEnabled(row.getEnabled() != null && row.getEnabled() == 1 ? 0 : 1);
        } else {
            row.setEnabled(enabled ? 1 : 0);
        }
        subjectMapper.update(row);
        return subjectMapper.selectById(row.getId());
    }
}
