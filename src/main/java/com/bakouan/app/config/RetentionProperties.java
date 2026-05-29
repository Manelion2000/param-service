package com.bakouan.app.config;

import com.bakouan.app.enums.DataRetentionMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.retention")
public class RetentionProperties {
    private boolean enabled = false;
    private int keepDays = 180;
    private int batchSize = 5000;
    private String cron = "0 30 2 * * *";
    private DataRetentionMode mode = DataRetentionMode.ARCHIVE_AND_PURGE;
}
