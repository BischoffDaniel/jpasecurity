/*
 * Copyright 2008 - 2011 Arne Limburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package net.sf.jpasecurity.jpql.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import net.sf.jpasecurity.ExceptionFactory;
import net.sf.jpasecurity.jpql.JpqlCompiledStatement;
import net.sf.jpasecurity.jpql.parser.JpqlAbs;
import net.sf.jpasecurity.jpql.parser.JpqlAdd;
import net.sf.jpasecurity.jpql.parser.JpqlAnd;
import net.sf.jpasecurity.jpql.parser.JpqlBetween;
import net.sf.jpasecurity.jpql.parser.JpqlBooleanLiteral;
import net.sf.jpasecurity.jpql.parser.JpqlBrackets;
import net.sf.jpasecurity.jpql.parser.JpqlConcat;
import net.sf.jpasecurity.jpql.parser.JpqlCurrentDate;
import net.sf.jpasecurity.jpql.parser.JpqlCurrentTime;
import net.sf.jpasecurity.jpql.parser.JpqlCurrentTimestamp;
import net.sf.jpasecurity.jpql.parser.JpqlDecimalLiteral;
import net.sf.jpasecurity.jpql.parser.JpqlDivide;
import net.sf.jpasecurity.jpql.parser.JpqlEquals;
import net.sf.jpasecurity.jpql.parser.JpqlEscapeCharacter;
import net.sf.jpasecurity.jpql.parser.JpqlExists;
import net.sf.jpasecurity.jpql.parser.JpqlFrom;
import net.sf.jpasecurity.jpql.parser.JpqlGreaterOrEquals;
import net.sf.jpasecurity.jpql.parser.JpqlGreaterThan;
import net.sf.jpasecurity.jpql.parser.JpqlGroupBy;
import net.sf.jpasecurity.jpql.parser.JpqlHaving;
import net.sf.jpasecurity.jpql.parser.JpqlIdentificationVariable;
import net.sf.jpasecurity.jpql.parser.JpqlIdentifier;
import net.sf.jpasecurity.jpql.parser.JpqlIn;
import net.sf.jpasecurity.jpql.parser.JpqlIntegerLiteral;
import net.sf.jpasecurity.jpql.parser.JpqlIsEmpty;
import net.sf.jpasecurity.jpql.parser.JpqlIsNull;
import net.sf.jpasecurity.jpql.parser.JpqlLength;
import net.sf.jpasecurity.jpql.parser.JpqlLessOrEquals;
import net.sf.jpasecurity.jpql.parser.JpqlLessThan;
import net.sf.jpasecurity.jpql.parser.JpqlLike;
import net.sf.jpasecurity.jpql.parser.JpqlLocate;
import net.sf.jpasecurity.jpql.parser.JpqlLower;
import net.sf.jpasecurity.jpql.parser.JpqlMemberOf;
import net.sf.jpasecurity.jpql.parser.JpqlMod;
import net.sf.jpasecurity.jpql.parser.JpqlMultiply;
import net.sf.jpasecurity.jpql.parser.JpqlNamedInputParameter;
import net.sf.jpasecurity.jpql.parser.JpqlNegative;
import net.sf.jpasecurity.jpql.parser.JpqlNot;
import net.sf.jpasecurity.jpql.parser.JpqlNotEquals;
import net.sf.jpasecurity.jpql.parser.JpqlOr;
import net.sf.jpasecurity.jpql.parser.JpqlOrderBy;
import net.sf.jpasecurity.jpql.parser.JpqlPath;
import net.sf.jpasecurity.jpql.parser.JpqlPositionalInputParameter;
import net.sf.jpasecurity.jpql.parser.JpqlSelectClause;
import net.sf.jpasecurity.jpql.parser.JpqlSize;
import net.sf.jpasecurity.jpql.parser.JpqlSqrt;
import net.sf.jpasecurity.jpql.parser.JpqlStringLiteral;
import net.sf.jpasecurity.jpql.parser.JpqlSubselect;
import net.sf.jpasecurity.jpql.parser.JpqlSubstring;
import net.sf.jpasecurity.jpql.parser.JpqlSubtract;
import net.sf.jpasecurity.jpql.parser.JpqlTrim;
import net.sf.jpasecurity.jpql.parser.JpqlTrimBoth;
import net.sf.jpasecurity.jpql.parser.JpqlTrimCharacter;
import net.sf.jpasecurity.jpql.parser.JpqlTrimLeading;
import net.sf.jpasecurity.jpql.parser.JpqlTrimTrailing;
import net.sf.jpasecurity.jpql.parser.JpqlUpper;
import net.sf.jpasecurity.jpql.parser.JpqlVisitorAdapter;
import net.sf.jpasecurity.jpql.parser.Node;

/**
 * This implementation of the {@link JpqlVisitorAdapter} evaluates queries in memory,
 * storing the result in the specified {@link QueryEvaluationParameters}.
 * If the evaluation cannot be performed due to missing information the result is set to <quote>undefined</quote>.
 * To evaluate subselect-query, pluggable implementations of {@link SubselectEvaluator} are used.
 * @author Arne Limburg
 */
