/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.complexity.tool.mvn;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.lang.System.in;


/**
 *
 * @author helenocampos
 */
public class Scanner {
    
    private Parser parser = null;
    
    public Scanner(Parser parser){
        this.parser = parser;
    }

    public void scan(String fileName) {
        try {
            FileInputStream in = new FileInputStream(fileName);
            CompilationUnit cu;
            cu = JavaParser.parse(in);
            new MethodVisitor().visit(cu, null);
        } catch (FileNotFoundException e) {
            System.out.println(fileName + " not found.");
        } catch (ParseException e){
            System.out.println("Parse exception. Details: "+e.getMessage());
        }finally {
            try {
                in.close();
            } catch (IOException e) {
                System.out.println("IO Exception. Details: "+e.getMessage());
            }
        }

    }

    private class MethodVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            int stmtCount = n.getBody().getStmts().size();
            for (Statement stmt : n.getBody().getStmts()) {
                parser.parseManager(stmt,--stmtCount);
                
            }
            parser.connectClosingNode(new ReturnStmt(n.getEndLine(), 1, n.getEndLine(), 1, null));
        }
    }
    
    public void scanMethod(MethodDeclaration n){
         int stmtCount = n.getBody().getStmts().size();
            for (Statement stmt : n.getBody().getStmts()) {
                parser.parseManager(stmt,--stmtCount);
                
            }
            if(parser.getRoot()==null){
                parser.setRoot(parser.createBlockNode(n.getBody()));
            }
            
            parser.connectClosingNode(new ReturnStmt(n.getEndLine(), 1, n.getEndLine(), 1, null));
    }
    
    
}
