package com.ginga.naviai.tags.service;

import com.ginga.naviai.tags.dto.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();
}
