/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 * 
 *******************************************************************************/
package org.eclipse.gef4.fx.examples.snippets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import org.eclipse.gef4.common.adapt.AdapterKey;
import org.eclipse.gef4.common.adapt.AdapterStore;
import org.eclipse.gef4.fx.anchors.AnchorKey;
import org.eclipse.gef4.fx.anchors.FXChopBoxAnchor;
import org.eclipse.gef4.fx.examples.FXApplication;
import org.eclipse.gef4.fx.gestures.FXMouseDragGesture;
import org.eclipse.gef4.fx.nodes.FXGeometryNode;
import org.eclipse.gef4.geometry.convert.fx.JavaFX2Geometry;
import org.eclipse.gef4.geometry.planar.BezierCurve;
import org.eclipse.gef4.geometry.planar.CurvedPolygon;
import org.eclipse.gef4.geometry.planar.ICurve;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.internal.geometry.utils.PrecisionUtils;

public class FXChopBoxELetterSnippet extends FXApplication {

	// TODO: use CSS for styling
	// configuration (colors and sizes)
	private static final double E_LETTER_STROKE_WIDTH = 1.5;

	private static final Paint VERTEX_STROKE = Color.BLACK;
	private static final Paint VERTEX_FILL = Color.WHITE;
	private static final double VERTEX_RADIUS = 3;

	private static final Paint DISTANCE_LINE_STROKE_NORMAL = Color.GREY;
	private static final Paint DISTANCE_LINE_STROKE_HOVER = Color.BLACK;
	private static final double DISTANCE_LINE_STROKE_WIDTH_NORMAL = 1;
	private static final double DISTANCE_LINE_STROKE_WIDTH_HOVER = 2.5;
	private static final double DISTANCE_LINE_SELECTION_STROKE_WIDTH = 5.5;

	private static final double DISTANCE_TEXT_SCALE = 1.5;
	private static final Paint DISTANCE_TEXT_STROKE = Color.TRANSPARENT;
	private static final Paint DISTANCE_TEXT_FILL = Color.BLACK;

	private static final Paint CENTER_POINT_STROKE = Color.BLACK;
	private static final Paint CENTER_POINT_FILL = Color.ORANGE;
	private static final double CENTER_POINT_RADIUS = 3;

	private static final double ELETTER_REFERENCE_POINT_RADIUS = 3;
	private static final Paint ELETTER_REFERENCE_POINT_STROKE = Color.TRANSPARENT;
	private static final Paint ELETTER_REFERENCE_POINT_FILL = Color.ORANGE;

	private static final Paint REFERENCE_POINT_FILL = Color.BLUE;
	private static final Paint REFERENCE_POINT_STROKE = Color.TRANSPARENT;
	private static final double REFERENCE_POINT_RADIUS = 5;

	private static final Paint CHOP_BOX_POINT_FILL = Color.RED;
	private static final Paint CHOP_BOX_POINT_STROKE = Color.TRANSPARENT;
	private static final double CHOP_BOX_POINT_RADIUS = 3;

	private static final double INTERSECTION_RADIUS = 3;
	private static final Paint INTERSECTION_STROKE = Color.BLACK;
	private static final Paint INTERSECTION_FILL = Color.DARKRED;

	private static final Paint CHOP_BOX_LINE_STROKE_REAL = Color.DARKGREEN;
	private static final Paint CHOP_BOX_LINE_STROKE_IMAGINARY = Color.DARKRED;

	private static final double PAD = 100;
	private static final double HEIGHT = 480;
	private static final double WIDTH = 640;

	private abstract static class OnDrag extends FXMouseDragGesture {
		private Node target;

		public OnDrag(Node target) {
			this.target = target;
		}

		public abstract void dragTo(double x, double y);

		@Override
		protected void drag(Node target, MouseEvent event, double dx, double dy) {
			// consider only mouse drags on our target
			if (target == this.target)
				// do not drag outside of scene
				if (event.getX() >= 0 && event.getY() >= 0
						&& event.getX() <= WIDTH && event.getY() <= HEIGHT)
					dragTo(event.getX(), event.getY());
		}

