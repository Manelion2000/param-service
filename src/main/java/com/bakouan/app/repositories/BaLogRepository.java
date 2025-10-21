package com.bakouan.app.repositories;

import com.bakouan.app.model.BaLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaLogRepository extends JpaRepository<BaLog, String> {
}
