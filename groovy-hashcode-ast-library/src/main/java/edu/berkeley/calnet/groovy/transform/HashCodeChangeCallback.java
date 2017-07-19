package edu.berkeley.calnet.groovy.transform;

/**
 * When an implementations of this interface is specified in the changeCallbackClass parameter of a LogicalEqualsAndHashCode annotation then the hashCodeChange() method will be called when hashCode() is called and a change in hash code is detected.
 */
public interface HashCodeChangeCallback {
    void hashCodeChange(LogicalEqualsAndHashCodeInterface object, int oldHashCode, int newHashCode);
}
