package com.nexevent.nexevent.repositories;

import com.nexevent.nexevent.domains.entities.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByActiveTrue(Pageable pageable);

    Page<Event> findByTitleContainingIgnoreCaseAndActiveTrue(String title, Pageable pageable);
}