public class QueryEvaluator extends JpqlVisitorAdapter<QueryEvaluationParameters> {

    public static final int DECIMAL_PRECISION = 100;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final JpqlCompiler compiler;
    private final PathEvaluator pathEvaluator;
    private final ExceptionFactory exceptionFactory;
    private final SubselectEvaluator[] subselectEvaluators;

    public QueryEvaluator(JpqlCompiler compiler,
                             PathEvaluator pathEvaluator,
                             ExceptionFactory exceptionFactory,
                             SubselectEvaluator... subselectEvaluators) {
        if (compiler == null) {
            throw new IllegalArgumentException("compiler may not be null");
        }
        if (pathEvaluator == null) {
            throw new IllegalArgumentException("pathEvaluator may not be null");
        }
        if (exceptionFactory == null) {
            throw new IllegalArgumentException("exceptionFactory may not be null");
        }
        this.compiler = compiler;
        this.pathEvaluator = pathEvaluator;
        this.exceptionFactory = exceptionFactory;
        this.subselectEvaluators = subselectEvaluators;
        for (SubselectEvaluator subselectEvaluator: subselectEvaluators) {
            subselectEvaluator.setQueryEvaluator(this);
        }
    }

    public boolean canEvaluate(Node node, QueryEvaluationParameters parameters) {
        try {
            evaluate(node, parameters);
            return true;
        } catch (NotEvaluatableException e) {
            return false;
        }
    }

    public <R> R evaluate(Node node, QueryEvaluationParameters<R> parameters) throws NotEvaluatableException {
        node.visit(this, parameters);
        return parameters.getResult();
    }

    public boolean visit(JpqlSelectClause node, QueryEvaluationParameters data) {
        data.setResultUndefined();
        return false;
    }

    public boolean visit(JpqlFrom node, QueryEvaluationParameters data) {
        data.setResultUndefined();
        return false;
    }

    public boolean visit(JpqlGroupBy node, QueryEvaluationParameters data) {
        data.setResultUndefined();
        return false;
    }

    public boolean visit(JpqlHaving node, QueryEvaluationParameters data) {
        data.setResultUndefined();
        return false;
    }

    public boolean visit(JpqlOrderBy node, QueryEvaluationParameters data) {
        data.setResultUndefined();
        return false;
    }

    public boolean visit(JpqlPath node, QueryEvaluationParameters data) {
        try {
            node.jjtGetChild(0).visit(this, data);
            String path = node.toString();
            int index = path.indexOf('.');
            if (index != -1) {
                path = path.substring(index + 1);
                PathEvaluator pathEvaluator = new MappedPathEvaluator(data.getMappingInformation(), exceptionFactory);
                Collection<Object> result = pathEvaluator.evaluateAll(Collections.singleton(data.getResult()), path);
                if (result.size() == 0) {
                    data.setResult(null);
                } else if (result.size() == 1) {
                    data.setResult(result.iterator().next());
                } else {
                    data.setResult(result);
                }
            }
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
        }
        return false;
    }

