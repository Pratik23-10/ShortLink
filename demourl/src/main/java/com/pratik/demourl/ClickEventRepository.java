package com.pratik.demourl;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByLinkCode(String linkCode);
    List<ClickEvent> findByLinkCodeAndTimestampAfter(String linkCode, Instant since);
}