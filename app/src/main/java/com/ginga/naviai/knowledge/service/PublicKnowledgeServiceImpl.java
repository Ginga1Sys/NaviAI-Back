package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.PublicKnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.PublicKnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.Tag;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicKnowledgeServiceImpl implements PublicKnowledgeService {

    private final KnowledgeRepository knowledgeRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicKnowledgePageResponse getPublicKnowledge(Pageable pageable) {
        Page<Knowledge> page = knowledgeRepository.findPublicKnowledge(pageable);

        List<PublicKnowledgeItemDto> items = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return PublicKnowledgePageResponse.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicRecommendedKnowledgeResponse getRecommendedKnowledge(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 10));
        List<Object[]> rows = knowledgeRepository.findTopRecommendedArticlesAll(cappedLimit);

        List<PublicRecommendedKnowledgeItemDto> items = rows.stream()
                .map(this::toRecommendedDto)
                .toList();

        return PublicRecommendedKnowledgeResponse.builder()
                .items(items)
                .build();
    }

    private PublicKnowledgeItemDto toDto(Knowledge k) {
        List<String> tagNames = k.getTags().stream()
                .map(Tag::getName)
                .sorted()
                .toList();

        String authorDisplayName = (k.getAuthor() != null && k.getAuthor().getDisplayName() != null)
                ? k.getAuthor().getDisplayName()
                : "";

        return PublicKnowledgeItemDto.builder()
                .id(k.getId())
                .title(k.getTitle())
                .excerpt(k.getExcerpt())
                .thumbnail(k.getThumbnail())
                .authorDisplayName(authorDisplayName)
                .publishedAt(k.getPublishedAt())
                .tags(tagNames)
                .build();
    }

        private PublicRecommendedKnowledgeItemDto toRecommendedDto(Object[] row) {
                Long id = ((Number) row[0]).longValue();
                String title = String.valueOf(row[1]);
                long likeCount = row[2] == null ? 0L : ((Number) row[2]).longValue();

                return PublicRecommendedKnowledgeItemDto.builder()
                                .id(id)
                                .title(title)
                                .likeCount(likeCount)
                                .build();
        }
}
