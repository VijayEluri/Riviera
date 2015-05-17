package geom;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import geom.points.Point;
import geom.points.EuclideanPoint;
import geom.points.PolarPoint;
import geom.points.Points;
import logging.Logs;
import script.ScriptEnvironment;

public class Polygons {
	public static boolean areSlopesEqual(Point pointA, Point pointB, Point testPointA, Point testPointB) {
		if (Points.areEqual(pointA, pointB.getX(), pointA.getX())) {
			if (Points.areEqual(testPointA, testPointA.getX(), testPointB.getX())) {
				return true;
			}
		}
		return Points.areEqual(Point.System.EUCLIDEAN, Math.abs(Polygons.getSlope(pointA, pointB)), Math.abs(Polygons.getSlope(testPointA, testPointB)));
	}

	public static void assertCCWPolygon(DiscreteRegion region) {
		assert Logs.openNode("CCW Polygon Assertions", "Asserting CCW-rotation of region.");
		assert Logs.addSnapNode("Tested region", region);
		if (region.isOptimized()) {
			assert Logs.closeNode("Region already optimized, returning.");
			return;
		}
		assert Logs.addSnapNode("Interior point", region.getInteriorPoint());
		PointSideStruct struct = Polygons.getPointSideList(region, region.getInteriorPoint());
		if (struct.hasIndeterminates()) {
			assert Logs.closeNode("Unanticipated colinear values in assertCCWPolygon.");
			return;
		}
		if (struct.getLeftPoints().isEmpty()) {
			region.reversePoints();
			assert Logs.closeNode("Polygon is clockwise, so reversing points and returning.");
		} else if (struct.getRightPoints().isEmpty()) {
			assert Logs.closeNode("Polygon is CCW, so returning.");
		}
	}

	public static DiscreteRegion clip(DiscreteRegion region, DiscreteRegion clip) {
		assert Logs.openNode("Clipping operations", "Clipping region");
		assert Logs.addSnapNode("Clip", clip);
		assert Logs.addSnapNode("Unclipped Region", region);
		DiscreteRegion clippedRegion = new DiscreteRegion(region);
		optimizePolygon(clippedRegion);
		optimizePolygon(clip);
		if (!getBoundingRectIntersection(clippedRegion, clip)) {
			assert Logs.closeNode("Region is entirely outside the clip,returning null.");
			return null;
		}
		List<Point> points = clip.getPoints();
		for (int i = 0; i < points.size(); i++) {
			PointSideStruct struct = getPointSideList(clippedRegion, points.get(i), points.get((i + 1) % points.size()));
			if (struct.getLeftPoints().size() != 0 && struct.getRightPoints().size() != 0) {
				DiscreteRegion interim = splitPolygonUsingEdge(clippedRegion, points.get(i), points.get((i + 1) % points.size()), true);
				if (interim != null) {
					assert Logs.openNode("Testing for polygon which is still valid");
					struct = getPointSideList(clippedRegion, points.get(i), points.get((i + 1) % points.size()));
					assert Logs.closeNode();
					if (struct.getLeftPoints().size() != 0) {
						clippedRegion = interim;
					}
					assert Logs.addSnapNode("Valid polygon", clippedRegion);
				}
			}
		}
		if (!getBoundingRectIntersection(clippedRegion, clip)) {
			assert Logs.closeNode("Region is entirely outside the clip,returning null.");
			return null;
		}
		assert Logs.closeNode();
		return clippedRegion;
	}

	// Confirms that the line formed by the two points is an interior line.
	public static boolean confirmInteriorLine(Point pointA, Point pointB, DiscreteRegion region) {
		List<Point> pointList = region.getPoints();
		assert Logs.openNode("ConfirmInteriorLine", "Confirming interior line with these points: " + pointA + ", " + pointB);
		assert Logs.addSnapNode("Region", region);
		assert Logs.addNode("Testing for intersections.");
		for (int i = 0; i < pointList.size(); i++) {
			Point testPointA = pointList.get(i);
			Point testPointB = pointList.get((i + 1) % pointList.size());
			if ((testPointA.equals(pointA) && testPointB.equals(pointB)) || (testPointA.equals(pointB) && testPointB.equals(pointA))) {
				assert Logs.closeNode();
				return false;
			}
			IntersectionPoint intersect = Polygons.getIntersection(pointA, pointB, testPointA, testPointB);
			if (intersect != null && !intersect.isTangent()) {
				assert Logs.addNode("Testing line intersected the polygon, returning false.");
				assert Logs.closeNode("Failing line: " + testPointA + ", " + testPointB);
				return false;
			}
		}
		assert Logs.addNode("Checking for crosses.");
		int crosses = Polygons.getCrosses(pointA.getX() + (pointB.getX() - pointA.getX()) / 2, pointA.getY() + (pointB.getY() - pointA.getY()) / 2, region);
		if (crosses == 0 || crosses % 2 == 0) {
			assert Logs.closeNode("Cross-test failed, returning false.");
			return false;
		}
		assert Logs.closeNode("Interior line confirmed, returning true.");
		return true;
	}

	// This process converts a concave polygon to a list of convex polygons.
	public static List<DiscreteRegion> convertPolyToConvex(DiscreteRegion originalRegion) {
		assert Logs.openNode("Polygon-to-convex conversions", "Attempting to convert this region into convex parts...");
		assert Logs.addSnapNode("Region", originalRegion);
		DiscreteRegion region = Polygons.optimizePolygon(originalRegion);
		assert Logs.addSnapNode("Optimized region", region);
		if (region == null) {
			return null;
		}
		List<DiscreteRegion> convexPolygons = new LinkedList<DiscreteRegion>();
		if (Polygons.isPolygonConvex(region) == true) {
			assert Logs.closeNode("This polygon is convex, so adding it to the list and returning.");
			convexPolygons.add(region);
			return convexPolygons;
		}
		assert Logs.addNode("This polygon is concave. Attempting to subdivide...");
		DiscreteRegion testRegion;
		List<Point> pointList = region.getPoints();
		for (int i = 0; i < pointList.size(); i++) {
			boolean alreadyCreated = false;
			assert Logs.openNode("Triangle Formations", "Attempting to form a triangle from these points");
			assert Logs.openNode("Points");
			assert Logs.addSnapNode("First point", pointList.get(i));
			assert Logs.addSnapNode("Second point", pointList.get((i + 1) % pointList.size()));
			assert Logs.addSnapNode("Third point", pointList.get((i + 2) % pointList.size()));
			assert Logs.closeNode(); // Node: Points
			if (Polygons.confirmInteriorLine(pointList.get(i), pointList.get((i + 2) % pointList.size()), region) == false) {
				assert Logs.closeNode("Failed interior-line test."); // Node: Triangle Formations
				continue;
			}
			testRegion = new DiscreteRegion(region.getEnvironment(), region.getProperties());
			testRegion.addPoint(pointList.get(i));
			testRegion.addPoint(pointList.get((i + 1) % pointList.size()));
			testRegion.addPoint(pointList.get((i + 2) % pointList.size()));
			for (int k = 0; k < convexPolygons.size(); k++) {
				if (testRegion.equals(convexPolygons.get(k))) {
					alreadyCreated = true;
					break;
				}
			}
			if (alreadyCreated) {
				assert Logs.closeNode("Polygon already created."); // Node: Triangle Formations
				continue;
			}
			assert Logs.addSnapNode("Removing this point", pointList.get((i + 1) % pointList.size()));
			region.removePoint(pointList.get((i + 1) % pointList.size()));
			assert Logs.addSnapNode("Yielding the new triangle and this remaining region: ", region);
			convexPolygons.add(testRegion);
			convexPolygons.addAll(Polygons.convertPolyToConvex(region));
			assert Logs.closeNode(); // Node: Triangle Formations
			break;
		}
		assert Logs.closeNode(); // Node: Convex conversions
		return convexPolygons;
	}

	// Converts the list of polygons into convex polygons, returning the list of these.
	public static List<DiscreteRegion> convertPolyToConvex(List<DiscreteRegion> polygons) {
		List<DiscreteRegion> list = new LinkedList<DiscreteRegion>();
		for (int i = 0; i < polygons.size(); i++) {
			list.addAll(Polygons.convertPolyToConvex(polygons.get(i)));
		}
		return list;
	}

	public static Point createPoint(Point referencePoint, String name, double x, double y, double z) {
		if (referencePoint instanceof EuclideanPoint) {
			return new EuclideanPoint(name, x, y, 0.0d);
		}
		return new PolarPoint(name, x, y, 0.0d);
	}

	// Using the two given points to create a line, it returns a list of the distribution of points from the polygon. 
	// This list contains two lists, one containing all the points on the left side, and one containing all the points on the right.
	public static void doPointSideTest(PointSideStruct struct, Point point, double value) {
		if (Points.areEqual(Point.System.EUCLIDEAN, value, 0.0d)) {
			struct.addIndeterminate(point);
		} else if (Points.isGreaterThan(value, 0.0d)) {
			struct.addRight(point, value);
		} else {
			struct.addLeft(point, value);
		}
	}

