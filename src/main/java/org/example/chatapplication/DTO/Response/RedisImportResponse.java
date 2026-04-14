package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisImportResponse {
    private int importedCount;
    private int totalCount;
    private String keyPrefix;
    private String source;
}

