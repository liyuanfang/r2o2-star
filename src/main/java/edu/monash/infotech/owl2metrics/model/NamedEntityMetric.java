package edu.monash.infotech.owl2metrics.model;

import com.google.common.collect.ComparisonChain;

/**
 * @author Yuan-Fang Li
 * @version $Id: NamedEntityMetric.java 65 2012-10-09 02:59:31Z yli $
 */
public class NamedEntityMetric<T extends Comparable<T>> implements Comparable<NamedEntityMetric<T>>{
    private String name;
    private T value;

    public NamedEntityMetric(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Class metric [" + name + ": " + value.toString() + "]\n";
    }

    public String[] toStringArray() {
        return new String[]{name, value.toString()};
    }

    @Override
    public int compareTo(NamedEntityMetric<T> o) {
        if (!name.equals(o.name)) {
            throw new IllegalArgumentException("Wrong metric to compare: " + name + " against " + o.name);
        }
        return ComparisonChain.start()
                .compare(this.value, o.value)
                .result();
    }
}
