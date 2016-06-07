package edu.berkeley.calnet.groovy.transform

import spock.lang.Specification

class HashCodeSaltsSpec extends Specification {
    void "test ensure salt quantity"() {
        given:
        int newQuantity = HashCodeSalts.salts.length + 32

        when:
        HashCodeSalts.ensureMaxSalts(newQuantity)

        then:
        // ensure the new salts got added
        for (int i = 0; i < newQuantity; i++) {
            assert HashCodeSalts.salts[i]
        }
        // ensure no salts are the same
        Arrays.sort(HashCodeSalts.salts)
        for (int i = 1; i < newQuantity; i++) {
            assert HashCodeSalts.salts[i] != HashCodeSalts.salts[i - 1]
        }
    }
}