    public boolean visit(JpqlOr node, QueryEvaluationParameters data) {
        boolean undefined = false;
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).visit(this, data);
            try {
                if ((Boolean)data.getResult()) {
                    //The result is true, when we return here it stays true
                    return false;
                }
            } catch (NotEvaluatableException e) {
                undefined = true;
            }
        }
        if (undefined) {
            data.setResultUndefined();
        } else {
            data.setResult(Boolean.FALSE);
        }
        return false;
    }

    public boolean visit(JpqlAnd node, QueryEvaluationParameters data) {
        boolean undefined = false;
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).visit(this, data);
            try {
                if (!(Boolean)data.getResult()) {
                    //The result is false, when we return here it stays false
                    return false;
                }
            } catch (NotEvaluatableException e) {
                undefined = true;
            }
        }
        if (undefined) {
            data.setResultUndefined();
        } else {
            data.setResult(Boolean.TRUE);
        }
        return false;
    }

    public boolean visit(JpqlNot node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        node.jjtGetChild(0).visit(this, data);
        try {
            data.setResult(!((Boolean)data.getResult()));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlBetween node, QueryEvaluationParameters data) {
        validateChildCount(node, 3);
        try {
            node.jjtGetChild(0).visit(this, data);
            Comparable value = (Comparable)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Comparable lower;
            try {
                lower = (Comparable)data.getResult();
            } catch (NotEvaluatableException e) {
                lower = null;
            }
            node.jjtGetChild(2).visit(this, data);
            Comparable upper;
            try {
                upper = (Comparable)data.getResult();
            } catch (NotEvaluatableException e) {
                upper = null;
            }
            if ((lower != null && lower.compareTo(value) > 0)
             || (upper != null && upper.compareTo(value) < 0)) {
                data.setResult(false);
            } else if (lower == null || upper == null) {
                data.setResultUndefined();
            } else {
                data.setResult(true);
            }
        } catch (ClassCastException e) {
            data.setResultUndefined();
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlIn node, QueryEvaluationParameters data) {
        Object value;
        try {
            node.jjtGetChild(0).visit(this, data);
            value = data.getResult();
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
            return false;
        }
        boolean undefined = false;
        Collection<Object> values = new ArrayList<Object>();
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).visit(this, data);
            try {
                if (data.getResult() instanceof Collection) {
                    values.addAll((Collection)data.getResult());
                } else {
                    values.add(data.getResult());
                }
            } catch (NotEvaluatableException e) {
                undefined = true;
            }
        }
        if (values.contains(value)) {
            data.setResult(Boolean.TRUE);
        } else if (undefined) {
            data.setResultUndefined();
        } else {
            data.setResult(Boolean.FALSE);
        }
        return false;
    }

    public boolean visit(JpqlLike node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            String text = (String)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            String pattern = (String)data.getResult();
            data.setResult(text.matches(createRegularExpression(pattern)));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    private String createRegularExpression(String pattern) {
        StringBuilder regularExpressionBuilder = new StringBuilder();
        int index = 0;
        int specialCharacterIndex = indexOfSpecialCharacter(pattern, index);
        appendSubPattern(regularExpressionBuilder, pattern, index, specialCharacterIndex);
        while (specialCharacterIndex < pattern.length()) {
            index = specialCharacterIndex;
            if (pattern.charAt(index) == '\\') {
                index++;
            }
            if (pattern.charAt(index) == '_') {
                regularExpressionBuilder.append('.');
                index++;
            } else if (pattern.charAt(index) == '%') {
                regularExpressionBuilder.append(".*");
                index++;
            } else {
                throw new IllegalStateException();
            }
            specialCharacterIndex = indexOfSpecialCharacter(pattern, index);
            appendSubPattern(regularExpressionBuilder, pattern, index, specialCharacterIndex);
        }
        return regularExpressionBuilder.toString();
    }

    /**
     * Returns the index of the next special character within the specified pattern
     * starting at the specified index or the length of the pattern, if no special character is present.
     */
    private int indexOfSpecialCharacter(String pattern, int startIndex) {
        int i1 = pattern.indexOf("\\_", startIndex);
        int i2 = pattern.indexOf("_", startIndex);
        int i3 = pattern.indexOf("\\%", startIndex);
        int i4 = pattern.indexOf("%", startIndex);
        int min = pattern.length();
        if (i1 > -1 && i1 < min) {
            min = i1;
        }
        if (i2 > -1 && i2 < min) {
            min = i2;
        }
        if (i3 > -1 && i3 < min) {
            min = i3;
        }
        if (i4 > -1 && i4 < min) {
            min = i4;
        }
        return min;
    }

    private void appendSubPattern(StringBuilder regularExpression, String pattern, int startIndex, int endIndex) {
        String subpattern = pattern.substring(startIndex, endIndex);
        if (subpattern.length() > 0) {
            regularExpression.append("\\Q").append(subpattern).append("\\E");
        }
    }

    public boolean visit(JpqlIsNull node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(data.getResult() == null);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlIsEmpty node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            Collection result = (Collection)data.getResult();
            data.setResult(result == null || result.isEmpty());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlMemberOf node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Object value = data.getResult();
            node.jjtGetChild(1).visit(this, data);
            data.setResult(((Collection)data.getResult()).contains(value));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlEquals node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Object value1 = data.getResult();
            if (value1 == null) {
                data.setResult(false);
            } else {
                node.jjtGetChild(1).visit(this, data);
                Object value2 = data.getResult();
                data.setResult(value1.equals(value2));
            }
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlNotEquals node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Object value1 = data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Object value2 = data.getResult();
            data.setResult(!value1.equals(value2));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlGreaterThan node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Comparable value1 = (Comparable)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Comparable value2 = (Comparable)data.getResult();
            data.setResult(value1.compareTo(value2) > 0);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlGreaterOrEquals node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Comparable value1 = (Comparable)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Comparable value2 = (Comparable)data.getResult();
            data.setResult(value1.compareTo(value2) >= 0);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlLessThan node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Comparable value1 = (Comparable)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Comparable value2 = (Comparable)data.getResult();
            data.setResult(value1.compareTo(value2) < 0);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlLessOrEquals node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            Comparable value1 = (Comparable)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            Comparable value2 = (Comparable)data.getResult();
            data.setResult(value1.compareTo(value2) <= 0);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlAdd node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            BigDecimal value1 = new BigDecimal(data.getResult().toString());
            node.jjtGetChild(1).visit(this, data);
            BigDecimal value2 = new BigDecimal(data.getResult().toString());
            data.setResult(value1.add(value2));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlSubtract node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            BigDecimal value1 = new BigDecimal(data.getResult().toString());
            node.jjtGetChild(1).visit(this, data);
            BigDecimal value2 = new BigDecimal(data.getResult().toString());
            data.setResult(value1.subtract(value2));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlMultiply node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            BigDecimal value1 = new BigDecimal(data.getResult().toString());
            node.jjtGetChild(1).visit(this, data);
            BigDecimal value2 = new BigDecimal(data.getResult().toString());
            data.setResult(value1.multiply(value2));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlDivide node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            BigDecimal value1 = new BigDecimal(data.getResult().toString());
            node.jjtGetChild(1).visit(this, data);
            BigDecimal value2 = new BigDecimal(data.getResult().toString());
            data.setResult(value1.divide(value2, DECIMAL_PRECISION, ROUNDING_MODE));
        } catch (ArithmeticException e) {
            data.setResultUndefined();
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlNegative node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            BigDecimal value = new BigDecimal(data.getResult().toString());
            data.setResult(value.negate());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlConcat node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            String value1 = (String)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            String value2 = (String)data.getResult();
            data.setResult(value1 + value2);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlSubstring node, QueryEvaluationParameters data) {
        validateChildCount(node, 3);
        try {
            node.jjtGetChild(0).visit(this, data);
            String text = (String)data.getResult();
            node.jjtGetChild(1).visit(this, data);
            int fromIndex = new BigDecimal(data.getResult().toString()).intValue();
            node.jjtGetChild(2).visit(this, data);
            int toIndex = new BigDecimal(data.getResult().toString()).intValue();
            data.setResult(text.substring(fromIndex, toIndex));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlTrim node, QueryEvaluationParameters data) {
        validateChildCount(node, 1, 3);
        try {
            boolean leading = true;
            boolean trailing = true;
            boolean trimSpecificationPresent = false;
            if (node.jjtGetChild(0) instanceof JpqlTrimLeading) {
                trailing = false;
                trimSpecificationPresent = true;
            } else if (node.jjtGetChild(0) instanceof JpqlTrimTrailing) {
                leading = false;
                trimSpecificationPresent = true;
            } else if (node.jjtGetChild(0) instanceof JpqlTrimBoth) {
                trimSpecificationPresent = true;
            }
            char trimCharacter = ' ';
            if (trimSpecificationPresent && node.jjtGetNumChildren() == 3) {
                node.jjtGetChild(1).visit(this, data);
                trimCharacter = ((String)data.getResult()).charAt(0);
            } else if (!trimSpecificationPresent && node.jjtGetNumChildren() == 2) {
                node.jjtGetChild(0).visit(this, data);
                trimCharacter = ((String)data.getResult()).charAt(0);
            }
            node.jjtGetChild(node.jjtGetNumChildren() - 1).visit(this, data);
            String text = (String)data.getResult();
            StringBuilder builder = new StringBuilder(text);
            if (leading) {
                while (builder.charAt(0) == trimCharacter) {
                    builder.deleteCharAt(0);
                }
            }
            if (trailing) {
                while (builder.charAt(builder.length() - 1) == trimCharacter) {
                    builder.deleteCharAt(builder.length() - 1);
                }
            }
            data.setResult(builder.toString());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlLower node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(data.getResult().toString().toLowerCase());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlUpper node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(data.getResult().toString().toUpperCase());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlLength node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(data.getResult().toString().length());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlLocate node, QueryEvaluationParameters data) {
        validateChildCount(node, 2, 3);
        try {
            node.jjtGetChild(0).visit(this, data);
            String text = data.getResult().toString();
            node.jjtGetChild(1).visit(this, data);
            String substring = data.getResult().toString();
            int start = 0;
            if (node.jjtGetNumChildren() == 3) {
                node.jjtGetChild(2).visit(this, data);
                start = new BigInteger(data.getResult().toString()).intValue();
            }
            data.setResult(text.indexOf(substring, start));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlAbs node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(new BigDecimal(data.getResult().toString()).abs());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlSqrt node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(Math.sqrt(new BigDecimal(data.getResult().toString()).doubleValue()));
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlMod node, QueryEvaluationParameters data) {
        validateChildCount(node, 2);
        try {
            node.jjtGetChild(0).visit(this, data);
            int i1 = Integer.parseInt(data.getResult().toString());
            node.jjtGetChild(1).visit(this, data);
            int i2 = Integer.parseInt(data.getResult().toString());
            data.setResult(i1 % i2);
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlSize node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(((Collection)data.getResult()).size());
        } catch (NotEvaluatableException e) {
            //result is undefined, which is ok here
        }
        return false;
    }

    public boolean visit(JpqlBrackets node, QueryEvaluationParameters data) {
        validateChildCount(node, 1);
        node.jjtGetChild(0).visit(this, data);
        return false;
    }

    public boolean visit(JpqlCurrentDate node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(new java.sql.Date(new Date().getTime()));
        return false;
    }

    public boolean visit(JpqlCurrentTime node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(new Time(new Date().getTime()));
        return false;
    }

    public boolean visit(JpqlCurrentTimestamp node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(new Timestamp(new Date().getTime()));
        return false;
    }

    public boolean visit(JpqlIdentifier node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        try {
            data.setResult(data.getAliasValue(node.getValue()));
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
        }
        return false;
    }

    public boolean visit(JpqlIdentificationVariable node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        try {
            data.setResult(data.getAliasValue(node.getValue()));
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
        }
        return false;
    }

    public boolean visit(JpqlIntegerLiteral node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(new BigDecimal(node.getValue()));
        return false;
    }

    public boolean visit(JpqlDecimalLiteral node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(new BigDecimal(node.getValue()));
        return false;
    }

    public boolean visit(JpqlBooleanLiteral node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(Boolean.valueOf(node.getValue()));
        return false;
    }

    public boolean visit(JpqlStringLiteral node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(node.getValue().substring(1, node.getValue().length() - 1)); //trim quotes
        return false;
    }

    public boolean visit(JpqlNamedInputParameter node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        try {
            data.setResult(data.getNamedParameterValue(node.getValue()));
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
        }
        return false;
    }

    public boolean visit(JpqlPositionalInputParameter node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        try {
            data.setResult(data.getPositionalParameterValue(Integer.parseInt(node.getValue())));
        } catch (NotEvaluatableException e) {
            data.setResultUndefined();
        }
        return false;
    }

    public boolean visit(JpqlEscapeCharacter node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(node.getValue());
        return false;
    }

    public boolean visit(JpqlTrimCharacter node, QueryEvaluationParameters data) {
        validateChildCount(node, 0);
        data.setResult(node.getValue().substring(1, node.getValue().length() - 1)); //trim quotes
        return false;
    }

    public boolean visit(JpqlExists node, QueryEvaluationParameters data) {
        try {
            node.jjtGetChild(0).visit(this, data);
            data.setResult(!((Collection)data.getResult()).isEmpty());
        } catch (NotEvaluatableException e) {
            //result is undefined
        }
        return false;
    }

    public boolean visit(JpqlSubselect node, QueryEvaluationParameters data) {
        JpqlCompiledStatement subselect = compiler.compile(node);
        for (SubselectEvaluator subselectEvaluator: subselectEvaluators) {
            try {
                data.setResult(subselectEvaluator.evaluate(subselect, data));
                return false;
            } catch (NotEvaluatableException e) {
                data.setResultUndefined();
            }
        }
        return false;
    }

    private void validateChildCount(Node node, int childCount) {
        if (node.jjtGetNumChildren() != childCount) {
            throw new IllegalStateException("node " + node.getClass().getName() + " must have " + childCount + " children");
        }
    }

    private void validateChildCount(Node node, int minChildCount, int maxChildCount) {
        if (node.jjtGetNumChildren() < minChildCount) {
            throw new IllegalStateException("node " + node.getClass().getName() + " must have at least " + minChildCount + " children");
        } else if (node.jjtGetNumChildren() > maxChildCount) {
            throw new IllegalStateException("node " + node.getClass().getName() + " must have at most " + maxChildCount + " children");
        }
    }
}
