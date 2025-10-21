package com.bakouan.app.service;

import com.bakouan.app.dto.BaLogDto;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaLog;
import com.bakouan.app.repositories.BaLogRepository;
import com.bakouan.app.utils.BaUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BaLogService {

    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);

    /**
     * Utilisé pour avoir accès aux paramètres d'entête
     * d'une requête.
     */
    private final HttpServletRequest request;

    private final BaLogRepository logRepository;

    /**
     * Enregistrer un log.
     *
     * @param logDto
     */
    public void log(final BaLogDto logDto) {
        final BaLog entity = mapper.maps(logDto);
        entity.setIpAdresse(BaUtils.retrieveIP(request));
        this.logRepository.save(entity);
    }
}
