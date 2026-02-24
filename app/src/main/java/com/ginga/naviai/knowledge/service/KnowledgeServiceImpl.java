package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeServiceImpl(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public Page<KnowledgeResponse> getMyKnowledgeByUsername(String username, Pageable pageable) {
        Page<Knowledge> knowledgePage = knowledgeRepository.findByAuthorUsername(username, pageable);
        return knowledgePage.map(this::convertToKnowledgeResponse);
    }

    @Override
    public Page<KnowledgeResponse> getKnowledgeByAuthorId(Long authorId, Pageable pageable) {
        Page<Knowledge> knowledgePage = knowledgeRepository.findByAuthorId(authorId, pageable);
        return knowledgePage.map(this::convertToKnowledgeResponse);
    }

    private KnowledgeResponse convertToKnowledgeResponse(Knowledge knowledge) {
        String statusLabel = switch (knowledge.getStatus()) {
            case DRAFT -> "下書き";
            case PENDING -> "レビュー中";
            case PUBLISHED -> "公開";
            case DECLINED -> "差し戻し";
        };

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("Asia/Tokyo"));
        String formattedDate = formatter.format(knowledge.getCreatedAt());

        return new KnowledgeResponse(
                knowledge.getId().toString(),
                knowledge.getTitle(),
                knowledge.getExcerpt(),
                formattedDate,
                statusLabel,
                knowledge.getThumbnail()
        );
    }
}
