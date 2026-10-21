package com.charroux.rentalQueryService.projection;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectionEventArchiveRepository extends MongoRepository<ProjectionEventArchive, String> {
    Optional<ProjectionEventArchive> findByEventId(String eventId);
    List<ProjectionEventArchive> findAllByOrderByReceivedAtAsc();
}
