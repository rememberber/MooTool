package com.luoboduner.moo.tool.util;

import java.util.List;
import java.util.function.Predicate;

public class ListUtils {

    /**
     * Returns the count of elements in the list that match the given predicate.
     *
     * @param list      the list to be checked
     * @param predicate the condition to be matched
     * @param <T>       the type of elements in the list
     * @return the count of matching elements
     */
    public static <T> long matchCount(List<T> list, Predicate<T> predicate) {
        return list.stream().filter(predicate).count();
    }
}