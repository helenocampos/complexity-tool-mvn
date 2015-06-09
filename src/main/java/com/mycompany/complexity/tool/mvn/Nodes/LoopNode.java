/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.complexity.tool.mvn.Nodes;

import com.github.javaparser.ast.stmt.Statement;

/**
 *
 * @author Heleno
 */
public class LoopNode {

    private Node source;
    private Node destination;

    public LoopNode(Node source, Node destination) {
        this.source = source;
        this.destination = destination;
    }

    /**
     * @return the source
     */
    public Node getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(Node source) {
        this.source = source;
    }

    /**
     * @return the destination
     */
    public Node getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(Node destination) {
        this.destination = destination;
    }
    
   
    
    
}
