package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeService {
    Page<KnowledgeResponse> getMyKnowledgeByUsername(String username, Pageable pageable);
    Page<KnowledgeResponse> getKnowledgeByAuthorId(Long authorId, Pageable pageable);
}
