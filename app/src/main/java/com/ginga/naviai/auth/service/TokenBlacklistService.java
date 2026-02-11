package com.ginga.naviai.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * トークンブラックリストサービス
 * Redis を使用してアクセストークンの jti をブラックリストに登録・検証する
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * トークンの jti をブラックリストに追加する
     * 
     * @param jti トークン識別子
     * @param ttlSeconds ブラックリストに保持する秒数（通常はアクセストークンの残存時間）
     */
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isEmpty()) {
            logger.warn("Attempted to blacklist null or empty jti");
            return;
        }
        
        try {
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
            logger.info("Token jti {} added to blacklist with TTL {} seconds", jti, ttlSeconds);
        } catch (Exception e) {
            logger.error("Failed to add jti {} to blacklist: {}", jti, e.getMessage());
            // Redis が利用できない場合でもログアウト処理自体は継続する
        }
    }

    /**
     * トークンの jti がブラックリストに存在するか確認する
     * 
     * @param jti トークン識別子
     * @return ブラックリストに存在する場合 true
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        
        try {
            String key = BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("Failed to check blacklist for jti {}: {}", jti, e.getMessage());
            // Redis が利用できない場合は安全側に倒してブラックリストに存在しないとみなす
            // ただし、セキュリティ要件によってはここで true を返す選択もある
            return false;
        }
    }

    /**
     * ブラックリストからトークンの jti を削除する（テスト用途等）
     * 
     * @param jti トークン識別子
     */
    public void removeFromBlacklist(String jti) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        
        try {
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.delete(key);
            logger.info("Token jti {} removed from blacklist", jti);
        } catch (Exception e) {
            logger.error("Failed to remove jti {} from blacklist: {}", jti, e.getMessage());
        }
    }
}