		@Override
		protected void press(Node target, MouseEvent event) {
		}

		@Override
		protected void release(Node target, MouseEvent event, double dx,
				double dy) {
		}
	}

	private Scene scene;
	private BorderPane root;
	private Group markerLayer; // between shape and ref points
	private Group interactionLayer; // always on top
	private FXGeometryNode<CurvedPolygon> eLetterShape;
	private FXChopBoxAnchor chopBoxAnchor;
	private ReadOnlyMapWrapper<AnchorKey, Point> referencePointProperty;
	private Map<AnchorKey, Circle> chopBoxPoints = new HashMap<AnchorKey, Circle>();
	private Map<AnchorKey, Line> chopBoxLinesReal = new HashMap<AnchorKey, Line>();
	private Map<AnchorKey, Line> chopBoxLinesImaginary = new HashMap<AnchorKey, Line>();
	private Map<AnchorKey, List<Node>> intersections = new HashMap<AnchorKey, List<Node>>();
	private List<Node> vertices = new ArrayList<Node>();
	private List<Node> distanceLines = new ArrayList<Node>();

	private MapChangeListener<AnchorKey, Point> anchorPositionListener = new MapChangeListener<AnchorKey, Point>() {
		@Override
		public void onChanged(
				javafx.collections.MapChangeListener.Change<? extends AnchorKey, ? extends Point> change) {
			Point p = change.getValueAdded();
			if (p != null) {
				onAnchorPositionChange(change.getKey(), p);
			}
		}
	};

	public static void main(String[] args) {
		launch();
	}

	protected void onAnchorPositionChange(AnchorKey key, Point anchorPosition) {
		// update chop box point
		Circle chopBoxPoint = chopBoxPoints.get(key);
		chopBoxPoint.setCenterX(anchorPosition.x);
		chopBoxPoint.setCenterY(anchorPosition.y);
	}

