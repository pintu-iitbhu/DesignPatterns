package com.rcpky.search;

import com.rcpky.model.Category;

import java.time.LocalDateTime;

public final class SearchQuery {
    private final String keyword;
    private final String city;
    private final String venueId;
    private final Category category;
    private final String artistName;
    private final LocalDateTime dateFrom;
    private final LocalDateTime dateTo;
    private final int maxResults;

    private SearchQuery(Builder b) {
        this.keyword = b.keyword;
        this.city = b.city;
        this.venueId = b.venueId;
        this.category = b.category;
        this.artistName = b.artistName;
        this.dateFrom = b.dateFrom;
        this.dateTo = b.dateTo;
        this.maxResults = b.maxResults;
    }

    public String getKeyword()          { return keyword; }
    public String getCity()             { return city; }
    public String getVenueId()          { return venueId; }
    public Category getCategory()       { return category; }
    public String getArtistName()       { return artistName; }
    public LocalDateTime getDateFrom()  { return dateFrom; }
    public LocalDateTime getDateTo()    { return dateTo; }
    public int getMaxResults()          { return maxResults; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String keyword;
        private String city;
        private String venueId;
        private Category category;
        private String artistName;
        private LocalDateTime dateFrom;
        private LocalDateTime dateTo;
        private int maxResults = 50;

        public Builder keyword(String v)        { this.keyword = v; return this; }
        public Builder city(String v)           { this.city = v; return this; }
        public Builder venueId(String v)        { this.venueId = v; return this; }
        public Builder category(Category v)     { this.category = v; return this; }
        public Builder artistName(String v)     { this.artistName = v; return this; }
        public Builder dateFrom(LocalDateTime v){ this.dateFrom = v; return this; }
        public Builder dateTo(LocalDateTime v)  { this.dateTo = v; return this; }
        public Builder maxResults(int v)        { this.maxResults = v; return this; }

        public SearchQuery build() { return new SearchQuery(this); }
    }
}
