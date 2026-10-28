package com.rcpky.repository;

import com.rcpky.model.Section;

import java.util.Optional;

public interface ISectionRepository {
    void save(Section section);
    Optional<Section> findById(String sectionId);
}
