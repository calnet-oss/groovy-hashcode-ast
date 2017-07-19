package edu.berkeley.calnet.groovy.transform

class TestHashCodeChangeCallback implements HashCodeChangeCallback {
    HashCodeChangeCallback delegate

    @Override
    void hashCodeChange(LogicalEqualsAndHashCodeInterface object, int oldHashCode, int newHashCode) {
        delegate?.hashCodeChange(object, oldHashCode, newHashCode)
    }
}
