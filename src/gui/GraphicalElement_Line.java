package gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import geom.DiscreteRegion;
import geom.Polygons;
import geom.RiffIntersectionPoint;
import geom.points.Point;
import geom.points.EuclideanPoint;
import inspect.Nodeable;
import logging.Logs;
import script.ScriptEnvironment;

public class GraphicalElement_Line extends InterfaceElement implements Nodeable {
	private Point pointA, pointB;

	public GraphicalElement_Line(ScriptEnvironment env, Point pointA, Point pointB) {
		super(env, null, null);
		this.pointA = pointA;
		this.pointB = pointB;
	}

	public Point getPointA() {
		return this.pointA;
	}

	public Point getPointB() {
		return this.pointB;
	}

	@Override
	public Rectangle getDrawingBounds() {
		return new Rectangle(this.getXAnchor() + this.getLeftMarginMagnitude() + this.getLeftBorderMagnitude(), this.getYAnchor() + this.getTopMarginMagnitude() + this.getTopBorderMagnitude(), this.getInternalWidth(), this.getInternalHeight());
	}

	@Override
	public boolean isFocusable() {
		return false;
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Line Graphical Element");
		assert Logs.addNode("First point: " + this.pointA);
		assert Logs.addNode("Second point: " + this.pointB);
		assert Logs.closeNode();
	}

	@Override
	public void paint(Graphics2D g2d) {
		assert Logs.openNode("Line Painting Operations", "Painting Line Element");
		assert Logs.addNode(this);
		Point offset;
		if (this.getParent() instanceof InterfaceElement_Panel) {
			offset = ((InterfaceElement_Panel) this.getParent()).getOffset();
		} else {
			offset = new EuclideanPoint(this.getXAnchor(), this.getYAnchor(), 0.0d);
		}
		// Offset translation
		assert this.pointA != null : "Point A is null in LineElement";
		assert this.pointB != null : "Point B is null in LineElement";
		assert offset != null : "Offset point is null in LineElement";
		double ax = this.pointA.getX() - offset.getX();
		double ay = this.pointA.getY() - offset.getY();
		double bx = this.pointB.getX() - offset.getX();
		double by = this.pointB.getY() - offset.getY();
		// Orthographic zoom
		ax = ax * Math.pow(2, offset.getZ());
		ay = ay * Math.pow(2, offset.getZ());
		bx = bx * Math.pow(2, offset.getZ());
		by = by * Math.pow(2, offset.getZ());
		// Converstion to screen coordinates
		assert this.getParent() != null;
		assert this.getParent().getContainerElement() != null;
		assert this.getParent().getContainerElement().getDrawingBounds() != null;
		double width = (this.getParent().getContainerElement().getDrawingBounds().getWidth() - this.getParent().getContainerElement().getDrawingBounds().getX()) / 2;
		double height = (this.getParent().getContainerElement().getDrawingBounds().getHeight() - this.getParent().getContainerElement().getDrawingBounds().getY()) / 2;
		// Draw transformed line
		Point translatedPointA = new EuclideanPoint(ax + this.getParent().getContainerElement().getDrawingBounds().getX() + width, ay + this.getParent().getContainerElement().getDrawingBounds().getY() + height, 0.0d);
		Point translatedPointB = new EuclideanPoint(bx + this.getParent().getContainerElement().getDrawingBounds().getX() + width, by + this.getParent().getContainerElement().getDrawingBounds().getY() + height, 0.0d);
		DiscreteRegion region = Polygons.convertToRegion(this.getEnvironment(), this.getParent().getContainerElement().getDrawingBounds());
		List<RiffIntersectionPoint> intersections = Polygons.getIntersections(translatedPointA, translatedPointB, region);
		if (intersections.size() == 0 && !Polygons.getBoundingRectIntersection(translatedPointA, translatedPointA, region) && !Polygons.getBoundingRectIntersection(translatedPointB, translatedPointB, region)) {
			return;
		}
		if (intersections.size() == 2) {
			translatedPointA = intersections.get(0).getIntersection();
			translatedPointB = intersections.get(1).getIntersection();
		} else if (intersections.size() == 1) {
			if (Polygons.getBoundingRectIntersection(translatedPointA, translatedPointA, region)) {
				translatedPointB = intersections.get(0).getIntersection();
			} else {
				translatedPointA = intersections.get(0).getIntersection();
			}
		}
		assert Logs.addNode("Translated first point: " + translatedPointA);
		assert Logs.addNode("Translated second point: " + translatedPointB);
		g2d.draw(new java.awt.geom.Line2D.Double(translatedPointA.getX(), translatedPointA.getY(), translatedPointB.getX(), translatedPointB.getY()));
		assert Logs.closeNode();
	}

	public void setPointA(Point point) {
		this.pointA = point;
	}

	public void setPointB(Point point) {
		this.pointB = point;
	}
}
