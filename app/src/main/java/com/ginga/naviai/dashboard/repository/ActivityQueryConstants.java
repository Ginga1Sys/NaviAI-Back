package com.ginga.naviai.dashboard.repository;

/**
 * ダッシュボードのアクティビティ集計に使用するSQLクエリ定数クラス。
 * <p>
 * comment テーブル・like テーブルは現時点で JPA エンティティ化されていないため、
 * NamedParameterJdbcTemplate 経由でアクセスする。
 * SQL は可読性・変更容易性のためここに集約する。
 * </p>
 */
public final class ActivityQueryConstants {

    private ActivityQueryConstants() {
        // ユーティリティクラスのため、インスタンス化禁止
    }

    /** 日付範囲内の投稿数を日別に集計するSQL */
    public static final String POSTS_BY_DAY =
            "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
            "FROM knowledge " +
            "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to AND is_deleted = false " +
            "GROUP BY dt ORDER BY dt";

    /** 日付範囲内のコメント数を日別に集計するSQL */
    public static final String COMMENTS_BY_DAY =
            "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
            "FROM comment " +
            "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to AND is_deleted = false " +
            "GROUP BY dt ORDER BY dt";

    /**
     * 日付範囲内のいいね数を日別に集計するSQL。
     * <p>
     * {@code "like"} はSQL予約語のためダブルクォートでエスケープしている。
     * </p>
     */
    public static final String LIKES_BY_DAY =
            "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
            "FROM \"like\" " +
            "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to " +
            "GROUP BY dt ORDER BY dt";
}
