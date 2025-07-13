package com.mengnankk.dto;

import java.util.List;

public class SearchResult<T> {
    private List<T> items;
    private Object[] nextSearchAfter;
    public List<T> getItems() {
        return items;
    }
    public void setItems(List<T> items) {
        this.items = items;
    }
    public Object[] getNextSearchAfter() {
        return nextSearchAfter;
    }
    public void setNextSearchAfter(Object[] nextSearchAfter) {
        this.nextSearchAfter = nextSearchAfter;
    }
}

