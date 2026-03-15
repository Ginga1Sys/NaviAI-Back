package com.ginga.naviai.tags.service;

import com.ginga.naviai.tags.dto.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();

    /**
     * 公開記事に紐づくタグを件数降順で取得する。
     * SCR-12 公開トップ画面向け（認証不要）。
     */
    List<TagResponse> getPublicTags();
}

