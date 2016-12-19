package edu.monash.infotech.owl2metrics.translate.jgrapht.graph;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Properties;
import java.util.SortedSet;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class NamedNode implements Comparable<NamedNode> {
    private String name;
    private Properties properties;
    private SortedSet<String> types;

    private NamedNode() {
    }
    
    public NamedNode(String name) {
        this.name = name;
        this.types = Sets.newTreeSet();
        this.properties = new Properties();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    public Properties setProperty(String key, String value) {
        properties.setProperty(key, value);
        return properties;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(name).
                append(types.toArray()).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamedNode)) {
            return false;
        }

        NamedNode nobj = (NamedNode) obj;
        return new EqualsBuilder().
                append(name, nobj.name).
                append(types.toArray(), types.toArray()).
                isEquals();
    }

    @Override
    public String toString() {
        return name + ":" + types.toString();
    }

    @Override
    public int compareTo(NamedNode o) {
        return ComparisonChain.start()
                .compare(this.name, o.name)
                .result();
    }

    public SortedSet<String> getTypes() {
        return types;
    }

    public void setTypes(SortedSet<String> types) {
        this.types = types;
    }
}
