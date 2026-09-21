package com.kalaconnect.repository;

import com.kalaconnect.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);
}
