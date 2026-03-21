<<<<<<< HEAD
package com.ginga.naviai.knowledge.repository;

import com.ginga.naviai.knowledge.entity.Knowledge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    /**
     * IDで記事を取得（論理削除済みを除外）。
     * author と tags を JOIN FETCH してN+1クエリを回避する。
     */
    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Knowledge> findByIdAndDeletedFalse(Long id);

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
                   "GROUP BY k.id, u.display_name " +
                   "ORDER BY like_count DESC, k.published_at DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopRecommendedArticles(@Param("limit") int limit);

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
     * 指定記事のいいね数を取得する。
     * "like" はSQL予約語のため引用符でエスケープする。
     */
    @Query(value = "SELECT COUNT(*) FROM \"like\" WHERE knowledge_id = :knowledgeId", nativeQuery = true)
    long countLikesByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    /**
     * 指定ユーザーが指定記事にいいねしているかを判定する。
     * 1件以上存在すれば1、存在しなければ0を返す。
     */
    @Query(value = "SELECT COUNT(*) FROM \"like\" WHERE knowledge_id = :knowledgeId AND user_id = :userId",
           nativeQuery = true)
    long countLikeByUserAndKnowledge(@Param("knowledgeId") Long knowledgeId, @Param("userId") Long userId);
}

=======
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
}
>>>>>>> 660599ffbff601f1cd328d28ad5cdd99ad0c77cb