	public static Point findMiddlePoint(Point pointA, Point pointB, Point pointC) {
		assert Logs.openNode("Middle-Point Searches", "Finding middle point");
		assert Logs.openNode("Points");
		assert Logs.addSnapNode("Point A", pointA);
		assert Logs.addSnapNode("Point B", pointB);
		assert Logs.addSnapNode("Point C", pointC);
		assert Logs.closeNode(); // Node: Points
		if (Polygons.getBoundingRectIntersection(Math.min(pointA.getX(), pointB.getX()), Math.max(pointA.getX(), pointB.getX()), Math.min(pointA.getY(), pointB.getY()), Math.max(pointA.getY(), pointB.getY()), pointC, true)) {
			assert Logs.closeNode("Returning point C.");
			return pointC;
		}
		if (Polygons.getBoundingRectIntersection(Math.min(pointA.getX(), pointC.getX()), Math.max(pointA.getX(), pointC.getX()), Math.min(pointA.getY(), pointC.getY()), Math.max(pointA.getY(), pointC.getY()), pointB, true)) {
			assert Logs.closeNode("Returning point B.");
			return pointB;
		}
		assert Logs.closeNode("Returning point A.");
		return pointA;
	}

	public static Point[] getAdjacentEdge(DiscreteRegion region, DiscreteRegion neighbor) {
		assert Logs.openNode("Adjacent Edge Finding", "Finding Adjacent Edge");
		assert Logs.addSnapNode("Region", region);
		assert Logs.addSnapNode("Neighbor", neighbor);
		List<Point> pointList = region.getPoints();
		IntersectionPoint intersect = null;
		for (int i = 0; i < pointList.size(); i++) {
			List<Point> otherPointList = neighbor.getPoints();
			Point pointA = pointList.get(i);
			Point pointB = pointList.get((i + 1) % pointList.size());
			assert Logs.openNode("Line-by-Line Tests", "Control points: " + pointA + ", " + pointB);
			for (int j = 0; j < otherPointList.size(); j++) {
				Point pointC = otherPointList.get(j);
				Point pointD = otherPointList.get((j + 1) % otherPointList.size());
				assert Logs.openNode("Testing points (" + pointC + ", " + pointD + ")");
				PointSideStruct struct = Polygons.getPointSideList(pointA, pointB, pointC, pointD);
				if (struct.getIndeterminates().size() == 2) {
					Point minPoint, otherMinPoint;
					IntersectionPoint thisIntersect = Polygons.getIntersection(pointA, pointB, pointC, pointD);
					if (thisIntersect != null && !Polygons.getBoundingRectIntersection(pointA, pointB, pointC, pointD, false)) {
						intersect = thisIntersect;
						assert Logs.closeNode("They intersect on a tangent, saving and continuing (Intersection: " + thisIntersect + ")");
						continue;
					}
					if ((pointA.equals(pointC) && pointB.equals(pointD)) || (pointA.equals(pointD) && pointB.equals(pointC))) {
						assert Logs.addNode("Lines are identical.");
						minPoint = pointA;
						otherMinPoint = pointB;
					} else {
						assert Logs.addNode("Finding middle points...");
						if (pointA.equals(pointC)) {
							minPoint = findMiddlePoint(pointA, pointB, pointD);
							otherMinPoint = pointC;
						} else if (pointB.equals(pointC)) {
							minPoint = findMiddlePoint(pointA, pointD, pointB);
							otherMinPoint = pointC;
						} else if (pointA.equals(pointD)) {
							minPoint = findMiddlePoint(pointA, pointB, pointC);
							otherMinPoint = pointD;
						} else if (pointB.equals(pointD)) {
							minPoint = findMiddlePoint(pointA, pointB, pointC);
							otherMinPoint = pointD;
						} else {
							minPoint = findMiddlePoint(pointA, pointB, pointC);
							otherMinPoint = findMiddlePoint(pointB, pointC, pointD);
						}
					}
					Point[] pointArray = new Point[2];
					pointArray[0] = minPoint;
					pointArray[1] = otherMinPoint;
					assert Logs.closeNode();
					assert Logs.closeNode();
					assert Logs.closeNode("Returning point array", pointArray);
					return pointArray;
				}
				assert Logs.closeNode("No colinearity found, continuing.");
			}
			assert Logs.closeNode("No conclusive matches found for this point.");
		}
		if (intersect != null) {
			Point[] pointArray = new Point[2];
			pointArray[0] = intersect.getPoint();
			pointArray[1] = intersect.getPoint();
			assert Logs.closeNode("Returning point array", pointArray);
			return pointArray;
		}
		assert Logs.closeNode("They share no adjacent edge, returning null.");
		return null;
	}

	// Tests for bounding rect intersection between the two regions.
	public static boolean getBoundingRectIntersection(DiscreteRegion region, DiscreteRegion otherRegion) {
		if (region.getLeftExtreme() >= otherRegion.getRightExtreme()) {
			return false;
		}
		if (region.getRightExtreme() <= otherRegion.getLeftExtreme()) {
			return false;
		}
		if (region.getTopExtreme() <= otherRegion.getBottomExtreme()) {
			return false;
		}
		if (region.getBottomExtreme() >= otherRegion.getTopExtreme()) {
			return false;
		}
		return true;
	}

	public static boolean getBoundingRectIntersection(double xMin, double xMax, double yMin, double yMax, Point point, boolean allowTangent) {
		if (Points.isLessThan(point.getX(), xMin)) {
			return false;
		}
		if (Points.areEqual(point, point.getX(), xMin) && !allowTangent) {
			return false;
		}
		if (Points.isGreaterThan(point.getX(), xMax)) {
			return false;
		}
		if (Points.areEqual(point, point.getX(), xMax) && !allowTangent) {
			return false;
		}
		if (Points.isLessThan(point.getY(), yMin)) {
			return false;
		}
		if (Points.areEqual(point, point.getY(), yMin) && !allowTangent) {
			return false;
		}
		if (Points.isGreaterThan(point.getY(), yMax)) {
			return false;
		}
		if (Points.areEqual(point, point.getY(), yMax) && !allowTangent) {
			return false;
		}
		return true;
	}

	public static boolean getBoundingRectIntersection(Point pointA, Point pointB, DiscreteRegion region) {
		if (Points.isLessThan(Math.max(pointA.getX(), pointB.getX()), region.getLeftExtreme())) {
			return false;
		}
		if (Points.isGreaterThan(Math.min(pointA.getX(), pointB.getX()), region.getRightExtreme())) {
			return false;
		}
		if (Points.isLessThan(Math.max(pointA.getY(), pointB.getY()), region.getBottomExtreme())) {
			return false;
		}
		if (Points.isGreaterThan(Math.min(pointA.getY(), pointB.getY()), region.getTopExtreme())) {
			return false;
		}
		return true;
	}

	public static boolean getBoundingRectIntersection(Point pointA, Point pointB, Point pointC, Point pointD) {
		return getBoundingRectIntersection(pointA, pointB, pointC, pointD, true);
	}

	public static boolean getBoundingRectIntersection(Point pointA, Point pointB, Point pointC, Point pointD, boolean includeTangents) {
		if (pointA.equals(pointC) && pointB.equals(pointD)) {
			return true;
		}
		if (pointA.equals(pointD) && pointB.equals(pointC)) {
			return true;
		}
		if (Points.areEqual(Point.System.EUCLIDEAN, Math.abs(Polygons.getSlope(pointA, pointB)), Double.POSITIVE_INFINITY) && Points.areEqual(Point.System.EUCLIDEAN, Math.abs(Polygons.getSlope(pointC, pointD)), Double.POSITIVE_INFINITY)) {
			if (Points.areEqual(pointA, pointA.getX(), pointC.getX())) {
				return false;
			}
		} else {
			if (Points.isLessThan(Math.max(pointA.getX(), pointB.getX()), Math.min(pointC.getX(), pointD.getX()))) {
				return false;
			}
			if (Points.areEqual(pointA, Math.max(pointA.getX(), pointB.getX()), Math.min(pointC.getX(), pointD.getX())) && !includeTangents) {
				return false;
			}
			if (Points.isGreaterThan(Math.min(pointA.getX(), pointB.getX()), Math.max(pointC.getX(), pointD.getX()))) {
				return false;
			}
			if (Points.areEqual(pointA, Math.min(pointA.getX(), pointB.getX()), Math.max(pointC.getX(), pointD.getX())) && !includeTangents) {
				return false;
			}
		}
		if (Points.areEqual(pointA, Polygons.getSlope(pointA, pointB), 0.0d) && Points.areEqual(Point.System.EUCLIDEAN, Polygons.getSlope(pointC, pointD), 0.0d)) {
			if (!Points.areEqual(pointA, pointA.getY(), pointC.getY())) {
				return false;
			}
		} else {
			if (Points.isLessThan(Math.max(pointA.getY(), pointB.getY()), Math.min(pointC.getY(), pointD.getY()))) {
				return false;
			}
			if (Points.areEqual(pointA, Math.max(pointA.getY(), pointB.getY()), Math.min(pointC.getY(), pointD.getY())) && !includeTangents) {
				return false;
			}
			if (Points.isGreaterThan(Math.min(pointA.getY(), pointB.getY()), Math.max(pointC.getY(), pointD.getY()))) {
				return false;
			}
			if (Points.areEqual(pointA, Math.min(pointA.getY(), pointB.getY()), Math.max(pointC.getY(), pointD.getY())) && !includeTangents) {
				return false;
			}
		}
		return true;
	}

