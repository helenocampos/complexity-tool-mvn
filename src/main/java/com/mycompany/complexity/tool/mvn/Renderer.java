package com.mycompany.complexity.tool.mvn;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.complexity.tool.mvn.Nodes.Edge;
import com.mycompany.complexity.tool.mvn.Nodes.IfNode;
import com.mycompany.complexity.tool.mvn.Nodes.Node;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

/**
 *
 * @author helenocampos
 */
public class Renderer {

    private int lastEdgeId;
    private Parser parser;
    private Tree<Integer, Edge> g = new DelegateTree<>();
    private VisualizationViewer<Integer, Edge> vv;
    private Node selectedNode;

    public Renderer(Parser parser) {
        this.parser = parser;
        lastEdgeId = 0;
        g.addVertex(1);
        constructGraph(parser.getRoot());
    }

    private void constructGraph(Node actualNode) {  // lateral link controls if its possible to link node laterally

        if (!actualNode.isRendered() && !actualNode.isLocked()) {
            actualNode.setLocked(true);

            if (actualNode.getLeft() != null) {
                Edge edge = new Edge(lastEdgeId++, actualNode, actualNode.getLeft());
                g.addEdge(edge, actualNode.getId(), actualNode.getLeft().getId(), EdgeType.DIRECTED);
            }

            if (actualNode.getRight() != null) {
                Edge edge = new Edge(lastEdgeId++, actualNode, actualNode.getRight());
                g.addEdge(edge, actualNode.getId(), actualNode.getRight().getId(), EdgeType.DIRECTED);

            }

            if (actualNode.getLeft() != null) {

                constructGraph(actualNode.getLeft());

            }

            if (actualNode.getRight() != null) {

                constructGraph(actualNode.getRight());

            }

            actualNode.setRendered(true);
        }
    }

    public VisualizationViewer<Integer, Edge> renderGraph(final JTextArea codeArea, final JTextArea statementDisplayArea, final MethodDeclaration method) {
        
        Layout<Integer, Edge> layout = new TreeLayout<>(g, parser.getNodes());

        vv = new VisualizationViewer<Integer, Edge>(layout);

        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Edge>());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());

        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();

        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);

        vv.setGraphMouse(gm);

        vv.addKeyListener(gm.getModeKeyListener());

        final PickedState<Integer> pickedState = vv.getPickedVertexState();
        
        pickedState.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                Object subject = e.getItem();
                if (subject instanceof Integer) {
                    Integer vertex = (Integer) subject;
                    if (pickedState.isPicked(vertex)) {
                        setSelectedVertexStroke(vertex);
                        selectedNode = Node.getNode(parser.getNodes(), vertex);
                        statementDisplayArea.setText(selectedNode.getStatementText());
                        int line = 0;
                        int finalLine = 0;
                        if(selectedNode.getBaseStatement()!=null){
                            line = selectedNode.getBaseStatement().getBeginLine() - method.getBeginLine() ;
                            finalLine = selectedNode.getBaseStatement().getEndLine()-method.getBeginLine();
                        }else if(selectedNode.getType().equals(Node.NodeType.IF)){
                            IfNode node = (IfNode) selectedNode;
                            line = node.getCondition().getBeginLine() - method.getBeginLine();
                            finalLine = node.getCondition().getEndLine()-method.getBeginLine();
                        }
                        
                        if(selectedNode.getType().equals(Node.NodeType.BLOCK)){
                             selectCodeAreaLines(codeArea, line+1, finalLine-1);
                        }else if(selectedNode.getType().equals(Node.NodeType.LOOP_EXIT)){
                           selectCodeAreaLines(codeArea,finalLine,finalLine);
                        }else{
                            selectCodeAreaLines(codeArea, line, finalLine);
                        }
                       

                    } else {
                        vv.getRenderContext().setVertexStrokeTransformer(new ConstantTransformer(new BasicStroke()));
                    }
                }
            }
        });

        return vv;
    }

    private void selectCodeAreaLines(JTextArea codeArea, int initialPosition, int finalPosition) {
        try {
            initialPosition = codeArea.getLineStartOffset(initialPosition);
            finalPosition = codeArea.getLineEndOffset(finalPosition);
            Highlighter highlighter = codeArea.getHighlighter();
            highlighter.removeAllHighlights();
            HighlightPainter painter
                    = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);

            highlighter.addHighlight(initialPosition, finalPosition, painter);

        } catch (BadLocationException ex) {
            Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setSelectedVertexStroke(final int vertex) {
        Transformer<Integer, Stroke> vertexStroke = new Transformer<Integer, Stroke>() {
            public Stroke transform(Integer i) {
                if (i == vertex) {
                    return new BasicStroke(3.0f);
                } else {
                    return new BasicStroke();
                }

            }
        };
        vv.getRenderContext().setVertexStrokeTransformer(vertexStroke);

    }

    public void fillPath(final Stack<Node> path) {
        Transformer<Integer, Paint> vertexPaint
                = new Transformer<Integer, Paint>() {
                    @Override
                    public Paint transform(Integer i) {
                        if (Node.contains(path, i)) {
                            return Color.BLUE;
                        } else {
                            return Color.RED;
                        }

                    }
                };
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

        Transformer<Edge, Paint> edgePaint = new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                if (Edge.contains(edge, path)) {
                    return Color.BLUE;
                } else {
                    return Color.BLACK;
                }
            }
        };

        Transformer<Edge, Stroke> edgeStroke = new Transformer<Edge, Stroke>() {
            public Stroke transform(Edge edge) {
                if (Edge.contains(edge, path)) {
                    return new BasicStroke(2.0f);
                } else {
                    return new BasicStroke();
                }

            }
        };

        vv.getRenderContext().setEdgeArrowStrokeTransformer(edgeStroke);
        vv.getRenderContext().setArrowFillPaintTransformer(edgePaint);
        vv.getRenderContext()
                .setEdgeStrokeTransformer(edgeStroke);

        vv.getRenderContext().setEdgeDrawPaintTransformer(edgePaint);
        vv.getRenderContext()
                .setVertexFillPaintTransformer(vertexPaint);
    }

    public VisualizationViewer<Integer, Edge> getVisualizationViewer() {
        return this.vv;
    }

    public Node getSelectedNode() {
        return this.selectedNode;
    }

}
