package edu.berkeley.calnet.groovy.transform

import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class HashCodeSpec extends Specification {

    void "test hash code generation"() {
        given:
        TestHash obj = new TestHash(hello1: "world1", hello2: "world2")

        when:
        int hashCode = ((LogicalEqualsAndHashCodeInterface) obj).hashCode()

        then:
        obj.logicalHashCodeIncludes == []
        obj.logicalHashCodeExcludes == []
        obj.logicalHashCodeProperties == ["hello1", "hello2"]
        hashCode == (
                (HashCodeSalts.salts[0] * obj.hello1.hashCode()) ^
                        (HashCodeSalts.salts[1] * obj.hello2.hashCode())
        )
    }

    void "test hash code generation with null fields"() {
        given:
        TestHash obj = new TestHash(hello1: "world1", hello2: null)

        when:
        int hashCode = obj.hashCode()

        then:
        hashCode == (HashCodeSalts.salts[0] * obj.hello1.hashCode())
    }

    void "test hash code generation with all null fields"() {
        given:
        LogicalEqualsAndHashCodeInterface obj = (LogicalEqualsAndHashCodeInterface) new TestHash(hello1: null, hello2: null)

        expect:
        obj.hashCode() == 0
    }

    @LogicalEqualsAndHashCode
    private static class EmptyObject {
    }

    void "test hash code generation on an empty object with no fields"() {
        given:
        LogicalEqualsAndHashCodeInterface obj1 = (LogicalEqualsAndHashCodeInterface) new EmptyObject()
        LogicalEqualsAndHashCodeInterface obj2 = (LogicalEqualsAndHashCodeInterface) new EmptyObject()

        expect:
        obj1.logicalHashCodeIncludes == []
        obj1.logicalHashCodeExcludes == []
        obj1.logicalHashCodeProperties == []
        obj2.logicalHashCodeIncludes == []
        obj2.logicalHashCodeExcludes == []
        obj2.logicalHashCodeProperties == []
        obj1.hashCode() == 0
        obj2.hashCode() == 0
        obj1.equals(obj2)
    }

    @LogicalEqualsAndHashCode(excludes = "excludedField")
    private static class TestEntryValue {
        String field1
        String field2
        String excludedField
    }

    void "test adding logically equivalent objects to maps and sets"() {
        given:
        LogicalEqualsAndHashCodeInterface val1 = (LogicalEqualsAndHashCodeInterface) new TestEntryValue(field1: "hello", field2: "world", excludedField: "ABC")
        LogicalEqualsAndHashCodeInterface val2 = (LogicalEqualsAndHashCodeInterface) new TestEntryValue(field1: "hello", field2: "world", excludedField: "DEF")
        LinkedHashMap<LogicalEqualsAndHashCodeInterface, String> map = new LinkedHashMap<LogicalEqualsAndHashCodeInterface, String>()
        LinkedHashSet<LogicalEqualsAndHashCodeInterface> hashSet = new LinkedHashSet<LogicalEqualsAndHashCodeInterface>()
        LinkedHashSet<LogicalEqualsAndHashCodeInterface> treeSet = new TreeSet<LogicalEqualsAndHashCodeInterface>()

        when:
        // add both to map
        map[val1] = "one"
        map[val2] = "two"
        // add both to hash set
        hashSet.add(val1)
        hashSet.add(val2)
        // add both to tree set
        treeSet.add(val1)
        treeSet.add(val2)


        then:
        val1.logicalHashCodeIncludes == []
        val1.logicalHashCodeExcludes == ["excludedField"]
        val1.logicalHashCodeProperties == ["field1", "field2"]
        val2.logicalHashCodeIncludes == []
        val2.logicalHashCodeExcludes == ["excludedField"]
        val2.logicalHashCodeProperties == ["field1", "field2"]

        // test for expected equality
        val1.hashCode() == val2.hashCode()
        val1.equals(val2)

        // since the logical hash code is the same and the two objects are equal, the second map put
        // should overwrite the first map put
        map.size() == 1
        // val1 and val2 have same hash code and are equal and point to same map entry
        map[val1] == "two" && map[val2] == "two"

        // with sets, same-hash-code adds are ignored, so we expect val1 to be there
        // since it was the first into the set
        hashSet.size() == 1
        System.identityHashCode(hashSet.first()) == System.identityHashCode(val1)
        treeSet.size() == 1
        System.identityHashCode(treeSet.first()) == System.identityHashCode(val1)

        // test a null equals check
        !val1.equals(null) && !val2.equals(null)
    }

    void "test that equals returns false for two logically distinct objects"() {
        given:
        LogicalEqualsAndHashCodeInterface val1 = (LogicalEqualsAndHashCodeInterface) new TestEntryValue(field1: "hello1", field2: "world1")
        LogicalEqualsAndHashCodeInterface val2 = (LogicalEqualsAndHashCodeInterface) new TestEntryValue(field1: "hello2", field2: "world2")

        expect:
        val1.hashCode() != val2.hashCode()
        !val1.equals(val2)
    }

    void "test field getters and property status for LogicalEqualsAndHashCodeInterface fields"() {
        given:
        LogicalEqualsAndHashCodeInterface val1 = (LogicalEqualsAndHashCodeInterface) new TestEntryValue(field1: "hello", field2: "world", excludedField: "ABC")

        expect:
        val1.getLogicalHashCodeIncludes() == []
        val1.getLogicalHashCodeExcludes() == ["excludedField"]
        val1.getLogicalHashCodeProperties() == ["field1", "field2"]
        // test that the interface fields rise to "property" status because they are public fields
        // with getters
        val1.hasProperty("logicalHashCodeIncludes")
        val1.hasProperty("logicalHashCodeExcludes")
        val1.hasProperty("logicalHashCodeProperties")
    }
}