	// Tests how many times a line, using the coord's as the first point, and the extreme right point of the poly, crosses any other border.
	public static int getCrosses(double xCoord, double yCoord, DiscreteRegion region) {
		assert Logs.openNode("GetCrosses", "Testing for crosses for x-coord:" + xCoord + " and y-coord: " + yCoord);
		List<Point> pointList = region.getPoints();
		int crosses = 0;
		double xExtremeCoord = Double.NEGATIVE_INFINITY;
		for (Point testPoint : pointList) {
			if (testPoint.getX() > xExtremeCoord) {
				xExtremeCoord = testPoint.getX();
			}
		}
		xExtremeCoord += 1.0f;
		assert Logs.addNode("This line extends to x-coord: " + xExtremeCoord + " and y-coord: " + yCoord);
		Point crossExtremePoint = createPoint(pointList.get(0), "Extreme right-point", xExtremeCoord, yCoord, 0.0d);
		Point crossMidPoint = createPoint(pointList.get(0), "MidPoint", xCoord, yCoord, 0.0d);
		List<Point> overlappedVertices = new LinkedList<Point>();
		for (int i = 0; i < pointList.size(); i++) {
			Point testPointA = pointList.get(i);
			Point testPointB = pointList.get((i + 1) % pointList.size());
			if (testPointA.getY() == yCoord) {
				if (overlappedVertices.contains(testPointA)) {
					continue;
				} else {
					overlappedVertices.add(testPointA);
				}
			} else if (testPointB.getY() == yCoord) {
				if (overlappedVertices.contains(testPointB)) {
					continue;
				} else {
					overlappedVertices.add(testPointB);
				}
			}
			IntersectionPoint intersect = Polygons.getIntersection(crossMidPoint, crossExtremePoint, testPointA, testPointB);
			if (intersect == null || intersect.isTangent()) {
				continue;
			}
			crosses++;
		}
		assert Logs.closeNode("Total Crosses: " + crosses);
		return crosses;
	}

	// Takes two points and extends their line so that the x-value provided by the extension is at an endpoint of the line.
	public static double getExtensionPoint(Point pointA, Point pointB, double extension) {
		return Polygons.getSlope(pointA, pointB) * (extension - pointA.getX()) + pointA.getY();
	}

	// Extends a line, provided by the two points, in both directions so that their x coordinates are equal to the left and right double values provided with correct slope.
	public static List<Point> getExtensionPoints(Point pointA, Point pointB, DiscreteRegion region) {
		List<Point> pointList = new LinkedList<Point>();
		assert Logs.openNode("Point extensions", "Extending these points");
		assert Logs.addSnapNode("Point A", pointA);
		assert Logs.addSnapNode("Point B", pointB);
		assert Logs.addSnapNode("Region", region);
		double YValue = Polygons.getExtensionPoint(pointA, pointB, region.getLeftExtreme() - 1.0d);
		double XValue = region.getLeftExtreme() - 1.0d;
		if (Points.isGreaterThan(YValue, region.getTopExtreme())) {
			YValue = region.getTopExtreme() + 1.0d;
			XValue = (YValue - pointA.getY()) / Polygons.getSlope(pointA, pointB) + pointA.getX();
		} else if (Points.isLessThan(YValue, region.getBottomExtreme())) {
			YValue = region.getBottomExtreme() - 1.0d;
			XValue = (YValue - pointA.getY()) / Polygons.getSlope(pointA, pointB) + pointA.getX();
		}
		pointList.add(createPoint(pointA, pointA.getName(), XValue, YValue, 0.0d));
		YValue = Polygons.getExtensionPoint(pointA, pointB, region.getRightExtreme() + 1.0d);
		XValue = region.getRightExtreme() + 1.0d;
		if (Points.isGreaterThan(YValue, region.getTopExtreme())) {
			YValue = region.getTopExtreme() + 1.0d;
			XValue = (YValue - pointA.getY()) / Polygons.getSlope(pointA, pointB) + pointA.getX();
		} else if (Points.isLessThan(YValue, region.getBottomExtreme())) {
			YValue = region.getBottomExtreme() - 1.0d;
			XValue = (YValue - pointA.getY()) / Polygons.getSlope(pointA, pointB) + pointA.getX();
		}
		pointList.add(createPoint(pointA, pointB.getName(), XValue, YValue, 0.0d));
		assert Logs.addSnapNode("Extended Point A", pointList.get(0));
		assert Logs.addSnapNode("Extended Point B", pointList.get(1));
		assert Logs.closeNode();
		return pointList;
	}

