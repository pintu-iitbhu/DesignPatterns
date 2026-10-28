package com.rcpky.search;

import com.rcpky.model.Category;
import com.rcpky.model.Event;
import com.rcpky.model.EventStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory inverted index for event discovery.
 * Search results may be slightly stale (eventually consistent);
 * the booking service always re-validates inventory before confirming.
 */
public class EventSearchIndex {

    private final Map<String, Event>        eventStore     = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>  termIndex      = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>  cityIndex      = new ConcurrentHashMap<>();
    private final Map<Category, Set<String>> categoryIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>  artistIndex    = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>  venueIndex     = new ConcurrentHashMap<>();

    public synchronized void index(Event event) {
        String eid = event.getId();
        eventStore.put(eid, event);

        tokenize(event.getName()).forEach(t ->
            termIndex.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(eid));
        tokenize(event.getDescription()).forEach(t ->
            termIndex.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(eid));

        cityIndex.computeIfAbsent(event.getCity().toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(eid);

        categoryIndex.computeIfAbsent(event.getCategory(), k -> ConcurrentHashMap.newKeySet()).add(eid);

        event.getArtists().forEach(a -> {
            String key = a.toLowerCase();
            artistIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(eid);
            tokenize(a).forEach(t ->
                artistIndex.computeIfAbsent(t, k -> ConcurrentHashMap.newKeySet()).add(eid));
        });

        venueIndex.computeIfAbsent(event.getVenue().getId(), k -> ConcurrentHashMap.newKeySet()).add(eid);
    }

    public synchronized void remove(String eventId) {
        Event event = eventStore.remove(eventId);
        if (event == null) return;

        tokenize(event.getName()).forEach(t -> removeFrom(termIndex, t, eventId));
        tokenize(event.getDescription()).forEach(t -> removeFrom(termIndex, t, eventId));
        removeFrom(cityIndex, event.getCity().toLowerCase(), eventId);
        Set<String> catSet = categoryIndex.get(event.getCategory());
        if (catSet != null) catSet.remove(eventId);
        event.getArtists().forEach(a -> {
            removeFrom(artistIndex, a.toLowerCase(), eventId);
            tokenize(a).forEach(t -> removeFrom(artistIndex, t, eventId));
        });
        removeFrom(venueIndex, event.getVenue().getId(), eventId);
    }

    public List<Event> search(SearchQuery query) {
        Set<String> candidates = null;

        if (isPresent(query.getKeyword())) {
            Set<String> hits = new HashSet<>();
            boolean first = true;
            for (String term : tokenize(query.getKeyword())) {
                Set<String> m = termIndex.getOrDefault(term, Collections.emptySet());
                if (first) { hits.addAll(m); first = false; }
                else        hits.retainAll(m);
            }
            candidates = intersect(candidates, hits);
        }

        if (query.getCategory() != null) {
            candidates = intersect(candidates,
                categoryIndex.getOrDefault(query.getCategory(), Collections.emptySet()));
        }

        if (isPresent(query.getCity())) {
            candidates = intersect(candidates,
                cityIndex.getOrDefault(query.getCity().toLowerCase(), Collections.emptySet()));
        }

        if (isPresent(query.getArtistName())) {
            Set<String> hits = new HashSet<>();
            for (String term : tokenize(query.getArtistName())) {
                hits.addAll(artistIndex.getOrDefault(term, Collections.emptySet()));
            }
            candidates = intersect(candidates, hits);
        }

        if (isPresent(query.getVenueId())) {
            candidates = intersect(candidates,
                venueIndex.getOrDefault(query.getVenueId(), Collections.emptySet()));
        }

        if (candidates == null) candidates = new HashSet<>(eventStore.keySet());

        final Set<String> finalCandidates = candidates;
        return finalCandidates.stream()
            .map(eventStore::get)
            .filter(Objects::nonNull)
            .filter(e -> e.getStatus() != EventStatus.CANCELLED)
            .filter(e -> matchesDateRange(e, query.getDateFrom(), query.getDateTo()))
            .sorted(Comparator.comparing(Event::getDateTime))
            .limit(query.getMaxResults())
            .collect(Collectors.toList());
    }

    public int size() { return eventStore.size(); }

    private Set<String> intersect(Set<String> existing, Set<String> next) {
        Set<String> copy = new HashSet<>(next);
        if (existing == null) return copy;
        existing.retainAll(copy);
        return existing;
    }

    private boolean matchesDateRange(Event e, LocalDateTime from, LocalDateTime to) {
        if (from != null && e.getDateTime().isBefore(from)) return false;
        if (to   != null && e.getDateTime().isAfter(to))   return false;
        return true;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        Set<String> tokens = new HashSet<>();
        for (String t : text.toLowerCase().split("[^a-z0-9]+")) {
            if (t.length() >= 2) tokens.add(t);
        }
        return tokens;
    }

    private void removeFrom(Map<String, Set<String>> index, String key, String eventId) {
        Set<String> s = index.get(key);
        if (s != null) s.remove(eventId);
    }

    private boolean isPresent(String s) { return s != null && !s.isEmpty(); }
}
