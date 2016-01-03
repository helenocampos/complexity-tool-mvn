/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.complexity.tool.mvn;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.type.Type;
import com.mycompany.complexity.tool.mvn.Nodes.IfNode;
import com.mycompany.complexity.tool.mvn.Nodes.Node;
import com.mycompany.complexity.tool.mvn.refactorer.ConditionRefactorer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author helenocampos
 */
public abstract class PatternAnalysis {

    static HashMap<String, Type> variablesTable;

    //TODO: make sure variable being analysed is not changed throughout the code

    public static boolean analize(LinkedList<Node> conditionals, ArrayList<Node> nodes, HashMap<String, Type> variables) {
        variablesTable = variables;
        normalizeConditinals(conditionals);
        return checkPattern(conditionals, nodes);
    }

    //method to normalize conditionals
    //left side of expression always a variable
    //right side can be a variable or a value
    private static void normalizeConditinals(LinkedList<Node> conditionals) {
        for (Node node : conditionals) {
            // all the nodes are IFs, safety checking anyway
            if (node.getBaseStatement() instanceof IfStmt) {
                IfStmt ifstmt = (IfStmt) node.getBaseStatement();
                BinaryExpr conditionExpression = ConditionRefactorer.getBinaryCondition(ifstmt.getCondition());

                if (conditionExpression != null) {
                    if (!(conditionExpression.getLeft() instanceof NameExpr) && (conditionExpression.getRight() instanceof NameExpr)) {    // left side of the condition expression is not a variable and right is, thus need to be inverted
                        Expression originalLeft = conditionExpression.getLeft();
                        Expression originalRight = conditionExpression.getRight();
                        conditionExpression.setLeft(originalRight);
                        conditionExpression.setRight(originalLeft);
                        conditionExpression.setOperator(invertOperator(conditionExpression.getOperator()));

                    }
                }

            }
        }
    }

    private static Operator invertOperator(Operator operator) {
        switch (operator) {
            case less:
                return Operator.greater;
            case lessEquals:
                return Operator.greaterEquals;
            case greater:
                return Operator.less;
            case greaterEquals:
                return Operator.lessEquals;
            default:
                return operator;
        }
    }

    private static LinkedList<ConditionsChain> getConditionClusters(LinkedList<Node> conditionals) {
        Collections.sort(conditionals);
        Stack<Node> conditionalsStack = linkedListToStack(conditionals);
        LinkedList<ConditionsChain> globalIntervals = new LinkedList<>();
//        LinkedList<Node> localIntervals;
        ConditionsChain localIntervals;

        while (!conditionalsStack.isEmpty()) {
            localIntervals = new ConditionsChain();
            Node actualCondition = conditionalsStack.pop();

            boolean first = true;
            while (continueToRightNode((IfNode) actualCondition)) {
                if (first) {
                    localIntervals.addCondition(actualCondition);
                    first = false;
                }

                localIntervals.addCondition(actualCondition.getRight());
                conditionalsStack.remove(actualCondition.getRight());
                actualCondition = actualCondition.getRight();

            }
            if (!localIntervals.getConditions().isEmpty()) {
//                globalIntervals.add(localIntervals);
                if (localIntervals.checkConsistency()) {
                    addList(globalIntervals, localIntervals);
                }
            }
        }
        return globalIntervals;
    }

    private static void addList(LinkedList<ConditionsChain> globalIntervals, ConditionsChain newList) {
        boolean merged = false;
        int count = 0;
        for (ConditionsChain interval : globalIntervals) {
            if (newList.getConditions().containsAll(interval.getConditions())) {
                globalIntervals.set(count, newList);
                merged = true;
            }
            count++;
        }
        if (!merged) {
            globalIntervals.add(newList);
        }
    }

