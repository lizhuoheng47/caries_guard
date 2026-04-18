package com.cariesguard.system.config;

import com.cariesguard.system.app.CompetitionExposureService;
import com.cariesguard.system.app.CompetitionModeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CompetitionModeStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(CompetitionModeStartupLogger.class);

    private final CompetitionExposureService competitionExposureService;

    public CompetitionModeStartupLogger(CompetitionExposureService competitionExposureService) {
        this.competitionExposureService = competitionExposureService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSummary() {
        CompetitionModeSnapshot snapshot = competitionExposureService.snapshot();
        log.info("Competition mode: {}", snapshot.enabled() ? "ENABLED" : "DISABLED");
        log.info(
                "Competition exposure summary: hiddenPermissionPrefixes={}, hiddenPermissionCodes={}, hiddenRoutes={}, forceVisibleRoutes={}, retainedEntries={}, enforcementSurfaces={}",
                snapshot.hiddenPermissionPrefixes(),
                snapshot.hiddenPermissionCodes(),
                snapshot.hiddenRoutePaths(),
                snapshot.forceVisibleRoutePaths(),
                snapshot.retainedEntries(),
                snapshot.enforcementSurfaces());
    }
}