	@Override
	public Scene createScene() {
		root = new BorderPane();
		scene = new Scene(root, WIDTH, HEIGHT);

		// description (what is demonstrated)
		Label descriptionLabel = new Label(
				"This example demonstrates the chop box anchor position computation. An FXChopBoxAnchor is associated with the E letter shape. The blue points are reference points for the anchor position computation. The red points are the resulting anchor positions.");
		descriptionLabel.setWrapText(true);
		descriptionLabel.resizeRelocate(10, 10, WIDTH - 20, PAD - 20);
		descriptionLabel.setAlignment(Pos.TOP_LEFT);
		root.getChildren().add(descriptionLabel);

		// legend (how to interact)
		Label legendLabel = new Label(
				"You can...\n...drag the blue reference points\n...press <V> to toggle shape vertices\n...press <L> to toggle distance lines");
		legendLabel.resizeRelocate(10, HEIGHT - PAD + 10, WIDTH - 20, PAD - 20);
		legendLabel.setAlignment(Pos.BOTTOM_RIGHT);
		root.getChildren().add(legendLabel);

		eLetterShape = createELetterShape();
		root.getChildren().add(eLetterShape);
		
		markerLayer = new Group();
		interactionLayer = new Group();
		root.getChildren().addAll(markerLayer, interactionLayer);

		// create chop box anchor and reference point property (so we can access
		// the reference points easily)
		chopBoxAnchor = new FXChopBoxAnchor(eLetterShape);
		chopBoxAnchor.positionProperty().addListener(anchorPositionListener);
		referencePointProperty = new ReadOnlyMapWrapper<AnchorKey, Point>(
				FXCollections.observableMap(new HashMap<AnchorKey, Point>()));

		// compute bounds center
		Point boundsCenterInLocal = JavaFX2Geometry.toRectangle(
				eLetterShape.getLayoutBounds()).getCenter();
		Point2D boundsCenterInScene = eLetterShape.localToScene(
				boundsCenterInLocal.x, boundsCenterInLocal.y);

		markerLayer.getChildren().add(createBoundsCenterNode(boundsCenterInScene));
		markerLayer.getChildren().add(createELetterReferenceNode());

		// create draggable reference points around the shape
		createReferencePoint(PAD / 2, HEIGHT / 2);
		createReferencePoint(WIDTH - PAD / 2, HEIGHT / 2);

		// show outline vertices and distance to the bounds center
		for (BezierCurve seg : eLetterShape.getGeometry().getOutlineSegments()) {
			// vertex
			Point vertexInLocal = seg.getP1();
			Point2D vertexInScene = eLetterShape.localToScene(vertexInLocal.x,
					vertexInLocal.y);
			Circle vertexNode = createVertexNode(vertexInScene);
			markerLayer.getChildren().add(vertexNode);
			vertexNode.toBack();

			// add to vertices list so we can disable/enable later
			vertices.add(vertexNode);

			// distance to bounds center
			final Line distanceLine = createDistanceLine(boundsCenterInScene,
					vertexInScene);
			markerLayer.getChildren().add(distanceLine);
			distanceLine.toBack();

			// show distance on mouse hover
			double distance = JavaFX2Geometry.toPoint(vertexInScene)
					.getDistance(JavaFX2Geometry.toPoint(boundsCenterInScene));
			final Text distanceText = new Text(String.format("%.2f", distance));
			// TODO: make configurable
			distanceText.setScaleX(DISTANCE_TEXT_SCALE);
			distanceText.setScaleY(DISTANCE_TEXT_SCALE);
			distanceText.setStroke(DISTANCE_TEXT_STROKE);
			distanceText.setFill(DISTANCE_TEXT_FILL);
			distanceText.relocate(
					(vertexInScene.getX() + boundsCenterInScene.getX()) / 2,
					(vertexInScene.getY() + boundsCenterInScene.getY()) / 2);
			distanceText.setVisible(false);
			markerLayer.getChildren().add(distanceText);

			// invisible selection line
			Line selectionLine = new Line(distanceLine.getStartX(),
					distanceLine.getStartY(), distanceLine.getEndX(),
					distanceLine.getEndY());
			selectionLine.setStrokeWidth(DISTANCE_LINE_SELECTION_STROKE_WIDTH);
			selectionLine.setStroke(Color.TRANSPARENT);
			markerLayer.getChildren().add(selectionLine);

			selectionLine.setOnMouseEntered(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					distanceLine.setStroke(DISTANCE_LINE_STROKE_HOVER);
					distanceLine
							.setStrokeWidth(DISTANCE_LINE_STROKE_WIDTH_HOVER);
					distanceText.setVisible(true);
				}
			});
			selectionLine.setOnMouseExited(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					distanceLine.setStroke(DISTANCE_LINE_STROKE_NORMAL);
					distanceLine
							.setStrokeWidth(DISTANCE_LINE_STROKE_WIDTH_NORMAL);
					distanceText.setVisible(false);
				}
			});

			// add to distance lines list so we can disable/enable later
			distanceLines.add(selectionLine);
			distanceLines.add(distanceLine);
		}

		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			private boolean verticesVisible = true;
			private boolean distanceLinesVisible = true;

			public void handle(KeyEvent event) {
				if (event.getText().toLowerCase().equals("v")) {
					verticesVisible = !verticesVisible;
					setVisible(vertices, verticesVisible);
				} else if (event.getText().toLowerCase().equals("l")) {
					distanceLinesVisible = !distanceLinesVisible;
					setVisible(distanceLines, distanceLinesVisible);
				}
			}
		});

		return scene;
	}

	private Node createELetterReferenceNode() {
		Circle node = new Circle(ELETTER_REFERENCE_POINT_RADIUS);
		node.setStroke(ELETTER_REFERENCE_POINT_STROKE);
		node.setFill(ELETTER_REFERENCE_POINT_FILL);
		Point p = chopBoxAnchor.getComputationStrategy()
				.computeReferencePointInScene(eLetterShape,
						eLetterShape.getGeometry());
		node.setCenterX(p.x);
		node.setCenterY(p.y);
		return node;
	}

	private Line createDistanceLine(Point2D boundsCenterInScene,
			Point2D vertexInScene) {
		final Line distanceLine = new Line(vertexInScene.getX(),
				vertexInScene.getY(), boundsCenterInScene.getX(),
				boundsCenterInScene.getY());
		distanceLine.setStrokeWidth(DISTANCE_LINE_STROKE_WIDTH_NORMAL);
		distanceLine.getStrokeDashArray().addAll(5d, 5d);
		distanceLine.setStroke(DISTANCE_LINE_STROKE_NORMAL);
		return distanceLine;
	}

	private Circle createVertexNode(Point2D vertexInScene) {
		Circle vertexNode = new Circle(VERTEX_RADIUS);
		vertexNode.setFill(VERTEX_FILL);
		vertexNode.setStroke(VERTEX_STROKE);
		vertexNode.setCenterX(vertexInScene.getX());
		vertexNode.setCenterY(vertexInScene.getY());
		return vertexNode;
	}

	private Circle createBoundsCenterNode(Point2D boundsCenterInScene) {
		Circle centerNode = new Circle(CENTER_POINT_RADIUS);
		centerNode.setFill(CENTER_POINT_FILL);
		centerNode.setStroke(CENTER_POINT_STROKE);
		centerNode.setCenterX(boundsCenterInScene.getX());
		centerNode.setCenterY(boundsCenterInScene.getY());
		return centerNode;
	}

	private FXGeometryNode<CurvedPolygon> createELetterShape() {
		FXGeometryNode<CurvedPolygon> eLetterShape = new FXGeometryNode<CurvedPolygon>(
				FXGeometryNodeExample.createEShapeGeometry());
		eLetterShape.relocate(PAD, PAD);
		eLetterShape.resize(WIDTH - PAD - PAD, HEIGHT - PAD - PAD);
		eLetterShape.setStrokeWidth(E_LETTER_STROKE_WIDTH);
		eLetterShape.setEffect(FXGeometryNodeExample.createShadowEffect());
		return eLetterShape;
	}

	private void setVisible(List<Node> nodes, boolean isVisible) {
		for (Node n : nodes) {
			n.setVisible(isVisible);
		}
	}

	private void createReferencePoint(final double x, final double y) {
		final Circle referencePointNode = createReferencePointNode(x, y);
		interactionLayer.getChildren().add(referencePointNode);
		Circle chopBoxPointNode = createChopBoxNode();

		// create key for the anchor relation (role is arbitrary)
		final AnchorKey ak = new AnchorKey(referencePointNode, "link");

		// create real and imaginary chop box lines
		Line chopBoxLineReal = createChopBoxLineReal(ak);
		Line chopBoxLineImaginary = createChopBoxLineImaginary(ak);
		markerLayer.getChildren().addAll(chopBoxLineReal, chopBoxLineImaginary,
				chopBoxPointNode);
		chopBoxLineReal.toBack();
		chopBoxLineImaginary.toBack();

		// associate the chop box point and line with that key
		chopBoxPoints.put(ak, chopBoxPointNode);
		chopBoxLinesReal.put(ak, chopBoxLineReal);
		chopBoxLinesImaginary.put(ak, chopBoxLineImaginary);

		// put initial reference point
		referencePointProperty.put(ak, new Point(x, y));

		// adjust reference point on drag
		OnDrag dragGesture = new OnDrag(referencePointNode) {
			@Override
			public void dragTo(double x, double y) {
				// update center point
				referencePointNode.setCenterX(x);
				referencePointNode.setCenterY(y);
				// update reference point
				referencePointProperty.put(ak, new Point(x, y));
				updateChopBoxLines(ak);
			}
		};
		dragGesture.setScene(scene);

		attachToChopBoxAnchor(ak, referencePointProperty);
	}

	private Line createChopBoxLineImaginary(AnchorKey ak) {
		Line chopBoxLineImaginary = new Line();
		chopBoxLineImaginary.getStrokeDashArray().addAll(5d, 5d);
		chopBoxLineImaginary.setStroke(CHOP_BOX_LINE_STROKE_IMAGINARY);
		return chopBoxLineImaginary;
	}

	private void updateChopBoxLines(AnchorKey ak) {
		// update real line
		Line lineReal = chopBoxLinesReal.get(ak);
		Point referencePosition = referencePointProperty.get(ak);
		Point anchorPosition = chopBoxAnchor.getPosition(ak);
		lineReal.setStartX(referencePosition.x);
		lineReal.setStartY(referencePosition.y);
		lineReal.setEndX(anchorPosition.x);
		lineReal.setEndY(anchorPosition.y);

		// update imaginary line
		Point eLetterReferencePoint = chopBoxAnchor.getComputationStrategy()
				.computeReferencePointInScene(eLetterShape,
						eLetterShape.getGeometry());
		Line lineImaginary = chopBoxLinesImaginary.get(ak);
		lineImaginary.setStartX(anchorPosition.x);
		lineImaginary.setStartY(anchorPosition.y);
		lineImaginary.setEndX(eLetterReferencePoint.x);
		lineImaginary.setEndY(eLetterReferencePoint.y);

		// update intersection points
		if (intersections.containsKey(ak)) {
			List<Node> toRemove = intersections.remove(ak);
			markerLayer.getChildren().removeAll(toRemove);
		}
		List<Node> intersectionNodes = new ArrayList<Node>();
		ICurve eLetterOutline = chopBoxAnchor
				.getComputationStrategy()
				.computeOutlineInScene(eLetterShape, eLetterShape.getGeometry());
		org.eclipse.gef4.geometry.planar.Line referenceLine = new org.eclipse.gef4.geometry.planar.Line(
				referencePosition, eLetterReferencePoint);
		Point[] intersectionPoints = eLetterOutline
				.getIntersections(referenceLine);
		for (Point p : intersectionPoints) {
			// TODO: precision problem!
			if (!unpreciseEquals(anchorPosition, p)
					&& !unpreciseEquals(eLetterReferencePoint, p)) {
				Node node = createIntersectionNode(p);
				intersectionNodes.add(node);
				markerLayer.getChildren().add(node);
			}
		}
		intersections.put(ak, intersectionNodes);
	}

	private boolean unpreciseEquals(Point p, Point q) {
		return PrecisionUtils.equal(q.x, p.x, -2)
				&& PrecisionUtils.equal(q.y, p.y, -2);
	}

	private Node createIntersectionNode(Point p) {
		Circle c = new Circle(INTERSECTION_RADIUS);
		c.setStroke(INTERSECTION_STROKE);
		c.setFill(INTERSECTION_FILL);
		c.setCenterX(p.x);
		c.setCenterY(p.y);
		return c;
	}

	private Line createChopBoxLineReal(AnchorKey ak) {
		Line chopBoxLineReal = new Line();
		chopBoxLineReal.setStroke(CHOP_BOX_LINE_STROKE_REAL);
		return chopBoxLineReal;
	}

	private void attachToChopBoxAnchor(final AnchorKey ak,
			final ReadOnlyMapWrapper<AnchorKey, Point> referencePointProperty) {
		AdapterStore as = new AdapterStore();
		as.setAdapter(
				AdapterKey.get(FXChopBoxAnchor.ReferencePointProvider.class),
				new FXChopBoxAnchor.ReferencePointProvider() {
					@Override
					public ReadOnlyMapWrapper<AnchorKey, Point> referencePointProperty() {
						return referencePointProperty;
					}
				});
		chopBoxAnchor.attach(ak, as);
		updateChopBoxLines(ak);
	}

	private Circle createChopBoxNode() {
		Circle chopBoxPointNode = new Circle(CHOP_BOX_POINT_RADIUS);
		chopBoxPointNode.setFill(CHOP_BOX_POINT_FILL);
		chopBoxPointNode.setStroke(CHOP_BOX_POINT_STROKE);
		return chopBoxPointNode;
	}

	private Circle createReferencePointNode(final double x, final double y) {
		final Circle referencePointNode = new Circle(REFERENCE_POINT_RADIUS);
		referencePointNode.setFill(REFERENCE_POINT_FILL);
		referencePointNode.setStroke(REFERENCE_POINT_STROKE);
		referencePointNode.setCenterX(x);
		referencePointNode.setCenterY(y);
		return referencePointNode;
	}

}
