package com.pratik.demourl.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pratik.demourl.Model.LinkRecord;

public interface LinkRecordRepository extends JpaRepository<LinkRecord, String> {
}