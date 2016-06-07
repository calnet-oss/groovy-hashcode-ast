package edu.berkeley.calnet.groovy.transform;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

/**
 * Partially derived from <pre>org.codehaus.groovy.transform.EqualsAndHashCodeASTTransformation</pre>.
 * <p>
 * Used in conjunction with the {@link LogicalEqualsAndHashCode} annotation
 * to add equals() and hashCode() to an annotated class based on the class's
 * properties.  Properties can be included or excluded using annotation
 * parameters.
 *
 * @author Brian Koehmstedt
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class LogicalEqualsAndHashCodeASTTransformation extends AbstractASTTransformation {
    private static final Class MY_CLASS = LogicalEqualsAndHashCode.class;
    private static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode OBJECT_TYPE = GenericsUtils.makeClassSafe(Object.class);
    private static final ClassNode STRING_TYPE = GenericsUtils.makeClassSafe(String.class);
    private static final ClassNode LIST_STRING_TYPE = GenericsUtils.makeClassSafeWithGenerics(List.class, STRING_TYPE);
    private static final ClassNode INTERFACE_TYPE = ClassHelper.make(LogicalEqualsAndHashCodeInterface.class);
    private static final ClassNode HASHCODESALTS_TYPE = ClassHelper.make(HashCodeSalts.class);
    private static final String EXCLUDES_FIELD = "logicalHashCodeExcludes";
    private static final String INCLUDES_FIELD = "logicalHashCodeIncludes";
    private static final String LOGICAL_HASHCODE_PROPS_FIELD = "logicalHashCodeProperties";
    private static final Token XOR = Token.newSymbol(Types.BITWISE_XOR, -1, -1);
    private static final Token MULT = Token.newSymbol(Types.MULTIPLY, -1, -1);

    /**
     * Main method called by the compiler to perform the AST transformation
     * at compile-time.
     */
    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            if (!checkNotInterface(cNode, MY_TYPE_NAME)) return;

            List<String> excludes = getMemberList(anno, "excludes");
            List<String> includes = getMemberList(anno, "includes");
            if (hasAnnotation(cNode, MY_TYPE)) {
                AnnotationNode canonical = cNode.getAnnotations(MY_TYPE).get(0);
                if (excludes == null || excludes.isEmpty())
                    excludes = getMemberList(canonical, "excludes");
                if (includes == null || includes.isEmpty())
                    includes = getMemberList(canonical, "includes");
            }

            // this throws an exception if annotated with both an excludes and includes parameter
            if (!checkIncludeExclude(anno, excludes, includes, MY_TYPE_NAME))
                return;

            // Need to build a list of properties in this class to include in the hash.
            // If includes is set: Property must be include list and not in optional excludes list
            // If includes is not set: Property must not be in excludes list
            List<String> propertyNodesToUse = getLogicalHashCodePropertyNames(cNode, excludes, includes);
            if (propertyNodesToUse.size() > HashCodeSalts.salts.length) {
                HashCodeSalts.ensureMaxSalts(HashCodeSalts.salts.length);
            }

            // logicalHashCodeExcludes and logicalHashCodeIncludes fields
            createIncludeExcludeFields(cNode, excludes, includes);

            // logicalHashCodeProperties field
            createLogicalHashCodePropertiesField(cNode, propertyNodesToUse);

            // hashCode()
            createHashCode(cNode, propertyNodesToUse);

            // equals()
            createEquals(cNode);

            // getters for the fields we added
            createGetter(cNode, EXCLUDES_FIELD);
            createGetter(cNode, INCLUDES_FIELD);
            createGetter(cNode, LOGICAL_HASHCODE_PROPS_FIELD);

            // add implements LogicalEqualsAndHashCodeInterface
            addInterface(cNode);
        }

    }

    private static void createIncludeExcludeFields(ClassNode cNode, List<String> excludes, List<String> includes) {
        createIncludeExcludeField(cNode, EXCLUDES_FIELD, excludes);
        createIncludeExcludeField(cNode, INCLUDES_FIELD, includes);
    }

    private static void createIncludeExcludeField(ClassNode cNode, String fieldName, List<String> list) {
        boolean hasExistingField = cNode.getDeclaredField(fieldName) != null;
        if (hasExistingField && cNode.getDeclaredField("_" + fieldName) != null)
            return;

        cNode.addField(new FieldNode(
                hasExistingField ? "_" + fieldName : fieldName,
                (hasExistingField ? ACC_PRIVATE : ACC_PUBLIC) | ACC_FINAL | ACC_STATIC,
                LIST_STRING_TYPE,
                cNode,
                arrayConstX(STRING_TYPE, list)
        ));
    }

    private static List<String> getLogicalHashCodePropertyNames(
            ClassNode cNode,
            final List<String> excludes,
            final List<String> includes
    ) {
        // Need to build a list of properties in this class to include in the hash.
        // If includes is set: Property must be include list and not in optional excludes list
        // If includes is not set: Property must not be in excludes list
        List<String> foundNames = new LinkedList<String>();
        for (PropertyNode propertyNode : cNode.getProperties()) {
            boolean eval1 = (includes == null || includes.size() == 0 || includes.contains(propertyNode.getName()));
            boolean eval2 = (excludes == null || excludes.size() == 0 || !excludes.contains(propertyNode.getName()));
            if (eval1 && eval2) {
                foundNames.add(propertyNode.getName());
            }
        }
        return foundNames;
    }

    private static FieldNode createLogicalHashCodePropertiesField(ClassNode cNode, List<String> propertyNodesToUse
    ) {
        FieldNode existing = cNode.getDeclaredField(LOGICAL_HASHCODE_PROPS_FIELD);
        if (existing != null) return existing;

        FieldNode fn = new FieldNode(
                LOGICAL_HASHCODE_PROPS_FIELD,
                ACC_PUBLIC | ACC_FINAL | ACC_STATIC,
                LIST_STRING_TYPE,
                cNode,
                arrayConstX(STRING_TYPE, propertyNodesToUse)
        );
        cNode.addField(fn);
        return fn;
    }

    private static void createHashCode(ClassNode cNode, List<String> propertyNodesToUse) {
        if (hasDeclaredMethod(cNode, "hashCode", 0)) return;

        // method body
        final BlockStatement body = new BlockStatement();
        body.addStatement(createHashStatements(propertyNodesToUse));

        // add method to class
        cNode.addMethod(new MethodNode(
                "hashCode",
                ACC_PUBLIC,
                ClassHelper.int_TYPE, // returnType
                Parameter.EMPTY_ARRAY, // parameters
                ClassNode.EMPTY_ARRAY, // exceptions
                body
        ));
    }

    private static Statement createHashStatements(List<String> propertyNodesToUse) {
        // HashCodeSalts.salts field
        FieldNode saltsFieldNode = HASHCODESALTS_TYPE.getDeclaredField("salts");
        assert saltsFieldNode.isPublic() && saltsFieldNode.isStatic();

        /**
         * Add the following code:
         * For every non-null field:
         * return (salt0 * field0) ^ ... ^ (saltN * fieldN)
         * null fields equal a hash code of 0 with no salt.
         *
         * Returns 0 if there are no fields to include in the hash.
         */

        Expression lastExpression = constX(0);
        if (DefaultGroovyMethods.asBoolean(propertyNodesToUse)) {
            int propertyIndex = 0;
            for (String propertyName : propertyNodesToUse) {
                lastExpression = xorX(
                        lastExpression,
                        ternaryX(
                                notNullX(
                                        varX(propertyName)
                                ),
                                multX(
                                        indexX(
                                                fieldX(HASHCODESALTS_TYPE, "salts"),
                                                constX(propertyIndex)
                                        ),
                                        callX(
                                                varX(propertyName),
                                                "hashCode")
                                ),
                                constX(0)
                        )
                );
                propertyIndex++;
            }

        }

        return returnS(lastExpression);
    }

    private static void createEquals(ClassNode cNode) {
        if (hasDeclaredMethod(cNode, "equals", 0)) return;

        // parameter to equals()
        VariableExpression objVar = varX("obj");

        // method body
        final BlockStatement body = new BlockStatement();
        body.addStatement(createEqualsStatements(objVar));

        // add method to class
        cNode.addMethod(new MethodNode(
                "equals",
                ACC_PUBLIC,
                ClassHelper.boolean_TYPE, // returnType
                params(param(OBJECT_TYPE, objVar.getName())), // parameters
                ClassNode.EMPTY_ARRAY, // exceptions
                body
        ));
    }

    private static Statement createEqualsStatements(VariableExpression objVar) {
        /**
         * Add the following code;
         * return (obj != null && obj.hashCode() == hashCode())
         */
        BooleanExpression notNull = notNullX(objVar);
        //BooleanExpression isInstanceOf = isInstanceOfX(objVar, cNode);
        BinaryExpression hashCodeEquals = eqX(callX(objVar, "hashCode"), callThisX("hashCode"));
        //return returnS(andX(andX(notNull, isInstanceOf), hashCodeEquals));
        return returnS(andX(notNull, hashCodeEquals));
    }

    private static void addInterface(ClassNode cNode) {
        cNode.addInterface(INTERFACE_TYPE);
    }

    private static ArrayExpression arrayConstX(ClassNode type, List constants) {
        List<Expression> constantExpressions = new ArrayList<Expression>(constants.size());
        for (Object constant : constants) {
            constantExpressions.add(constX(constant));
        }
        return new ArrayExpression(type, constantExpressions);
    }

    private static BinaryExpression xorX(Expression lhs, Expression rhs) {
        return new BinaryExpression(lhs, XOR, rhs);
    }

    private static BinaryExpression multX(Expression lhs, Expression rhs) {
        return new BinaryExpression(lhs, MULT, rhs);
    }

    private static void createGetter(
            ClassNode cNode,
            String fieldName
    ) {
        FieldNode field = cNode.getField(fieldName);
        if (field == null) return;
        String methodName = "get" + capitalize(fieldName);
        if (hasDeclaredMethod(cNode, methodName, 0)) return;

        // method body: return property;
        final BlockStatement body = new BlockStatement();
        body.addStatement(returnS(fieldX(field)));

        // add method to class
        cNode.addMethod(new MethodNode(
                methodName,
                ACC_PUBLIC,
                field.getType(), // returnType
                Parameter.EMPTY_ARRAY, // parameters
                ClassNode.EMPTY_ARRAY, // exceptions
                body
        ));
    }

    private static String capitalize(String str) {
        return (str != null && str.length() > 0 ? str.substring(0, 1).toUpperCase()
                + (str.length() > 1 ? str.substring(1) : "") : str);
    }
}
