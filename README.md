Groovy HashCode AST Library
===========================

This library provides an AST annotation for Groovy classes that provides an
`equals()` and `hashCode()` implementation based on a configurable list of
properties in the Groovy class.

One use case where this may be useful: Using the annotation on Grails Domain
classes so that domain instances can be added to Sets or Maps in a way that
makes sense (which probably means excluding the row id key and instead rely
on other data properties to determine object 'sameness').

## License

BSD Two-Clause.  See [LICENSE.txt](LICENSE.txt) and
[NOTICE_THIRD_PARTY.txt](NOTICE_THIRD_PARTY.txt).

## Usage of @LogicalEqualsAndHashCode

Example:
```
import edu.berkeley.calnet.groovy.transform.LogicalEqualsAndHashCode

@LogicalEqualsAndHashCode
class Person {
    String firstName
    String lastName
}
```

This will create an `equals()` and `hashCode()` method based on the
`firstName` and `lastName` properties of this class.  This will also add an
`implements LogicalEqualsAndHashCodeInterface` to the class and provide some
informational getters that provide back information about what properties
the AST transformer was configured with during compile time (see
[LogicalEqualsAndHashCodeInterface.java](groovy-hashcode-ast-library/src/main/java/edu/berkeley/calnet/groovy/transform/LogicalEqualsAndHashCodeInterface.java)
for the getters provided).

Then these assertions should pass as true:
```
Person obj1 = new Person(firstName: 'John', lastName: 'Smith')
Person obj2 = new Person(firstName: 'John', lastName: 'Smith')
assert obj1.hashCode() == obj2.hashCode()
assert obj1.equals(obj2)
```

`equals(obj)` will return `true` when
`(obj != null && obj instanceof LogicalEqualsAndHashCodeInterface && obj.hashCode() == hashCode())`

Note that two different class types, as long as they implement
`LogicalEqualsAndHashCodeInterface` (which the annotation adds), can be
equal as long as their hash codes are equal.

There are two optional parameters for the annotation to control which
properties are part of the `hashCode()` and `equals()` annotation.

* `excludes`
** A list of properties to exclude from `hashCode()` and `equals()`
   calculations.

Example:
```
@LogicalEqualsAndHashCode(excludes = 'excludedField')
class Person {
    String firstName
    String lastName
    String excludedField // not part of hashCode() or equals() calculations
}
```

* `includes`
** A list of properties to include in `hashCode()` and `equals()`
   calculations.

Example:
```
@LogicalEqualsAndHashCode(includes = ['firstName', 'lastName'])
class Person {
    String firstName
    String lastName
    String excludedField // not part of hashCode() or equals() calculations
}
```

*Note: You can only use one of `excludes` or `includes`, but not both.  A
compile-time error will result if both `excludes` and `includes` are
specified.*

## A Recommendation for Your Unit Tests

It is highly recommended you build unit tests in your code that confirms the
attributes that you want to be included in the `hashCode()` and `equals()`
calculations.  This will reduce confusion (and possible bugs) when you or
another programmer adds a field to the annotated class, but forgets to
consider whether they want that new attribute included or not included in
the hash code calculations.  By having a unit test for this, it forces the
consideration.

The `LogicalEqualsAndHashCodeInterface` defines a getter,
`getLogicalHashCodeProperties()`, that returns the list of properties that
are included in the `hashCode()` and `equals()` calculation.  This property
is added automatically when you use the `@LogicalEqualsAndHashCode`
annotation.

Example:
```
@LogicalEqualsAndHashCode(excludes = 'excludedField')
class Person {
    String firstName
    String lastName
    String excludedField // not part of hashCode() or equals() calculations
}
```

Then in a unit test, confirm that what you want to be included is actually
what the AST transformation included:
```
assert new Person().logicalHashCodeProperties == ['firstName', 'lastName']
```

## A Note About hashCode() Calculations
 
This implementation is not making any guarantees or contracts about what
kind of internal hashCode() algorithm is utilized by the AST transformer
that is kicked off at compile time by using the annotation on your class. 
Assumptions should not be made, except maybe in your unit tests (see
[HashCodeSpec.groovy](groovy-hashcode-ast-tests/src/test/groovy/edu/berkeley/calnet/groovy/transform/HashCodeSpec.groovy)).

The algorithm may be tweaked (improved) from version to version of this
library.

That said, the algorithm used is in the `createHashStatements()` method in
[LogicalEqualsAndHashCodeASTTransformation.java](groovy-hashcode-ast-library/src/main/java/edu/berkeley/calnet/groovy/transform/LogicalEqualsAndHashCodeASTTransformation.java).

As of this writing, the algorithm is:
```
        /**
         * (Pseudo-Code)
         * return
         *   (salts[0] * getter(logicalHashCodeProperties[0])?.hashCode())
         *   ^ ...
         *   ^
         *   (salts[N] * getter(logicalHashCodeProperties[N])?.hashCode())
         *
         * null property values equal a hash code of 0.
         *
         * Returns 0 if logicalHashCodeProperties is empty or all property
         * values are null.
         */
```
