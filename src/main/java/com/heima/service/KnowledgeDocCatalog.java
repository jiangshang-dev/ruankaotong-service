package com.heima.service;

import com.heima.config.AiProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 综合知识讲义：从 classpath 读取 {@code src/main/resources/docs}。
 * 打成 JAR 后仍走 classpath，不依赖本机绝对路径，换电脑/服务器都能用。
 */
@Slf4j
@Component
public class KnowledgeDocCatalog {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final String location;
    private volatile List<String> cachedNames;

    public KnowledgeDocCatalog(AiProperties props) {
        this.location = StringUtils.hasText(props.getKnowledgeDocs())
                ? props.getKnowledgeDocs().trim()
                : "classpath:docs";
    }

    public List<String> listNames() {
        List<String> cached = cachedNames;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedNames != null) {
                return cachedNames;
            }
            List<String> names = new ArrayList<>();
            try {
                String pattern = location.startsWith("classpath")
                        ? location.replace("classpath:", "classpath*:") + (location.endsWith("/") ? "*" : "/*")
                        : location + (location.endsWith("/") ? "*" : "/*");
                Resource[] resources = resolver.getResources(pattern);
                for (Resource resource : resources) {
                    String name = resource.getFilename();
                    if (!StringUtils.hasText(name) || name.startsWith(".")) {
                        continue;
                    }
                    names.add(name);
                }
                names.sort(Comparator.naturalOrder());
            } catch (IOException e) {
                log.warn("读取讲义目录失败 location={}", location, e);
            }
            cachedNames = List.copyOf(names);
            if (names.isEmpty()) {
                log.warn("讲义目录为空 location={}（请把 PDF 放进 src/main/resources/docs）", location);
            } else {
                log.info("综合知识讲义 {} 份，来自 {}", names.size(), location);
            }
            return cachedNames;
        }
    }

    public String promptBlock(String title, String question) {
        List<String> names = listNames();
        if (names.isEmpty()) {
            return "【讲义目录】未找到打包进 JAR 的 classpath:docs，请确认 src/main/resources/docs 已随应用打包。";
        }
        String query = ((title == null ? "" : title) + " " + (question == null ? "" : question))
                .toLowerCase(Locale.ROOT);
        List<String> hit = names.stream().filter(name -> matches(query, name)).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("【讲义目录】以下文件已打进应用（classpath:docs），换机器也可用。优先按文件名对应的讲义口径回答。\n");
        if (!hit.isEmpty()) {
            sb.append("与本题最相关：\n");
            for (String name : hit) {
                sb.append("- ").append(name).append('\n');
            }
        }
        sb.append("全部讲义：\n");
        for (String name : names) {
            sb.append("- ").append(name).append('\n');
        }
        return sb.toString().trim();
    }

    private static boolean matches(String query, String filename) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        String name = filename.toLowerCase(Locale.ROOT);
        for (String key : Arrays.asList(
                "英语", "词汇", "思维导图", "导图", "易混淆", "100条", "知识点",
                "打卡", "计划", "备考", "自查", "考频", "100题", "例题", "经典",
                "核心", "宝典", "集锦", "三色", "案例", "论文", "软件工程",
                "架构", "数据库", "网络", "嵌入式", "项目", "大数据", "安全")) {
            if (query.contains(key) && name.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
