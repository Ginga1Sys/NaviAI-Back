package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Override
    public Page<KnowledgeResponse> getMyKnowledge(Long userId, Pageable pageable) {
        return getKnowledgeByAuthorId(userId, pageable);
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

        String formattedDate = knowledge.getCreatedAt().toString();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            formattedDate = formatter.format(knowledge.getCreatedAt());
        } catch (Exception e) {
            //
        }

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
