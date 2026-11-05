package edu.arizona.engagement.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Listings are bounded: a page of at most 100 rows, newest first, so a popular game cannot make one response huge. */
public final class Paging {

    public static final String DEFAULT_SIZE = "50";
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static Pageable newestFirst(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }
}
