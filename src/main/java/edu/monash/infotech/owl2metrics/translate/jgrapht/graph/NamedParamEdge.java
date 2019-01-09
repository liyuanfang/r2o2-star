package edu.monash.infotech.owl2metrics.translate.jgrapht.graph;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jgrapht.graph.DefaultEdge;

import java.util.Properties;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class NamedParamEdge extends DefaultEdge {
    private String name;
    private Properties properties;
    private long timestamp;

    public NamedParamEdge() {
        this("DEFAULT");
    }
    public NamedParamEdge(String name) {
        this.name = name;
        this.properties = new Properties();
        this.timestamp = System.nanoTime();
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
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    @Override
    public Object getSource() {
        return super.getSource();
    }
    
    @Override
    public Object getTarget() {
        return super.getTarget();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(name)
                .append(timestamp)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamedParamEdge)) {
            return false;
        }

        NamedParamEdge nobj = (NamedParamEdge) obj;
        return new EqualsBuilder()
                .append(name, nobj.name)
                .append(timestamp, nobj.timestamp)
                .isEquals();
    }

    @Override
    public String toString() {
        return name + ": " + timestamp;
    }
}
