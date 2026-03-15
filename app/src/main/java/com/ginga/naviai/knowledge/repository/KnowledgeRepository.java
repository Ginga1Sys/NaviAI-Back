package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Knowledge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    long countByDeletedFalse();

    long countByCreatedAtAfterAndDeletedFalse(Instant start);

    long countByStatusAndDeletedFalse(String status);

    @Query(value = "SELECT t.name as tag, COUNT(kt.knowledge_id) as count " +
                   "FROM tag t " +
                   "JOIN knowledge_tag kt ON t.id = kt.tag_id " +
                   "GROUP BY t.name " +
                   "ORDER BY count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopTags(@Param("limit") int limit);

    @Query("SELECT k FROM Knowledge k WHERE k.status = 'published' AND k.deleted = false ORDER BY k.publishedAt DESC")
    List<Knowledge> findRecentArticles(Pageable pageable);

    @Query(value = "SELECT k.id, k.title, u.display_name, k.published_at, COUNT(l.id) as like_count " +
                   "FROM knowledge k " +
                   "LEFT JOIN users u ON k.author_id = u.id " +
                   "LEFT JOIN \"like\" l ON k.id = l.knowledge_id " +
                   "WHERE k.status = 'published' AND k.is_deleted = false " +
                   "GROUP BY k.id, k.title, k.published_at, u.display_name " +
                   "ORDER BY like_count DESC, k.published_at DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopRecommendedArticles(@Param("limit") int limit);

    /**
     * 公開トップ画面の「今週の注目」向け。
     * 非公開記事（visibility=private）のみを、いいね数の多い順で取得する。
     */
    @Query(value = "SELECT k.id, k.title, COUNT(l.id) as like_count " +
                   "FROM knowledge k " +
                   "LEFT JOIN \"like\" l ON k.id = l.knowledge_id " +
                   "WHERE k.is_deleted = false AND k.status = 'published' AND k.visibility = 'private' " +
                   "GROUP BY k.id, k.title " +
                   "ORDER BY like_count DESC, k.created_at DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopRecommendedArticlesAll(@Param("limit") int limit);

    long countByCreatedAtBetweenAndDeletedFalse(Instant start, Instant end);

    /**
     * 指定期間内に作成された記事の作成日時一覧を1回のクエリで取得する。
     * <p>
     * 週次アクティビティ集計で N+1 問題を回避するために使用する。
     * 期間を絞った上でアプリケーション側で週別に集計する。
     * </p>
     */
    @Query("SELECT k.createdAt FROM Knowledge k WHERE k.deleted = false AND k.createdAt >= :start AND k.createdAt < :end")
    List<Instant> findCreatedAtInRange(@Param("start") Instant start, @Param("end") Instant end);

    Page<Knowledge> findByAuthorId(Long authorId, Pageable pageable);

    Page<Knowledge> findByAuthorUsername(String username, Pageable pageable);

    /**
     * 公開記事（visibility=public かつ status=published かつ未削除）をページ取得する。
     * SCR-12 公開トップ画面向け。認証不要エンドポイントから呼ばれる。
     */
    @Query("SELECT k FROM Knowledge k WHERE k.visibility = 'public' AND k.status = 'published' AND k.deleted = false ORDER BY k.publishedAt DESC")
    Page<Knowledge> findPublicKnowledge(Pageable pageable);
}
