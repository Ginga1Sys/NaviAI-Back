package com.ginga.naviai.auth.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void validPassword_withThreeCategories_isValid() {
        // 英大文字・英小文字・数字・記号のうち3種類以上を含み、長さ要件を満たすパスワードが受け入れられることを検証する
        assertTrue(validator.isValid("Abcdef1!", null)); // upper, lower, digit, symbol
        assertTrue(validator.isValid("ABCdefgh1", null)); // upper, lower, digit
        assertTrue(validator.isValid("abcd1!@#A", null));
    }

    @Test
    void invalidPassword_lessThanThreeCategories_or_short_isInvalid() {
        // 3種類未満のカテゴリ、短すぎる、または null のパスワードが拒否されることを検証する
        assertFalse(validator.isValid("abcdefg1", null)); // lower+digit only (2 categories)
        assertFalse(validator.isValid("ABCDEFGH", null)); // upper only
        assertFalse(validator.isValid("Ab1!", null)); // too short
        assertFalse(validator.isValid(null, null));
    }
}
