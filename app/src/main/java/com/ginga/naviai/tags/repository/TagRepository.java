package com.ginga.naviai.tags.repository;

import com.ginga.naviai.knowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    interface TagCountProjection {
        String getName();
        Long getCount();
    }

    @Query(value = "SELECT t.name AS name, COUNT(kt.knowledge_id) AS count " +
            "FROM tag t " +
            "JOIN knowledge_tag kt ON t.id = kt.tag_id " +
            "JOIN knowledge k ON k.id = kt.knowledge_id " +
            "WHERE k.is_deleted = false AND k.status = 'published' " +
            "GROUP BY t.name " +
            "ORDER BY COUNT(kt.knowledge_id) DESC, t.name ASC", nativeQuery = true)
    List<TagCountProjection> findTagUsageCounts();

    /**
     * 公開記事（visibility=public かつ status=published かつ未削除）に紐づくタグを件数降順で取得する。
     * SCR-12 公開トップ画面向け。認証不要エンドポイントから利用する。
     */
    @Query(value = "SELECT t.name AS name, COUNT(kt.knowledge_id) AS count " +
            "FROM tag t " +
            "JOIN knowledge_tag kt ON t.id = kt.tag_id " +
            "JOIN knowledge k ON k.id = kt.knowledge_id " +
            "WHERE k.is_deleted = false AND k.status = 'published' AND k.visibility = 'public' " +
            "GROUP BY t.name " +
            "ORDER BY COUNT(kt.knowledge_id) DESC, t.name ASC", nativeQuery = true)
    List<TagCountProjection> findPublicTagUsageCounts();
}
