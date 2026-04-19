package com.jhddt.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBatchImportResult {
    private int totalRows;
    private int successCount;
    private int failCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
