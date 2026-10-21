package com.charroux.rentalQueryService.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/** Imports an optional historical NDJSON export and can rebuild from the Mongo archive. */
@Component
public class ProjectionBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionBootstrap.class);
    private final String path;
    private final boolean rebuildFromArchive;
    private final boolean consumerEnabled;
    private final RentalViewRepository repository;
    private final ProjectionEventArchiveRepository archiveRepository;
    private final RentalProjectionProjector projector;
    private final ProjectionIngestionService ingestionService;

    public ProjectionBootstrap(
            @Value("${projection.bootstrap.path:}") String path,
            @Value("${projection.rebuild-from-archive:false}") boolean rebuildFromArchive,
            @Value("${events.consumer.enabled:true}") boolean consumerEnabled,
            RentalViewRepository repository,
            ProjectionEventArchiveRepository archiveRepository,
            RentalProjectionProjector projector,
            ProjectionIngestionService ingestionService) {
        this.path = path;
        this.rebuildFromArchive = rebuildFromArchive;
        this.consumerEnabled = consumerEnabled;
        this.repository = repository;
        this.archiveRepository = archiveRepository;
        this.projector = projector;
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (rebuildFromArchive && consumerEnabled) {
            throw new IllegalStateException(
                "Projection rebuild requires EVENTS_CONSUMER_ENABLED=false"
            );
        }
        int imported = 0;
        if (path != null && !path.isBlank()) {
            Path source = Path.of(path);
            if (!Files.isRegularFile(source)) throw new IllegalStateException("Bootstrap file not found: " + source);
            for (String line : Files.readAllLines(source)) {
                if (!line.isBlank() && ingestionService.ingest(line)) imported++;
            }
            logger.info("Projection archive imported {} event(s) from {}", imported, source);
        }
        if (rebuildFromArchive) {
            repository.deleteAll();
            int replayed = 0;
            for (ProjectionEventArchive event : archiveRepository.findAllByOrderByReceivedAtAsc()) {
                projector.apply(event.getPayload());
                replayed++;
            }
            logger.info("Rebuilt rental_views from {} archived event(s)", replayed);
        }
    }
}
