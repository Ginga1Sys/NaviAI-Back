package com.ginga.naviai.exception;

import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDuplicate_returns409AndMessage() {
        // DuplicateResourceException が HTTP 409 レスポンスに変換され、メッセージがボディに含まれることを検証する
        DuplicateResourceException ex = new DuplicateResourceException("already exists");
        ResponseEntity<Object> resp = handler.handleDuplicate(ex);
        assertEquals(409, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().toString().contains("already exists"));
    }
}
