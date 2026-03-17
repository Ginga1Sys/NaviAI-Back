package com.ginga.naviai.admin.advice;

import com.ginga.naviai.admin.exception.AdminApiException;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminConflictException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdminExceptionHandler のユニットテスト
 */
public class AdminExceptionHandlerTest {

    private final AdminExceptionHandler handler = new AdminExceptionHandler();

    // ========== AdminApiException ハンドリング ==========

    @Test
    void handleAdmin_notFound_returns404() {
        // AdminNotFoundException が 404 レスポンスを返すことを検証する
        AdminApiException ex = new AdminNotFoundException("Resource not found");

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertResponseContains(res, "NOT_FOUND", "Resource not found");
    }

    @Test
    void handleAdmin_badRequest_returns400() {
        // AdminBadRequestException が 400 レスポンスを返すことを検証する
        AdminApiException ex = new AdminBadRequestException("Invalid input");

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertResponseContains(res, "INVALID_INPUT", "Invalid input");
    }

    @Test
    void handleAdmin_badRequestWithDetails_includesDetails() {
        // AdminBadRequestException の details がレスポンスに含まれることを検証する
        AdminApiException ex = new AdminBadRequestException("Validation failed",
                Map.of("field", "must not be blank"));

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertNotNull(error.get("details"));
    }

    @Test
    void handleAdmin_conflict_returns409() {
        // AdminConflictException が 409 レスポンスを返すことを検証する
        AdminApiException ex = new AdminConflictException("Status conflict");

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertResponseContains(res, "INVALID_STATUS_TRANSITION", "Status conflict");
    }

    @Test
    void handleAdmin_conflictWithCustomCode_returnsCustomCode() {
        // カスタムコード付き AdminConflictException が正しいコードを返すことを検証する
        AdminApiException ex = new AdminConflictException("DUPLICATE_ENTRY", "Already exists");

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertResponseContains(res, "DUPLICATE_ENTRY", "Already exists");
    }

    @Test
    void handleAdmin_nullMessage_returnsDefaultError() {
        // メッセージが null の場合 "Error" がデフォルトで返されることを検証する
        AdminApiException ex = new AdminApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", null);

        ResponseEntity<?> res = handler.handleAdmin(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("Error", error.get("message"));
    }

    // ========== ヘルパー ==========

    @SuppressWarnings("unchecked")
    private void assertResponseContains(ResponseEntity<?> res, String expectedCode, String expectedMessage) {
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertNotNull(error);
        assertEquals(expectedCode, error.get("code"));
        assertEquals(expectedMessage, error.get("message"));
    }
}
