/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.complexity.tool.mvn;

import com.github.javaparser.ast.body.MethodDeclaration;
import javax.swing.JTextArea;

/**
 *
 * @author helenocampos
 */
public class App {

    private Parser parser;
    private Renderer renderer;
    private String code;
    private GraphAnalysis analysis;

    public void processMethod(MethodDeclaration n, JTextArea codeArea, JTextArea statementArea) {
        parser = new Parser(false);
        Scanner scanner = new Scanner(parser);
        scanner.scanMethod(n);
        renderer = new Renderer(parser);
        renderer.renderGraph(codeArea, statementArea, n);
        setAnalysis(new GraphAnalysis(parser.getRoot(), parser.getExitNode()));
        getAnalysis().analyzeGraph();
        setCode(n.toString());
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

}