    /* conditions to be true
     right node is an IF condition without ELSE (except when the ELSE is followed by an IF or exit  node (IF/ELSE chain))
     the condition tests the same variable as the actual node
     left has to be a NameExpr and the name must be equal
     the condition expression  right side's type is like the actual node's 
     */
    private static boolean continueToRightNode(IfNode node) {
        if (node.getRight() != null) {
            if (node.getRight().getType().equals(Node.NodeType.IF)) {
                IfNode rightNode = (IfNode) node.getRight();
                if (!rightNode.hasElse() || isIfElseChain(rightNode)) {
                    BinaryExpr rightNodeCondition = ConditionRefactorer.getBinaryCondition(rightNode.getCondition());
                    if (rightNodeCondition != null) {
                        BinaryExpr nodeCondition = ConditionRefactorer.getBinaryCondition(node.getCondition());
                        if (nodeCondition != null) {
                            if (nodeCondition.getLeft() instanceof NameExpr) {
                                NameExpr leftSideExpression = (NameExpr) nodeCondition.getLeft();
                                if (rightNodeCondition.getLeft() instanceof NameExpr) {
                                    NameExpr rightNodeleftSideExpression = (NameExpr) rightNodeCondition.getLeft();
                                    if (rightNodeleftSideExpression.getName().equals(leftSideExpression.getName())) {
                                        normalizeTypes(nodeCondition, rightNodeCondition);
                                        if (nodeCondition.getRight().getClass().equals(rightNodeCondition.getRight().getClass())) {
                                            return true;
                                        }
                                    }
                                }
                            }

                        }

                    }
                }
            }

        }
        return false;

    }
    /*
     Checks if the node has ELSE and is followed by another IF inside this else
     */

    private static boolean isIfElseChain(IfNode node) {
        if (node.hasElse()) {
            if (node.getRight().getType().equals(Node.NodeType.IF)
                    || node.getRight().getType().equals(Node.NodeType.EXIT)) {
                return true;
            }
        }
        return false;
    }

    private static Stack<Node> linkedListToStack(LinkedList<Node> list) {
        Stack<Node> conditionalsStack = new Stack<>();
        Iterator<Node> listIterator = list.iterator();
        while (listIterator.hasNext()) {
            Node node = listIterator.next();
            conditionalsStack.push(node);
        }

        return conditionalsStack;
    }

    private static LinkedList<Interval> conditionsToIntervalsList(ConditionsChain conditions) {
        LinkedList<Interval> intervals = new LinkedList<>();
        for (Node node : conditions.getConditions()) {
            Interval interval = conditionToInterval(node);
            if (interval != null) {
                intervals.add(interval);
            }
        }
        return intervals;
    }

    private static Interval conditionToInterval(Node condition) {
        if (condition instanceof IfNode) {
            IfNode ifNode = (IfNode) condition;
            BinaryExpr binaryExpr = ConditionRefactorer.getBinaryCondition(ifNode.getCondition());
            if (binaryExpr != null) {
                if (binaryExpr.getRight().getClass().equals(IntegerLiteralExpr.class)) {
                    IntegerLiteralExpr intConditionExpr = (IntegerLiteralExpr) binaryExpr.getRight();
                    return new Interval(binaryExpr.getOperator().toString(), Integer.parseInt(intConditionExpr.getValue()), condition);

                } else if (binaryExpr.getRight().getClass().equals(NameExpr.class)) {
                    NameExpr conditionNameExpr = (NameExpr) binaryExpr.getRight();
                    return new Interval(binaryExpr.getOperator().toString(), conditionNameExpr.getName(), condition);
                } else if (binaryExpr.getRight().getClass().equals(LongLiteralExpr.class)) { //TODO: no needed
                    LongLiteralExpr longConditionExpr = (LongLiteralExpr) binaryExpr.getRight();
                    return new Interval(binaryExpr.getOperator().toString(), Long.parseLong(longConditionExpr.getValue()), condition);
                } else if (binaryExpr.getRight().getClass().equals(DoubleLiteralExpr.class)) {
                    DoubleLiteralExpr doubleConditionExpr = (DoubleLiteralExpr) binaryExpr.getRight();
                    return new Interval(binaryExpr.getOperator().toString(), Double.parseDouble(doubleConditionExpr.getValue()), condition);
                }
            }
        }
        return null;

    }

