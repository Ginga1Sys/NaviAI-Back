package com.ginga.naviai.tags.service;

import com.ginga.naviai.tags.dto.TagResponse;
import com.ginga.naviai.tags.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    /**
     * タグ一覧が count 付きで返ることを確認する。
     */
    @Test
    void getAllTags_returnsMappedTagResponses() {
        TagRepository.TagCountProjection row1 = mock(TagRepository.TagCountProjection.class);
        when(row1.getName()).thenReturn("機械学習");
        when(row1.getCount()).thenReturn(42L);

        TagRepository.TagCountProjection row2 = mock(TagRepository.TagCountProjection.class);
        when(row2.getName()).thenReturn("Next.js");
        when(row2.getCount()).thenReturn(18L);

        when(tagRepository.findTagUsageCounts()).thenReturn(List.of(row1, row2));

        List<TagResponse> result = tagService.getAllTags();

        assertEquals(2, result.size());
        assertEquals("機械学習", result.get(0).getName());
        assertEquals(42L, result.get(0).getCount());
        assertEquals("Next.js", result.get(1).getName());
        assertEquals(18L, result.get(1).getCount());
    }

    /**
     * タグが存在しない場合に空配列を返すことを確認する。
     */
    @Test
    void getAllTags_returnsEmptyListWhenNoTags() {
        when(tagRepository.findTagUsageCounts()).thenReturn(List.of());

        List<TagResponse> result = tagService.getAllTags();

        assertTrue(result.isEmpty());
    }
}