	// Tests if (pointA, pointB) intersects (testPointA, testPointB), and returns the point if it does.
	public static IntersectionPoint getIntersection(Point pointA, Point pointB, Point testPointA, Point testPointB) {
		assert Logs.openNode("Intersection Tests (Line vs Line)", "Intersection Test: Line against Line");
		assert Logs.addNode("Control Points: " + pointA + ", " + pointB);
		assert Logs.addNode("Test Points: " + testPointA + ", " + testPointB);
		boolean tangentFlag = false;
		// Check for invalid lines.
		if (pointA.equals(pointB)) {
			assert Logs.closeNode("First control point is equal to second control point, returning null.");
			return null;
		}
		if (testPointA.equals(testPointB)) {
			assert Logs.closeNode("First test point is equal to second test point, returning null.");
			return null;
		}
		if ((pointA.equals(testPointB) && pointB.equals(testPointA)) || (pointA.equals(testPointA) && pointB.equals(testPointB))) {
			assert Logs.closeNode("These lines are equal, returning null.");
			return null;
		}
		if (pointA.equals(testPointA) || pointA.equals(testPointB)) {
			IntersectionPoint returning = new IntersectionPoint(pointA, true);
			assert Logs.closeNode("First control point is equal to one of the test points, yielding a tangent, so returning that tangent.", returning);
			return returning;
		}
		if (pointB.equals(testPointA) || pointB.equals(testPointB)) {
			IntersectionPoint returning = new IntersectionPoint(pointB, true);
			assert Logs.closeNode("Second control point is equal to one of the test points, yielding a tangent, so returning that tangent.", returning);
			return returning;
		}
		// Test for colinearity of these points.
		if (Polygons.testForColinearity(pointA, pointB, testPointA, testPointB)) {
			assert Logs.closeNode("These lines are colinear, returning null.");
			return null;
		}
		// Get slopes.
		double slope = Polygons.getSlope(pointA, pointB);
		double testSlope = Polygons.getSlope(testPointA, testPointB);
		// Test for infinite slopes, and if found, get real intersection points.
		if (Math.abs(slope) == Double.POSITIVE_INFINITY) {
			assert Logs.addNode("Slope of the control points is infinite.");
			if (Points.isGreaterThan(pointA.getX(), Math.max(testPointA.getX(), testPointB.getX()))) {
				assert Logs.closeNode("The X-value is greater than the maximum X-value of the test points, returning null.");
				return null;
			} else if (Points.areEqual(pointA, pointA.getX(), Math.max(testPointA.getX(), testPointB.getX()))) {
				assert Logs.addNode("The X-value is equal to the maximum X-value of the test points, setting tangent flag.");
				tangentFlag = true;
			}
			if (Points.isLessThan(pointA.getX(), Math.min(testPointA.getX(), testPointB.getX()))) {
				assert Logs.closeNode("The X-value is less than the minimum X-value of the test points, returning null.");
				return null;
			} else if (Points.areEqual(pointA, pointA.getX(), Math.min(testPointA.getX(), testPointB.getX()))) {
				assert Logs.addNode("The X-value is equal to the minimum X-value of the test points, setting tangent flag.");
				tangentFlag = true;
			}
			double yIntersection = testSlope * (pointA.getX() - testPointA.getX()) + testPointA.getY();
			if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, testSlope)) {
				assert Logs.addNode("The testSlope is not zero.");
				if (Points.isLessThan(yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the testPoints, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the testPoints, setting tangent flag.");
					tangentFlag = true;
				}
				if (Points.isGreaterThan(yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the testPoints, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the testpoints, setting tangent flag.");
					tangentFlag = true;
				}
			}
			if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, slope)) {
				assert Logs.addNode("The slope is not zero.");
				if (Points.isLessThan(yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the control points, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the control points, setting tangent flag.");
					tangentFlag = true;
				}
				if (Points.isGreaterThan(yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the control points, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the control points, setting tangent flag.");
					tangentFlag = true;
				}
			}
			Point intersectPoint = createPoint(pointA, String.format("(%s, %s)+(%s, %s)", pointA.getName(), pointB.getName(), testPointA.getName(), testPointB.getName()), pointA.getX(), yIntersection, 0.0f);
			IntersectionPoint point = new IntersectionPoint(intersectPoint, tangentFlag);
			if (tangentFlag) {
				assert Logs.closeNode("These lines intersect, but only on a tangent: " + intersectPoint, point);
			} else {
				assert Logs.closeNode("These lines intersect: " + intersectPoint, point);
			}
			return point;
		}
		if (Math.abs(testSlope) == Double.POSITIVE_INFINITY) {
			assert Logs.addNode("Slope of the test points is infinite.");
			if (Points.isGreaterThan(testPointA.getX(), Math.max(pointA.getX(), pointB.getX()))) {
				assert Logs.closeNode("The X-value is greater than the maximum X-value of the control points, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, testPointA.getX(), Math.max(pointA.getX(), pointB.getX()))) {
				assert Logs.addNode("The X-value is equal to the maximum X-value of the control points, setting tangent flag.");
				tangentFlag = true;
			}
			if (Points.isLessThan(testPointA.getX(), Math.min(pointA.getX(), pointB.getX()))) {
				assert Logs.closeNode("The X-value is less than the minimum X-value of the control points, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, testPointA.getX(), Math.min(pointA.getX(), pointB.getX()))) {
				assert Logs.addNode("The X-value is equal to the minimum X-value of the control points, setting tangent flag.");
				tangentFlag = true;
			}
			double yIntersection = slope * (testPointA.getX() - pointA.getX()) + pointA.getY();
			if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, testSlope)) {
				assert Logs.addNode("The testSlope is not zero.");
				if (Points.isLessThan(yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the testPoints, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the testPoints, setting tangent flag.");
					tangentFlag = true;
				}
				if (Points.isGreaterThan(yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the testPoints, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the testpoints, setting tangent flag.");
					tangentFlag = true;
				}
			}
			if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, slope)) {
				assert Logs.addNode("The slope is not zero.");
				if (Points.isLessThan(yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the control points, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the control points, setting tangent flag.");
					tangentFlag = true;
				}
				if (Points.isGreaterThan(yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
					assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the control points, returning null.");
					return null;
				} else if (Points.areEqual(testPointA, yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
					assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the control points, setting tangent flag.");
					tangentFlag = true;
				}
			}
			Point intersectPoint = createPoint(pointA, "(" + pointA.getName() + "," + pointB.getName() + ")+(" + testPointA.getName() + "," + testPointB.getName() + ")", testPointA.getX(), yIntersection, 0.0f);
			IntersectionPoint returning = new IntersectionPoint(intersectPoint, tangentFlag);
			if (tangentFlag) {
				assert Logs.closeNode("These lines intersect, but only on a tangent: " + intersectPoint, returning);
			} else {
				assert Logs.closeNode("These lines intersect: " + intersectPoint, returning);
			}
			return returning;
		}
		// Bounding rect testing of the two lines.
		if (Points.isLessThan(Math.max(pointB.getX(), pointA.getX()), Math.min(testPointA.getX(), testPointB.getX()))) {
			assert Logs.closeNode("The maximum x-value of the control points is less than the minimum X-value of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, Math.max(pointB.getX(), pointA.getX()), Math.min(testPointA.getX(), testPointB.getX()))) {
			assert Logs.addNode("The maximum x-value of the control points is equal to the minimum X-value of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isGreaterThan(Math.min(pointB.getX(), pointA.getX()), Math.max(testPointA.getX(), testPointB.getX()))) {
			assert Logs.closeNode("The minimum x-value of the control points is greater than the maximum X-value of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, Math.min(pointB.getX(), pointA.getX()), Math.max(testPointA.getX(), testPointB.getX()))) {
			assert Logs.addNode("The minimum x-value of the control points is equal to the maximum X-value of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isLessThan(Math.max(pointA.getY(), pointB.getY()), Math.min(testPointA.getY(), testPointB.getY()))) {
			assert Logs.closeNode("The maximum Y-value of the control points is less than the minimum Y-value of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, Math.max(pointA.getY(), pointB.getY()), Math.min(testPointA.getY(), testPointB.getY()))) {
			assert Logs.addNode("The maximum Y-value of the control points is equal to the minimum  Y-value of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isGreaterThan(Math.min(pointA.getY(), pointB.getY()), Math.max(testPointA.getY(), testPointB.getY()))) {
			assert Logs.closeNode("The minimum Y-value of the control points is greater than the maximum Y-value of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, Math.min(pointA.getY(), pointB.getY()), Math.max(testPointA.getY(), testPointB.getY()))) {
			assert Logs.addNode("The minimum Y-value of the control points is equal to than the maximum Y-value of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		// X-intersection testing.
		double xIntersection = ((-slope * pointA.getX() + pointA.getY()) - (-testSlope * testPointA.getX() + testPointA.getY())) / (testSlope - slope);
		assert Logs.addNode("The X-intercept between these two points is: " + xIntersection);
		if (Points.isLessThan(xIntersection, Math.min(testPointA.getX(), testPointB.getX()))) {
			assert Logs.closeNode("The X-intercept is less than the minimum of the X-values of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, xIntersection, Math.min(testPointA.getX(), testPointB.getX()))) {
			assert Logs.addNode("The X-intercept is equal to the minimum of the X-values of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isGreaterThan(xIntersection, Math.max(testPointA.getX(), testPointB.getX()))) {
			assert Logs.closeNode("The X-intercept is greater than the maximum of the X-values of the testPoints, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, xIntersection, Math.max(testPointA.getX(), testPointB.getX()))) {
			assert Logs.addNode("The X-intercept is equal to the maximum of the X-values of the testPoints, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isLessThan(xIntersection, Math.min(pointA.getX(), pointB.getX()))) {
			assert Logs.closeNode("The X-intercept is less than the minimum of the X-values of the control points, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, xIntersection, Math.min(pointA.getX(), pointB.getX()))) {
			assert Logs.addNode("The X-intercept is equal to the minimum of the X-values of the control points, setting tangent flag.");
			tangentFlag = true;
		}
		if (Points.isGreaterThan(xIntersection, Math.max(pointA.getX(), pointB.getX()))) {
			assert Logs.closeNode("The X-intercept is greater than the maximum of the X-values of the control points, returning null.");
			return null;
		} else if (Points.areEqual(testPointA, xIntersection, Math.max(pointA.getX(), pointB.getX()))) {
			assert Logs.addNode("The X-intercept is equal to the maximum of the X-values of the control points, setting tangent flag.");
			tangentFlag = true;
		}
		// Y-intersection testing.
		double yIntersection = slope * xIntersection + (-slope * pointA.getX() + pointA.getY());
		assert Logs.addNode("The Y-intercept between these two points is: " + yIntersection);
		if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, testSlope)) {
			assert Logs.addNode("The testSlope is not zero.");
			if (Points.isLessThan(yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
				assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the testPoints, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, yIntersection, Math.min(testPointA.getY(), testPointB.getY()))) {
				assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the testPoints, setting tangent flag.");
				tangentFlag = true;
			}
			if (Points.isGreaterThan(yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
				assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the testPoints, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, yIntersection, Math.max(testPointA.getY(), testPointB.getY()))) {
				assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the testpoints, setting tangent flag.");
				tangentFlag = true;
			}
		}
		if (!Points.areEqual(Point.System.EUCLIDEAN, 0.0d, slope)) {
			assert Logs.addNode("The slope is not zero.");
			if (Points.isLessThan(yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
				assert Logs.closeNode("The Y-intercept is less than the minimum of the Y-values of the control points, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, yIntersection, Math.min(pointA.getY(), pointB.getY()))) {
				assert Logs.addNode("The Y-intercept is equal to the minimum of the Y-values of the control points, setting tangent flag.");
				tangentFlag = true;
			}
			if (Points.isGreaterThan(yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
				assert Logs.closeNode("The Y-intercept is greater than the maximum of the Y-values of the control points, returning null.");
				return null;
			} else if (Points.areEqual(testPointA, yIntersection, Math.max(pointA.getY(), pointB.getY()))) {
				assert Logs.addNode("The Y-intercept is equal to the maximum of the Y-values of the control points, setting tangent flag.");
				tangentFlag = true;
			}
		}
		Point intersectPoint = createPoint(pointA, "(" + pointA.getName() + "," + pointB.getName() + ")+(" + testPointA.getName() + "," + testPointB.getName() + ")", xIntersection, yIntersection, 0.0f);
		IntersectionPoint returning = new IntersectionPoint(intersectPoint, tangentFlag);
		// New point creation of intersection.
		if (tangentFlag) {
			assert Logs.closeNode("These lines intersect, but only on a tangent: " + intersectPoint, returning);
		} else {
			assert Logs.closeNode("These lines intersect: " + intersectPoint, returning);
		}
		return returning;
	}

	// Tests for intersections of this line provided by the two points against the lines of the region provided.
	public static List<RiffIntersectionPoint> getIntersections(Point pointA, Point pointB, DiscreteRegion region) {
		assert Logs.openNode("Intersection Tests (Line vs Region)", "Intersection Test: Line against Region");
		assert Logs.addNode(region);
		assert Logs.addNode("Line: " + pointA + ", " + pointB);
		List<Point> pointList = region.getPoints();
		List<RiffIntersectionPoint> intersectPoints = new LinkedList<RiffIntersectionPoint>();
		if (!Polygons.getBoundingRectIntersection(pointA, pointB, region)) {
			assert Logs.closeNode("Bounding-rect test between the line and the region returned false, returning empty list.");
			return intersectPoints;
		}
		for (int i = 0; i < pointList.size(); i++) {
			IntersectionPoint intersectPoint = Polygons.getIntersection(pointA, pointB, pointList.get(i), pointList.get((i + 1) % pointList.size()));
			if (intersectPoint != null) {
				RiffIntersectionPoint intersect = new RiffIntersectionPoint(intersectPoint.getPoint(), i);
				if (!intersectPoints.contains(intersect)) {
					intersectPoints.add(intersect);
				}
			}
			/*
			 * boolean tangentFlag = false;
			if(intersectPoint==null){
				if(tangentFlag){
					assert Debugger.addNode("Resetting tangent flag.");
					tangentFlag=false;
				}
			}else if(intersectPoint.isTangent()){
				if(intersectPoint.getPoint().equals(pointList.get(0))){
					assert Debugger.addNode("Setting tangentFlag to true (firstTangentFlag is true)");
					tangentFlag=true;
				}
				if(!tangentFlag){
					tangentFlag=true;
					assert Debugger.addNode("Setting the tangent flag.");
				}else{
					if(!intersectPoint.getPoint().equals(pointList.get(0))&&!intersectPoint.getPoint().equals(pointList.get(i))){
						assert Debugger.addNode("firstTangentFlag is unset, but our point isn't equal to the correct point, so continuing.");
						continue;
					}
					tangentFlag=false;
					intersectPoints.add(new RiffIntersectionPoint(pointList.get(i),i));
					assert Debugger.addSnapNode("Adding this tangent intersect point",intersectPoints.get(intersectPoints.size()-1));
				}
			}else{
				tangentFlag=false;
				intersectPoints.add(new RiffIntersectionPoint(intersectPoint.getPoint(),i));
				assert Debugger.addSnapNode("Adding this intersect point",intersectPoints.get(intersectPoints.size()-1));
			}*/
		}
		assert Logs.closeNode("Returning list (" + intersectPoints.size() + " intersection(s))", intersectPoints);
		return intersectPoints;
	}

	public static Point getMidPointOfLine(Point pointA, Point pointB) {
		double xValue = Math.min(pointA.getX(), pointB.getX()) + .5 * (Math.max(pointA.getX(), pointB.getX()) - Math.min(pointA.getX(), pointB.getX()));
		double yValue = Math.min(pointA.getY(), pointB.getY()) + .5 * (Math.max(pointA.getY(), pointB.getY()) - Math.min(pointA.getY(), pointB.getY()));
		Point point = createPoint(pointA, null, xValue, yValue, 0.0d);
		return point;
	}

	public static Point getMinimumPointBetweenLine(Point pointA, Point pointB, Point source) {
		assert Logs.openNode("Minimum Point Searches", "Getting minimum point");
		assert Logs.addSnapNode("Source", source);
		assert Logs.addSnapNode("Point A", pointA);
		assert Logs.addSnapNode("Point B", pointB);
		if (pointA.equals(pointB)) {
			assert Logs.closeNode("PointA is equal to PointB, returning pointA.");
			return pointA;
		}
		double omegaValue = ((source.getX() - pointA.getX()) * (pointB.getX() - pointA.getX()) + (source.getY() - pointA.getY()) * (pointB.getY() - pointA.getY())) / Math.pow(Points.getDistance(pointA, pointB), 2);
		double xValue = pointA.getX() + omegaValue * (pointB.getX() - pointA.getX());
		double yValue = pointA.getY() + omegaValue * (pointB.getY() - pointA.getY());
		Point minimumPoint = createPoint(pointA, null, xValue, yValue, 0.0d);
		assert Logs.addSnapNode("Yielded point", minimumPoint);
		try {
			return Polygons.findMiddlePoint(pointA, pointB, minimumPoint);
		} finally {
			assert Logs.closeNode();
		}
	}

	public static PointSideStruct getPointSideList(DiscreteRegion region, Point testPoint) {
		List<Point> pointList = region.getPoints();
		assert Logs.openNode("Point-Side Tests", "Point-Side Test (Point vs. Region)");
		assert Logs.addNode("Test-Point: " + testPoint);
		assert Logs.addSnapNode("Testing-Region", region);
		PointSideStruct struct = new PointSideStruct();
		for (int k = 0; k < pointList.size(); k++) {
			Point linePointA = pointList.get(k);
			Point linePointB = pointList.get((k + 1) % pointList.size());
			if (Polygons.testForColinearity(linePointA, linePointB, testPoint)) {
				struct.addIndeterminate(testPoint);
				continue;
			}
			doPointSideTest(struct, testPoint, Polygons.testPointAgainstLine(testPoint, linePointA, linePointB));
		}
		struct.validate();
		assert Logs.closeNode(struct);
		return struct;
	}

	public static PointSideStruct getPointSideList(DiscreteRegion region, Point linePointA, Point linePointB) {
		assert Logs.openNode("Point-Side Tests", "Point-Side Test (Region vs. Line)");
		assert Logs.addSnapNode("Test-Line", linePointA + ", " + linePointB);
		assert Logs.addSnapNode("Region", region);
		List<Point> pointList = region.getPoints();
		PointSideStruct struct = new PointSideStruct();
		for (int k = 0; k < pointList.size(); k++) {
			Point testPoint = pointList.get(k);
			if (Polygons.testForColinearity(linePointA, linePointB, testPoint)) {
				struct.addIndeterminate(testPoint);
				continue;
			}
			doPointSideTest(struct, testPoint, Polygons.testPointAgainstLine(testPoint, linePointA, linePointB));
		}
		struct.validate();
		assert Logs.closeNode(struct);
		return struct;
	}

	public static PointSideStruct getPointSideList(Point linePointA, Point linePointB, Point testPointA, Point testPointB) {
		assert Logs.openNode("Point-Side Tests", "Point-Side Test (Line vs. Line)");
		assert Logs.addSnapNode("First-Line", linePointA + ", " + linePointB);
		assert Logs.addSnapNode("Test-Line", testPointA + ", " + testPointB);
		PointSideStruct struct = new PointSideStruct();
		if (Polygons.testForColinearity(linePointA, linePointB, testPointA)) {
			struct.addIndeterminate(testPointA);
		} else {
			doPointSideTest(struct, testPointA, Polygons.testPointAgainstLine(testPointA, linePointA, linePointB));
		}
		if (Polygons.testForColinearity(linePointA, linePointB, testPointB)) {
			struct.addIndeterminate(testPointB);
		} else {
			doPointSideTest(struct, testPointB, Polygons.testPointAgainstLine(testPointB, linePointA, linePointB));
		}
		struct.validate();
		assert Logs.closeNode(struct);
		return struct;
	}

	/**
	 * Returns the slope of the line that passes through both specified points.
	 * The line segment begins at {@code start} and is directed towards
	 * {@code end}.
	 * 
	 * @param start
	 *            the first point of the line
	 * @param end
	 *            the second point of the line
	 * @return the slope of the line formed by the specified points
	 */
	public static double getSlope(Point start, Point end) {
		return (end.getY() - start.getY()) / (end.getX() - start.getX());
	}

	/**
	 * Tests if a polygon is convex.
	 * 
	 * @param region
	 *            the tested region
	 * @return {@code true} if the region is convex
	 */
	public static boolean isPolygonConvex(DiscreteRegion region) {
		List<Point> pointList = region.getPoints();
		Boolean myBool = null;
		for (int i = 0; i < pointList.size(); i++) {
			Point linePointA = pointList.get(i);
			Point linePointB = pointList.get((i + 1) % pointList.size());
			for (int k = 0; k < pointList.size(); k++) {
				Point testPoint = pointList.get(k);
				//(y - y0) (x1 - x0) - (x - x0) (y1 - y0)
				double value = (testPoint.getY() - linePointA.getY()) * (linePointB.getX() - linePointA.getX()) - (testPoint.getX() - linePointA.getX()) * (linePointB.getY() - linePointA.getY());
				if (myBool == null) {
					if (value > 0) {
						myBool = new Boolean(true);
					} else if (value < 0) {
						myBool = new Boolean(false);
					}
				} else {
					if (value > 0) {
						if (myBool.booleanValue() == true) {
							continue;
						}
						return false;
					} else if (value < 0) {
						if (myBool.booleanValue() == false) {
							continue;
						}
						return false;
					} else {
						continue;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Takes a list of convex, non-overlapping polygons, and joins them if they
	 * share a common border and their joined polygon is convex.
	 * 
	 * @param originals
	 *            the list of original convex polygons.
	 * @return a list of convex polygons, joined where applicable
	 */
	public static List<DiscreteRegion> joinPolygons(List<DiscreteRegion> originals) {
		List<DiscreteRegion> optimizedList = new LinkedList<DiscreteRegion>();
		for (int i = 0; i < originals.size(); i++) {
			for (int j = 0; j < originals.size(); j++) {
				if (i == j) {
					continue;
				}
				DiscreteRegion firstRegion = originals.get(i);
				DiscreteRegion secondRegion = originals.get(j);
				for (int q = 0; q < firstRegion.getPoints().size(); q++) {
					for (int x = 0; x < secondRegion.getPoints().size(); x++) {
						if (!firstRegion.getPoints().get(q).equals(secondRegion.getPoints().get(x)) && !firstRegion.getPoints().get((q + 1) % firstRegion.getPoints().size()).equals(secondRegion.getPoints().get(x))) {
							continue;
						}
						if (!firstRegion.getPoints().get(q).equals(secondRegion.getPoints().get((x + 1) % secondRegion.getPoints().size())) && !firstRegion.getPoints().get((q + 1) % firstRegion.getPoints().size()).equals(secondRegion.getPoints().get((x + 1) % secondRegion.getPoints().size()))) {
							continue;
						}
						DiscreteRegion testRegion = new DiscreteRegion(firstRegion.getEnvironment(), firstRegion.getProperties());
						PointSideStruct struct = Polygons.getPointSideList(firstRegion, firstRegion.getPoints().get(q), firstRegion.getPoints().get((q + 1) % firstRegion.getPoints().size()));
						if (struct.getLeftPoints().isEmpty()) {
							int firstPoint = q;
							if (q > (q + 1) % firstRegion.getPoints().size()) {
								firstPoint = (q + 1) % firstRegion.getPoints().size();
							}
							for (int offsetPoint = 0; offsetPoint < firstRegion.getPoints().size(); offsetPoint++) {
								testRegion.addPoint(firstRegion.getPoints().get((firstPoint + offsetPoint) % firstRegion.getPoints().size()));
							}
							firstPoint = x;
							if (x > (x + 1) % secondRegion.getPoints().size()) {
								firstPoint = (x + 1) % secondRegion.getPoints().size();
							}
							for (int offsetPoint = 1; offsetPoint < secondRegion.getPoints().size(); offsetPoint++) {
								testRegion.addPoint(secondRegion.getPoints().get((firstPoint + offsetPoint) % secondRegion.getPoints().size()));
							}
						} else {
							int firstPoint = x;
							if (x > (x + 1) % secondRegion.getPoints().size()) {
								firstPoint = (x + 1) % secondRegion.getPoints().size();
							}
							for (int offsetPoint = 0; offsetPoint < secondRegion.getPoints().size(); offsetPoint++) {
								testRegion.addPoint(secondRegion.getPoints().get((firstPoint + offsetPoint) % secondRegion.getPoints().size()));
							}
							firstPoint = q;
							if (q > (q + 1) % firstRegion.getPoints().size()) {
								firstPoint = (q + 1) % firstRegion.getPoints().size();
							}
							for (int offsetPoint = 1; offsetPoint < firstRegion.getPoints().size(); offsetPoint++) {
								testRegion.addPoint(firstRegion.getPoints().get((firstPoint + offsetPoint) % firstRegion.getPoints().size()));
							}
						}
						if (!Polygons.isPolygonConvex(testRegion)) {
							continue;
						}
						optimizedList.add(testRegion);
						// XXX I would love to make originals be unmodified by this operation, but I can't guarantee
						// these removals aren't necessary.
						originals.remove(firstRegion);
						originals.remove(secondRegion);
						optimizedList.addAll(originals);
						return Polygons.joinPolygons(optimizedList);
					}
				}
			}
		}
		return originals;
	}

	/**
	 * Removes overlapping points, points that are not essential, and also
	 * confirms that the polygon is at least three points, and invalidate
	 * self-intersecting polygons.
	 * 
	 * @param region
	 *            the region that will be optimized
	 * @return {@code region} if it has been optimized, {@code null} if it is
	 *         invalid
	 */
	public static DiscreteRegion optimizePolygon(DiscreteRegion region) {
		if (region.isOptimized()) {
			return region;
		}
		assert Logs.openNode("Optimizing Polygons", "Optimizing Polygon");
		assert Logs.addNode(region);
		List<Point> pointList = region.getPoints();
		// Fail if not at least a triangle
		if (pointList.size() < 3) {
			assert Logs.closeNode("Polygon invalid because it has less than 3 points, returning null.");
			return null;
		}
		Polygons.assertCCWPolygon(region);
		// Remove all overlapping points.
		for (int i = 0; i < pointList.size(); i++) {
			for (int j = 0; j < pointList.size(); j++) {
				if (i == j) {
					continue;
				}
				if (pointList.get(i).equals(pointList.get(j))) {
					assert Logs.addNode("Overlapping Point Removal", "Removing this point because it is redundant: " + pointList.get(j));
					pointList.remove(j);
					j--;
				}
			}
		}
		// Remove all unnecessary points.
		for (int i = 0; i < pointList.size(); i++) {
			if (pointList.size() < 3) {
				assert Logs.closeNode("Polygon invalid because it has less than 3 points, returning null.");
				return null;
			}
			Point pointA = pointList.get(i);
			for (int j = 1; j < pointList.size(); j++) {
				if (i == j) {
					continue;
				}
				Point pointB = pointList.get(j);
				double slope = Polygons.getSlope(pointA, pointB);
				for (int k = 0; k < pointList.size(); k++) {
					if (k == i || k == j) {
						continue;
					}
					Point pointC = pointList.get(k);
					double testSlope = Polygons.getSlope(pointA, pointC);
					if (testSlope == slope) {
						double y = slope * (pointC.getX() - pointA.getX()) + pointA.getY();
						if (y == pointC.getY()) {
							if (Points.getDistance(pointA, pointB) + Points.getDistance(pointB, pointC) == Points.getDistance(pointA, pointC)) {
								if (Polygons.confirmInteriorLine(pointA, pointC, region)) {
									assert Logs.addNode("Redundant Colinear Point Removals", "Removing this point:" + pointB);
									assert Logs.addNode("Redundant Colinear Point Removals", "More optimal line: " + pointA + ", " + pointC);
									pointList.remove(j);
									j--;
								}
								continue;
							}
						} else if (Points.getDistance(pointB, pointC) + Points.getDistance(pointA, pointC) == Points.getDistance(pointA, pointB)) {
							if (Polygons.confirmInteriorLine(pointA, pointB, region)) {
								assert Logs.addNode("Redundant Colinear Point Removals", "Removing this point:" + pointC);
								assert Logs.addNode("Redundant Colinear Point Removals", "More optimal line: " + pointA + ", " + pointB);
								pointList.remove(k);
								k--;
							}
							continue;
						} else if (Points.getDistance(pointB, pointA) + Points.getDistance(pointA, pointC) == Points.getDistance(pointC, pointB)) {
							if (Polygons.confirmInteriorLine(pointC, pointB, region)) {
								assert Logs.addNode("Redundant Colinear Point Removals", "Removing this point:" + pointA);
								assert Logs.addNode("Redundant Colinear Point Removals", "More optimal line: " + pointC + ", " + pointB);
								pointList.remove(i);
								i--;
							}
							continue;
						}
					}
				}
			}
		}
		// Test for self-intersections
		for (int i = 0; i < pointList.size(); i++) {
			for (int j = 0; j < pointList.size(); j++) {
				if (i == j) {
					continue;
				}
				IntersectionPoint point = Polygons.getIntersection(pointList.get(i), pointList.get((i + 1) % pointList.size()), pointList.get(j), pointList.get((j + 1) % pointList.size()));
				if (point == null || point.isTangent()) {
					continue;
				}
				assert Logs.closeNode("Polygon intersects itself and is invalid, returning null.");
				return null;
			}
		}
		// Fail if not at least a triangle
		if (pointList.size() < 3) {
			assert Logs.closeNode("Polygon invalid because it has less than 3 points, returning null.");
			return null;
		}
		region.setPointList(pointList);
		region.setOptimized(true);
		assert Logs.closeNode("Optimization complete", region);
		return region;
	}

	/**
	 * Optimizes each {@link DiscreteRegion} in the specified collection.
	 * 
	 * @param regions
	 *            the regions to optimize
	 * @return a list of optimized regions
	 */
	public static List<DiscreteRegion> optimizePolygons(Collection<DiscreteRegion> regions) {
		List<DiscreteRegion> polygons = new LinkedList<DiscreteRegion>();
		for (DiscreteRegion region : regions) {
			region = Polygons.optimizePolygon(region);
			if (region == null) {
				continue;
			}
			polygons.add(region);
		}
		return polygons;
	}

	/**
	 * Removes overlapping polygons and creates new ones with the
	 * characteristics of the previous two merged together.
	 * <p>
	 * These method parameters are a bit weird, due to the recursion. This
	 * really should be a private method with a more-sensible public interface
	 * method.
	 * 
	 * 
	 * @param root
	 *            the root node of the BSP tree
	 * @param region
	 *            the region that will be added to the BSP tree
	 * @param recurse
	 *            whether to recurse, optimizing any formed regions
	 * @return the node of the created BSP tree
	 */
	public static DiscreteRegionBSPNode removeOverlappingPolygons(DiscreteRegionBSPNode root, DiscreteRegion region, boolean recurse) {
		assert Logs.openNode("Overlapping Polygon Removals", "Removing Overlapping Polygons");
		if (region == null || root == null) {
			assert Logs.closeNode("Region or root are null, returning root.");
			return root;
		}
		assert Logs.addNode(region);
		Set<DiscreteRegion> potentialList = root.getPotentialList(region);
		//RiffPolygonToolbox.snapVertices(potentialList, region);
		if (potentialList == null || potentialList.size() == 0) {
			assert Logs.addNode("Potential intersection list from BSP tree is null or zero-size, adding region to tree.");
			root.addRegion(region);
			root.removeFromTempList(region);
			assert Logs.closeNode();
			return root;
		}
		List<Point> regionPoints = region.getPoints();
		Iterator<DiscreteRegion> iter = potentialList.iterator();
		assert Logs.openNode("Beginning overlap-check sequence...");
		while (iter.hasNext()) {
			DiscreteRegion otherRegion = iter.next();
			assert Logs.addSnapNode("Compared region", otherRegion);
			if (otherRegion != region && otherRegion.equals(region)) {
				assert Logs.addNode("Regions are equal, adding this regions assets to that and returning root.");
				otherRegion.setProperty("Archetypes", region.getProperty("Archetypes"));
				assert Logs.closeNode();
				assert Logs.closeNode();
				return root;
			}
			List<Point> otherRegionPoints = otherRegion.getPoints();
			if (region.checkClearedRegionMap(otherRegion)) {
				assert Logs.addNode("Cleared-region check returned true.");
				potentialList.remove(otherRegion);
				iter = potentialList.iterator();
				continue;
			}
			if (!Polygons.getBoundingRectIntersection(region, otherRegion)) {
				assert Logs.addNode("Polygons do not intersect with their bounding rects.");
				region.addRegionToMap(otherRegion);
				otherRegion.addRegionToMap(region);
				potentialList.remove(otherRegion);
				iter = potentialList.iterator();
				continue;
			}
			if (!Polygons.testforRegionPointSideIntersection(region, otherRegion) && !Polygons.testforRegionPointSideIntersection(otherRegion, region)) {
				assert Logs.addNode("Polygons do not intersect according to the point-side polygon test.");
				region.addRegionToMap(otherRegion);
				otherRegion.addRegionToMap(region);
				potentialList.remove(otherRegion);
				iter = potentialList.iterator();
				continue;
			}
			assert Logs.openNode("Beginning primary line-by-line overlap-check sequence.");
			for (int k = 0; k < regionPoints.size(); k++) {
				assert Logs.addNode("Now testing using this potentially intersecting line: " + regionPoints.get(k) + ", " + regionPoints.get((k + 1) % regionPoints.size()));
				if (!Polygons.getBoundingRectIntersection(regionPoints.get(k), regionPoints.get((k + 1) % regionPoints.size()), otherRegion)) {
					assert Logs.addNode("The current line's bounding rect does not overlap the other region's.");
					continue;
				}
				DiscreteRegion splittingRegion = new DiscreteRegion(otherRegion);
				DiscreteRegion newRegion = Polygons.splitPolygonUsingEdge(splittingRegion, regionPoints.get(k), regionPoints.get((k + 1) % regionPoints.size()), false);
				if (newRegion == null) {
					assert Logs.addNode("New-region is null, continuing...");
					continue;
				} else {
					assert Logs.openNode("Split-test confirmed and new region created.");
					assert Logs.addSnapNode("New region", newRegion);
					assert Logs.addSnapNode("Other region", splittingRegion);
					assert Logs.addSnapNode("Original region", otherRegion);
					assert Logs.closeNode();
					root.removeRegion(otherRegion);
					if (recurse) {
						assert Logs.addNode("Recursing.");
						root = Polygons.removeOverlappingPolygons(root, splittingRegion, recurse);
						root = Polygons.removeOverlappingPolygons(root, newRegion, recurse);
						root = Polygons.removeOverlappingPolygons(root, region, recurse);
						assert Logs.closeNode();
						assert Logs.closeNode();
						assert Logs.closeNode();
						return root;
					} else {
						root.addToTempList(splittingRegion);
						root.addToTempList(newRegion);
						root.addToTempList(region);
						assert Logs.closeNode();
						assert Logs.closeNode();
						assert Logs.closeNode();
						return root;
					}
				}
			}
			assert Logs.closeNode();
			assert Logs.openNode("Beginning counter line-by-line overlap-check sequence.");
			for (int k = 0; k < otherRegionPoints.size(); k++) {
				assert Logs.addNode("Now testing using this potentially intersecting line: " + otherRegionPoints.get(k) + ", " + otherRegionPoints.get((k + 1) % otherRegionPoints.size()));
				if (!Polygons.getBoundingRectIntersection(otherRegionPoints.get(k), otherRegionPoints.get((k + 1) % otherRegionPoints.size()), region)) {
					assert Logs.addNode("The current line's bounding rect does not overlap the other region's.");
					continue;
				}
				DiscreteRegion oldRegion = new DiscreteRegion(region);
				DiscreteRegion newRegion = Polygons.splitPolygonUsingEdge(region, otherRegionPoints.get(k), otherRegionPoints.get((k + 1) % otherRegionPoints.size()), false);
				if (newRegion == null) {
					assert Logs.addNode("New-region is null.");
					continue;
				} else {
					assert Logs.openNode("Split-test confirmed and new region created.");
					assert Logs.addSnapNode("New region", newRegion);
					assert Logs.addSnapNode("Other region", oldRegion);
					assert Logs.addSnapNode("Original region", otherRegion);
					assert Logs.closeNode();
					if (recurse) {
						assert Logs.addNode("Recursing.");
						root = Polygons.removeOverlappingPolygons(root, newRegion, recurse);
						root = Polygons.removeOverlappingPolygons(root, region, recurse);
						assert Logs.closeNode();
						assert Logs.closeNode();
						assert Logs.closeNode();
						return root;
					} else {
						root.addToTempList(newRegion);
						root.addToTempList(region);
						assert Logs.closeNode();
						assert Logs.closeNode();
						assert Logs.closeNode();
						return root;
					}
				}
			}
			assert Logs.closeNode();
			region.addRegionToMap(otherRegion);
			otherRegion.addRegionToMap(region);
		}
		assert Logs.closeNode("Region passed all overlapping tests, clearing from temp-list and adding to BSP tree, then returning.");
		root.removeFromTempList(region);
		root.addRegion(region);
		assert Logs.closeNode();
		return root;
	}

	public static void snapVertices(List<DiscreteRegion> polygons, DiscreteRegion region) {
		assert Logs.openNode("Testing for snappable vertices.");
		assert Logs.addSnapNode("Region to be snapped", region);
		List<Point> regionPoints = region.getPoints();
		for (int i = 0; i < polygons.size(); i++) {
			List<Point> pointList = polygons.get(i).getPoints();
			for (int k = 0; k < pointList.size(); k++) {
				for (int l = 0; l < regionPoints.size(); l++) {
					if (pointList.get(k).equals(regionPoints.get(l))) {
						assert Logs.openNode("Equality found");
						assert Logs.addSnapNode("Correct point", pointList.get(k));
						assert Logs.addSnapNode("Point to be snapped", regionPoints.get(l));
						regionPoints.get(l).setPosition(pointList.get(k));
						assert Logs.closeNode();
					}
				}
			}
		}
		assert Logs.closeNode();
	}

	public static DiscreteRegion splitPolygonUsingEdge(DiscreteRegion otherRegion, Point pointA, Point pointB, boolean hyperPlane) {
		assert Logs.openNode("Polygon Plane-Splitting Operations", "Polygon Plane-Splitting");
		assert Logs.addNode("Splitting edge: " + pointA + ", " + pointB);
		assert Logs.addSnapNode("Region to split", otherRegion);
		assert Logs.addNode("Hyperplane: " + hyperPlane);
		List<Point> otherRegionPoints = otherRegion.getPoints();
		List<Point> extendedPointsList = Polygons.getExtensionPoints(pointA, pointB, otherRegion);
		List<RiffIntersectionPoint> intersectedList = Polygons.getIntersections(extendedPointsList.get(0), extendedPointsList.get(1), otherRegion);
		if (intersectedList.size() != 2) {
			assert Logs.closeNode("No, insufficient, or too many intersections found, returning null.");
			return null;
		}
		if (!hyperPlane && !Polygons.getBoundingRectIntersection(pointA, pointB, intersectedList.get(0).getIntersection(), intersectedList.get(1).getIntersection())) {
			assert Logs.closeNode("Bounding rect test failed between splitting edge and intersecting points, returning null.");
			return null;
		}
		assert Logs.addSnapNode("Valid intersections found, so creating new polygon.", intersectedList);
		DiscreteRegion newRegion = new DiscreteRegion(otherRegion.getEnvironment(), otherRegion.getProperties());
		newRegion.addPoint(intersectedList.get(0).getIntersection());
		for (int q = Math.min(intersectedList.get(0).getListOffset(), intersectedList.get(1).getListOffset()); q < Math.max(intersectedList.get(0).getListOffset(), intersectedList.get(1).getListOffset()); q++) {
			newRegion.addPoint(otherRegionPoints.get(q + 1));
		}
		newRegion.addPoint(intersectedList.get(1).getIntersection());
		assert Logs.addSnapNode("New region formed.", newRegion);
		if (intersectedList.get(0).getListOffset() < intersectedList.get(1).getListOffset()) {
			otherRegion.addPointAt(intersectedList.get(0).getListOffset() + 1, intersectedList.get(0).getIntersection());
			otherRegion.addPointAt(intersectedList.get(1).getListOffset() + 2, intersectedList.get(1).getIntersection());
		} else {
			otherRegion.addPointAt(intersectedList.get(1).getListOffset() + 1, intersectedList.get(0).getIntersection());
			otherRegion.addPointAt(intersectedList.get(0).getListOffset() + 2, intersectedList.get(1).getIntersection());
		}
		assert Logs.addSnapNode("Old region after intersect-point addition", otherRegion);
		for (int q = Math.min(intersectedList.get(0).getListOffset(), intersectedList.get(1).getListOffset()); q < Math.max(intersectedList.get(0).getListOffset(), intersectedList.get(1).getListOffset()); q++) {
			otherRegion.removePoint((2 + Math.min(intersectedList.get(0).getListOffset(), intersectedList.get(1).getListOffset())) % otherRegion.getPoints().size());
			assert Logs.addSnapNode("Point-Removals", "Current State", otherRegion);
		}
		assert Logs.addSnapNode("Old region", otherRegion);
		Polygons.optimizePolygon(otherRegion);
		Polygons.optimizePolygon(newRegion);
		assert Logs.closeNode();
		return newRegion;
	}

	public static boolean testForColinearity(Point pointA, DiscreteRegion region) {
		List<Point> pointList = region.getPoints();
		for (int i = 0; i < pointList.size(); i++) {
			if (pointA.equals(pointList.get(i)) || pointA.equals(pointList.get((i + 1) % pointList.size()))) {
				continue;
			}
			if (Points.areEqual(pointA, pointA.getY(), Polygons.getSlope(pointList.get(i), pointList.get((i + 1) % pointList.size())) * (pointA.getX() - (pointList.get(i)).getX()) + (pointList.get(i)).getY())) {
				return true;
			}
		}
		return false;
	}

	public static boolean testForColinearity(Point linePointA, Point linePointB, Point testPoint) {
		return Points.areEqual(testPoint, testPoint.getY(), (Polygons.getSlope(linePointA, linePointB) * (testPoint.getX() - linePointA.getX()) + linePointA.getY()));
	}

	public static boolean testForColinearity(Point pointA, Point pointB, Point testPointA, Point testPointB) {
		assert Logs.openNode("Colinearity Tests", "Testing for colinearity");
		assert Logs.openNode("Lines", "First Line");
		assert Logs.addSnapNode("Point A", pointA);
		assert Logs.addSnapNode("Point B", pointB);
		assert Logs.closeNode();
		assert Logs.openNode("Lines", "Test Line");
		assert Logs.addSnapNode("Test Point A", testPointA);
		assert Logs.addSnapNode("Test Point B", testPointB);
		assert Logs.closeNode();
		if ((pointA.equals(testPointA) && pointB.equals(testPointB)) || (pointA.equals(testPointB) && pointB.equals(testPointA))) {
			assert Logs.closeNode("Lines are equal, returning true.");
			return true;
		}
		if (!Polygons.areSlopesEqual(pointA, pointB, testPointA, testPointB)) {
			assert Logs.closeNode("Slopes are not equal, so returning false.");
			return false;
		}
		assert Logs.addNode("Slopes are equal, so beginning point-slope test.");
		double pointSlopeTest = Polygons.getSlope(pointA, pointB) * (testPointA.getX() - pointA.getX()) + pointA.getY();
		assert Logs.addNode("First point-slope test: " + pointSlopeTest);
		assert Logs.addNode("Expected value: " + testPointA.getY());
		if (!Points.areEqual(Point.System.EUCLIDEAN, pointSlopeTest, testPointA.getY())) {
			assert Logs.closeNode("First point failed point-slope test, so returning false.");
			return false;
		}
		pointSlopeTest = Polygons.getSlope(pointA, pointB) * (testPointB.getX() - pointA.getX()) + pointA.getY();
		assert Logs.addNode("Second point-slope test: " + pointSlopeTest);
		assert Logs.addNode("Expected value: " + testPointB.getY());
		if (!Points.areEqual(Point.System.EUCLIDEAN, Polygons.getSlope(pointA, pointB) * (testPointB.getX() - pointA.getX()) + pointA.getY(), testPointB.getY())) {
			assert Logs.closeNode("Second point failed point-slope test, so returning false.");
			return false;
		}
		assert Logs.closeNode("They are colinear.");
		return true;
	}

	public static boolean testforRegionPointSideIntersection(DiscreteRegion region, DiscreteRegion otherRegion) {
		assert Logs.openNode("Point-Side Intersection Tests", "Testing for point-side intersection between these two regions.");
		assert Logs.addSnapNode("Region A", region);
		assert Logs.addSnapNode("Region B", otherRegion);
		List<Point> regionList = region.getPoints();
		assert Logs.addNode("Beginning line-by-line point-side check.");
		for (int i = 0; i < regionList.size(); i++) {
			PointSideStruct struct = Polygons.getPointSideList(otherRegion, regionList.get(i), regionList.get((i + 1) % regionList.size()));
			if (struct.isGreaterThan()) {
				assert Logs.openNode("Qualifying line found");
				assert Logs.addSnapNode("Point A", regionList.get(i));
				assert Logs.addSnapNode("Point B", regionList.get((i + 1) % regionList.size()));
				assert Logs.closeNode();
				assert Logs.closeNode("This line guarantees a pass of the point-side test, returning false.");
				return false;
			}
		}
		assert Logs.closeNode("These polygons may intersect, returning true.");
		return true;
	}

	public static double testPointAgainstLine(Point testPoint, Point linePointA, Point linePointB) {
		return -1 * ((testPoint.getY() - linePointA.getY()) * (linePointB.getX() - linePointA.getX()) - (testPoint.getX() - linePointA.getX()) * (linePointB.getY() - linePointA.getY()));
	}

	public static Polygon convertToPolygon(DiscreteRegion region) {
		Polygon polygon = new Polygon();
		for (Point point : region.getPoints()) {
			polygon.addPoint((int) point.getX(), (int) point.getY());
		}
		return polygon;
	}

	public static DiscreteRegion convertToRegion(ScriptEnvironment env, Rectangle rect) {
		DiscreteRegion region = new DiscreteRegion();
		region.addPoint(new EuclideanPoint(rect.getX(), rect.getY(), 0.0d));
		region.addPoint(new EuclideanPoint(rect.getX(), rect.getY() + rect.getHeight(), 0.0d));
		region.addPoint(new EuclideanPoint(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0.0d));
		region.addPoint(new EuclideanPoint(rect.getX() + rect.getWidth(), rect.getY(), 0.0d));
		Polygons.optimizePolygon(region);
		return region;
	}

}
