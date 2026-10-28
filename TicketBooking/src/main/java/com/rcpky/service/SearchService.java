package com.rcpky.service;

import com.rcpky.model.Event;
import com.rcpky.search.EventSearchIndex;
import com.rcpky.search.SearchQuery;

import java.util.List;

/**
 * Thin facade over EventSearchIndex.
 * Queries return eventually-consistent results; booking always re-validates inventory.
 */
public class SearchService implements ISearchService {

    private final EventSearchIndex index;

    public SearchService(EventSearchIndex index) { this.index = index; }

    @Override public List<Event> search(SearchQuery query) { return index.search(query); }
    @Override public void indexEvent(Event event)          { index.index(event); }
    @Override public void removeEvent(String eventId)      { index.remove(eventId); }
}