    private static boolean checkPattern(LinkedList<Node> conditionals, ArrayList<Node> nodes) {
        boolean changed = false;
        LinkedList<ConditionsChain> conditionsClusters = getConditionClusters(conditionals);
        int count = 0;
        boolean updated = false;
        while (conditionsClusters.size() > 0) {
            ConditionsChain conditionCluster = conditionsClusters.get(count);
            conditionCluster.setNextNodeOutsideScope();
            LinkedList<Interval> actualInterval = conditionsToIntervalsList(conditionCluster);

            if (Interval.checkCoverage(actualInterval)) {
                if (actualInterval.size() > 1) {
                    updated = true;
                    if (!changed) {
                        changed = true;
                    }
                    IfNode preLastCondition = (IfNode) actualInterval.get(actualInterval.size() - 2).getNode();
                    IfNode lastCondition = (IfNode) actualInterval.get(actualInterval.size() - 1).getNode();
                    removeNode(lastCondition, preLastCondition, nodes, conditionCluster.getNextNodeOutsideScope());
                    if (!preLastCondition.hasElse()) {
                        preLastCondition.setHasElse(true);
                        lastCondition.setDegree(preLastCondition.getDegree() + 1);
                    }
                    conditionals.remove(lastCondition);
                    conditionsClusters = getConditionClusters(conditionals);
                    count = 0;
                }
            } else {
                count++;
            }

            if (count >= conditionsClusters.size()) {
                if (!updated) {
                    break;
                } else {
                    count = 0;
                    updated = false;
                }
            }
        }
        return changed;
    }

    private static void removeNode(Node toRemoveNode, Node parentNode, ArrayList<Node> nodes, Node nextNodeOutsideScope) {
        parentNode.setRight(toRemoveNode.getLeft());
        List<Node> incidentNodes = Node.getIncidentNodes(nodes, toRemoveNode);
        for (Node node : incidentNodes) {
            if (!node.equals(parentNode) && !node.equals(toRemoveNode)) {
                if (node.getLeft().equals(toRemoveNode)) {
//                    node.setLeft(toRemoveNode.getRight());
                    node.setLeft(nextNodeOutsideScope);
                } else {
                    if (node.hasElse()) { // toRemoveNode is inside an else
                        node.setHasElse(false);
                    }
                    if (!node.getRight().equals(toRemoveNode.getRight())) {
                        node.setRight(nextNodeOutsideScope);
                    } else {
                        node.setRight(null);
                    }
                }
            }
        }
        nodes.remove(toRemoveNode);
    }

    /*
     This method is a workaround for a problem with Javaparser API.
     When the decimal part of a value is .0 (e.g. 30.0) javaparser parses it as an Integer value.
    
     */
    private static void normalizeTypes(BinaryExpr firstExpr, BinaryExpr secondExpr) {
        checkTypes(firstExpr);
        checkTypes(secondExpr);
        
    }
    
    private static void checkTypes(BinaryExpr expression){
        if (expression.getLeft() instanceof NameExpr) {
            NameExpr leftVarName = (NameExpr) expression.getLeft();
            String varType = variablesTable.get(leftVarName.getName()).toString();
            switch (varType) {
                case "int":
                case "long":
                    if(expression.getRight() instanceof DoubleLiteralExpr){
                        DoubleLiteralExpr doubleValue = (DoubleLiteralExpr) expression.getRight();
                        expression.setRight(new IntegerLiteralExpr(doubleValue.getBeginLine(), doubleValue.getBeginColumn(), doubleValue.getEndLine(), doubleValue.getEndColumn(), doubleValue.getValue()));
                    }
                    break;
                case "double":
                case "float":
                    if(expression.getRight() instanceof IntegerLiteralExpr){
                        IntegerLiteralExpr intValue = (IntegerLiteralExpr) expression.getRight();
                        expression.setRight(new DoubleLiteralExpr(intValue.getBeginLine(), intValue.getBeginColumn(), intValue.getEndLine(), intValue.getEndColumn(), intValue.getValue()));
                    }
                    break;
            }
            
        }
    }
    
    

}
