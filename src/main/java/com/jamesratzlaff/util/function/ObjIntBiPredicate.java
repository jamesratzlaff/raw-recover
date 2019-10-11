/**
 *
 */
package com.jamesratzlaff.util.function;

import java.util.Objects;

/**
 * @author
 *
 */
@FunctionalInterface
public interface ObjIntBiPredicate<T> {

    boolean test(T t, int u);

    default ObjIntBiPredicate<T> and(ObjIntBiPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (T t, int u) -> test(t, u) && other.test(t, u);
    }

    default ObjIntBiPredicate<T> neg() {
        return (T t, int u) -> !test(t, u);
    }

    default ObjIntBiPredicate<T> or(ObjIntBiPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (T t, int u) -> test(t, u) || other.test(t, u);
    }


}
