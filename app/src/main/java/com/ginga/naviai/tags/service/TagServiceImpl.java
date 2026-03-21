package com.ginga.naviai.tags.service;

import com.ginga.naviai.tags.dto.TagResponse;
import com.ginga.naviai.tags.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findTagUsageCounts().stream()
                .map(row -> TagResponse.builder()
                        .name(row.getName())
                        .count(row.getCount())
                        .build())
                .toList();
    }
}
