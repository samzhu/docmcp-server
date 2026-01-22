package io.github.samzhu.docmcp.service;

import com.github.f4b6a3.tsid.TsidFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdService 單元測試
 * <p>
 * 驗證 TSID 生成服務的正確性，包含格式、唯一性及順序性。
 * </p>
 */
@DisplayName("IdService 測試")
class IdServiceTest {

    private IdService idService;

    @BeforeEach
    void setUp() {
        // 使用預設配置的 TsidFactory
        TsidFactory tsidFactory = TsidFactory.builder().build();
        idService = new IdService(tsidFactory);
    }

    @Test
    @DisplayName("應該生成 13 字元的 TSID")
    void shouldGenerateThirteenCharacterId() {
        String id = idService.generateId();

        assertNotNull(id);
        assertEquals(13, id.length(), "TSID 應該是 13 個字元");
    }

    @Test
    @DisplayName("應該生成符合 Crockford Base32 格式的 ID")
    void shouldGenerateCrockfordBase32Format() {
        String id = idService.generateId();

        // Crockford Base32 只包含 0-9 和 A-Z（排除 I, L, O, U）
        assertTrue(id.matches("^[0-9A-HJKMNP-TV-Z]+$"),
                "ID 應該符合 Crockford Base32 格式: " + id);
    }

    @Test
    @DisplayName("連續生成的 ID 應該唯一")
    void shouldGenerateUniqueIds() {
        Set<String> ids = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            ids.add(idService.generateId());
        }

        assertEquals(count, ids.size(), "所有生成的 ID 應該唯一");
    }

    @Test
    @DisplayName("連續生成的 ID 應該有遞增趨勢")
    void shouldGenerateIdsInIncreasingOrder() {
        String id1 = idService.generateId();
        String id2 = idService.generateId();
        String id3 = idService.generateId();

        // TSID 的字串比較應該反映時間順序
        assertTrue(id1.compareTo(id2) < 0,
                "後生成的 ID 應該大於先生成的: " + id1 + " < " + id2);
        assertTrue(id2.compareTo(id3) < 0,
                "後生成的 ID 應該大於先生成的: " + id2 + " < " + id3);
    }

    @Test
    @DisplayName("不同實例應該都能生成有效 ID")
    void shouldGenerateValidIdFromDifferentInstances() {
        TsidFactory factory1 = TsidFactory.builder().build();
        TsidFactory factory2 = TsidFactory.builder().build();

        IdService service1 = new IdService(factory1);
        IdService service2 = new IdService(factory2);

        String id1 = service1.generateId();
        String id2 = service2.generateId();

        assertEquals(13, id1.length());
        assertEquals(13, id2.length());
        assertNotEquals(id1, id2, "不同實例生成的 ID 應該不同");
    }
}
