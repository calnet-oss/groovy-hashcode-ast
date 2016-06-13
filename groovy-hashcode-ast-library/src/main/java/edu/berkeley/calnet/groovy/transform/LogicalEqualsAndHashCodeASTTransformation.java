/**
 * <u>Third Party Notice</u>
 * <p>
 * This code is partially derived from the souce code of
 * <pre>org.codehaus.groovy.transform.EqualsAndHashCodeASTTransformation</pre>,
 * which is licensed under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache
 * License, Version 2.0</a>.
 *
 * The original <a href="https://github.com/groovy/groovy-core/blob/master/src/main/org/codehaus/groovy/transform/EqualsAndHashCodeASTTransformation.java">EqualsAndHashCodeASTTransformation.java</a>
 * is part of the Groovy source code.
 *
 * <a href="https://github.com/groovy/groovy-core/blob/master/LICENSE">Full
 * Groovy ASF 2.0 LICENSE</a>
 *
 * <a href="https://github.com/groovy/groovy-core/blob/master/NOTICE">Groovy
 * Copyright NOTICE</a>
 */

 /*
 * Copyright (c) 2016, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.calnet.groovy.transform;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

/**
 * <u>Third Party Notice</u> This code is partially derived from the souce
 * code of
 * <pre>org.codehaus.groovy.transform.EqualsAndHashCodeASTTransformation</pre>,
 * which is licensed under the Apache License, Version 2.0.  This license is
 * available: http://www.apache.org/licenses/LICENSE-2.0.  The original
 * EqualsAndHashCodeASTTransformation.java is part of the Groovy source code
 * and can be located here (as of this writing): https://github.com/groovy/groovy-core/blob/master/src/main/org/codehaus/groovy/transform/EqualsAndHashCodeASTTransformation.java
 * The full Groovy ASF 2.0 LICENSE is available here:
 * https://github.com/groovy/groovy-core/blob/master/LICENSE The Groovy
 * Copyright NOTICE is available here: https://github.com/groovy/groovy-core/blob/master/NOTICE
 *
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
    private static final ClassNode LOGICALEQUALSHASHCODE_INTERFACE_TYPE = ClassHelper.make(LogicalEqualsAndHashCodeInterface.class);
    private static final ClassNode HASHCODESALTS_TYPE = ClassHelper.make(HashCodeSalts.class);
    private static final ClassNode HASHMAP_TYPE = GenericsUtils.makeClassSafe(HashMap.class);
    private static final ClassNode INT_TYPE = GenericsUtils.makeClassSafe(Integer.class);
    private static final ClassNode BOOLEAN_TYPE = GenericsUtils.makeClassSafe(Boolean.class);
    private static final ClassNode VISITMAP_TYPE = GenericsUtils.makeClassSafeWithGenerics(HASHMAP_TYPE, new GenericsType(INT_TYPE), new GenericsType(BOOLEAN_TYPE));
    private static final ClassNode SYSTEM_TYPE = GenericsUtils.makeClassSafe(System.class);
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
        if (!hasDeclaredMethod(cNode, "__hashCode", 0)) {
            // add __hashCode() to class
            cNode.addMethod(new MethodNode(
                    "__hashCode",
                    ACC_PUBLIC,
                    ClassHelper.int_TYPE, // returnType
                    params(param(VISITMAP_TYPE, "visitMap")), // parameters
                    ClassNode.EMPTY_ARRAY, // exceptions
                    createHashStatements(cNode, propertyNodesToUse)
            ));
        }

        if (!hasDeclaredMethod(cNode, "hashCode", 0)) {
            // add hashCode() to class
            cNode.addMethod(new MethodNode(
                    "hashCode",
                    ACC_PUBLIC,
                    ClassHelper.int_TYPE, // returnType
                    Parameter.EMPTY_ARRAY, // parameters
                    ClassNode.EMPTY_ARRAY, // exceptions
                    createWrapperHashStatements(cNode)
            ));
        }
    }

    private static BlockStatement createHashStatements(ClassNode cNode, List<String> propertyNodesToUse) {
        // HashCodeSalts.salts field
        FieldNode saltsFieldNode = HASHCODESALTS_TYPE.getDeclaredField("salts");
        assert saltsFieldNode.isPublic() && saltsFieldNode.isStatic();

        /**
         * (Pseudo-Code)
         * visitMap.put(System.identityHashCode(this), Boolean.TRUE)
         * return
         *   (getter(logicalHashCodeProperties[0]) != null && !visitMap.containsKey(System.identityHashCode(getter(logicalHashCodeProperties[0])) ? salts[0] * (getter(logicalHashCodeProperties[0]) instanceof LogicalEqualsAndHashCodeInterface ? getter(logicalHashCodeProperties[0]).__hashCode(visitMap) : getter(logicalHashCodeProperties[0]).hashCode()) : 0)
         *   ^ ...
         *   ^
         *   (getter(logicalHashCodeProperties[N]) != null && !visitMap.containsKey(System.identityHashCode(getter(logicalHashCodeProperties[N])) ? salts[N] * (getter(logicalHashCodeProperties[N]) instanceof LogicalEqualsAndHashCodeInterface ? getter(logicalHashCodeProperties[N]).__hashCode(visitMap) : getter(logicalHashCodeProperties[N]).hashCode()) : 0)
         *
         * null property values equal a hash code of 0.
         *
         * Returns 0 if logicalHashCodeProperties is empty or all property
         * values are null.
         */

        final BlockStatement body = new BlockStatement();

        body.addStatement(new ExpressionStatement(callX(
                varX("visitMap"),
                "put",
                args(
                        callX(SYSTEM_TYPE, "identityHashCode", varX("this")),
                        fieldX(BOOLEAN_TYPE, "TRUE")
                )
        )));

        Expression lastExpression = constX(0);
        if (propertyNodesToUse != null && propertyNodesToUse.size() > 0) {
            int propertyIndex = 0;
            for (String propertyName : propertyNodesToUse) {
                PropertyNode pNode = cNode.getProperty(propertyName);
                assert pNode != null;
                Expression propValExpr = getterX(cNode, pNode);
                Expression notVisitedExpr = notX(callX(
                        varX("visitMap"),
                        "containsKey",
                        callX(
                                SYSTEM_TYPE,
                                "identityHashCode",
                                propValExpr
                        )
                ));
                lastExpression = xorX(
                        lastExpression,
                        ternaryX(
                                andX(
                                        notNullX(propValExpr),
                                        notVisitedExpr
                                ),
                                multX(
                                        indexX(
                                                fieldX(HASHCODESALTS_TYPE, "salts"),
                                                constX(propertyIndex)
                                        ),
                                        ternaryX(
                                                isInstanceOfX(propValExpr, MY_TYPE),
                                                callX(
                                                        propValExpr,
                                                        "__hashCode",
                                                        varX("visitMap")
                                                ),
                                                callX(
                                                        propValExpr,
                                                        "hashCode"
                                                )
                                        )
                                ),
                                constX(0)
                        )
                );
                propertyIndex++;
            }

        }

        body.addStatement(returnS(lastExpression));

        return body;
    }

    private static Statement createWrapperHashStatements(ClassNode cNode) {
        /**
         * Add the following code:
         * {@code
         * return __hashCode(new HashMap<Integer,Boolean>());
         * }
         */
        return returnS(callThisX("__hashCode", ctorX(VISITMAP_TYPE)));
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
         * return (obj != null && obj instanceof LogicalEqualsAndHashCodeInterface && obj.hashCode() == hashCode())
         */
        BooleanExpression notNull = notNullX(objVar);
        BooleanExpression isInstanceOf = isInstanceOfX(objVar, LOGICALEQUALSHASHCODE_INTERFACE_TYPE);
        BinaryExpression hashCodeEquals = eqX(callX(objVar, "hashCode"), callThisX("hashCode"));
        return returnS(andX(andX(notNull, isInstanceOf), hashCodeEquals));
    }

    private static void addInterface(ClassNode cNode) {
        cNode.addInterface(LOGICALEQUALSHASHCODE_INTERFACE_TYPE);
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
