package edu.berkeley.calnet.groovy.transform;

import java.util.List;

public interface LogicalEqualsAndHashCodeInterface {
    public List<String> getLogicalHashCodeExcludes();

    public List<String> getLogicalHashCodeIncludes();

    public List<String> getLogicalHashCodeProperties();
}
