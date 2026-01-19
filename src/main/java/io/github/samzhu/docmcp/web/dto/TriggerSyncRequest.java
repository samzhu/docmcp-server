package io.github.samzhu.docmcp.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 觸發同步請求
 * <p>
 * 用於手動觸發文件同步的請求資料。
 * </p>
 *
 * @param version 要同步的版本
 */
public record TriggerSyncRequest(
        @NotBlank(message = "版本不可為空")
        String version
) {}
