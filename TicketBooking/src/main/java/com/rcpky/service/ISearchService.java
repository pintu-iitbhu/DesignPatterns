package com.rcpky.service;

import com.rcpky.model.Event;
import com.rcpky.search.SearchQuery;

import java.util.List;

public interface ISearchService {
    List<Event> search(SearchQuery query);
    void indexEvent(Event event);
    void removeEvent(String eventId);
}
