package com.heima.service;

import com.heima.dto.EssayAiDtos.EssayGuideStreamEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AiQaStreamRecorder {

    private final AiQaLogService aiQaLogService;

    public AiQaStreamRecorder(AiQaLogService aiQaLogService) {
        this.aiQaLogService = aiQaLogService;
    }

    public Flux<EssayGuideStreamEvent> tap(
            Flux<EssayGuideStreamEvent> flux,
            String clientIp,
            String module,
            String action,
            String subjectId,
            String subjectName,
            String fileName,
            String topic,
            String question,
            String email) {
        return flux.doOnNext(event -> {
            if (event == null || !"done".equals(event.type())) {
                return;
            }
            String answer = StringUtils.hasText(event.markdown()) ? event.markdown() : "";
            Mono.fromRunnable(() -> aiQaLogService.record(
                            clientIp, module, action, subjectId, subjectName, fileName, topic, question, answer, email))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        });
    }
}
