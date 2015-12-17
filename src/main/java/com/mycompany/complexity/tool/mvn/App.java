/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.complexity.tool.mvn;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.complexity.tool.mvn.Nodes.Node;
import javax.swing.JTextArea;

/**
 *
 * @author helenocampos
 */
public class App {

    private Parser parser;
    private Renderer renderer;
    private Renderer optimizedRenderer = null;
    private String code;
    private GraphAnalysis analysis;
    private GraphAnalysis optimizedAnalysis;
    private boolean optimizingSugestions = false;

    public void processMethod(MethodDeclaration n, JTextArea codeArea, JTextArea statementArea) {
        parser = new Parser(false);
        Scanner scanner = new Scanner(parser);
        scanner.scanMethod(n);
        renderer = new Renderer(parser);
        renderer.renderGraph(codeArea, statementArea, n);
        setAnalysis(new GraphAnalysis(parser.getRoot(), parser.getExitNode()));
        getAnalysis().analyzeGraph();
        setCode(n.toString());
        
        /*
            Temporary pattern analysis needs to duplicate parser
        
        */
        Parser secondaryParser = new Parser(false);
        Scanner secondaryScanner = new Scanner(secondaryParser);
        secondaryScanner.scanMethod(n);
        setOptimizingSugestions(PatternAnalysis.analize(secondaryParser.getConditionals(),secondaryParser.getNodes()));
        Node.resetNodesRenderingStates(secondaryParser.getNodes());
        optimizedRenderer = new Renderer(secondaryParser);
        optimizedRenderer.renderGraph(codeArea, statementArea, n);
        setOptimizedAnalysis(new GraphAnalysis(secondaryParser.getRoot(), secondaryParser.getExitNode()));
        getOptimizedAnalysis().analyzeGraph();

        
    }
   
    public Parser getParser() {
        return this.parser;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public GraphAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(GraphAnalysis analysis) {
        this.analysis = analysis;
    }

    /**
     * @return the optimizedRenderer
     */
    public Renderer getOptimizedRenderer() {
        return optimizedRenderer;
    }

    /**
     * @param optimizedRenderer the optimizedRenderer to set
     */
    public void setOptimizedRenderer(Renderer optimizedRenderer) {
        this.optimizedRenderer = optimizedRenderer;
    }

    public boolean hasOptimizingSugestions() {
        return optimizingSugestions;
    }

    public void setOptimizingSugestions(boolean optimizingSugestions) {
        this.optimizingSugestions = optimizingSugestions;
    }

    public GraphAnalysis getOptimizedAnalysis() {
        return optimizedAnalysis;
    }

    public void setOptimizedAnalysis(GraphAnalysis optimizedAnalysis) {
        this.optimizedAnalysis = optimizedAnalysis;
    }

}
