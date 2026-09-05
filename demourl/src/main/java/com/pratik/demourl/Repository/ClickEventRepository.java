package com.pratik.demourl.Repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pratik.demourl.Model.ClickEvent;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByLinkCode(String linkCode);
    List<ClickEvent> findByLinkCodeAndTimestampAfter(String linkCode, Instant since);
}