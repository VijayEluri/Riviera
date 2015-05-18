package geom;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import geom.points.Point;
import geom.points.Points;
import inspect.Nodeable;
import logging.Logs;
import asset.Archetype;
import asset.ArchetypeMapNode;
import asset.Asset;

public class DiscreteRegionBSPNode implements Nodeable {
	private final Point pointA;
	private final Point pointB;
	private DiscreteRegionBSPNode leftNode, rightNode;
	private final Set<DiscreteRegion> leftNeighbors = new HashSet<DiscreteRegion>();
	private final Set<DiscreteRegion> rightNeighbors = new HashSet<DiscreteRegion>();
	private final Set<DiscreteRegion> tempList = new HashSet<DiscreteRegion>();
	private final DiscreteRegionBSPNode root;

	public DiscreteRegionBSPNode(DiscreteRegion region) {
		this.root = this;
		this.pointA = region.getPoints().get(0);
		this.pointB = region.getPoints().get(1);
		this.addRegion(region);
	}

	public DiscreteRegionBSPNode(DiscreteRegionBSPNode root, Point pointA, Point pointB) {
		this.root = root;
		this.pointA = pointA;
		this.pointB = pointB;
	}

	public synchronized void addLine(DiscreteRegion owner, Point pointA, Point pointB) {
		assert Logs.openNode("BSP Line Additions", "Adding line to BSP tree (" + pointA + ", " + pointB + ")");
		assert Logs.addSnapNode("Current node (" + this.pointA + ", " + this.pointB + ")", this);
		PointSideStruct struct = Polygons.getPointSideList(this.pointA, this.pointB, pointA, pointB);
		assert Logs.addNode(struct);
		if (Polygons.testForColinearity(pointA, pointB, this.pointA, this.pointB) || struct.isColinear()) {
			assert Logs.closeNode();
			return;
		}
		if (struct.isLessThan()) {
			assert Logs.addNode("Line is less than this node's line.");
			if (this.leftNode != null) {
				assert Logs.openNode("Deferring to left node");
				this.leftNode.addLine(owner, pointA, pointB);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return;
			}
			this.leftNode = new DiscreteRegionBSPNode(this.root, pointA, pointB);
			assert Logs.addNode("Creating new left node.", this.leftNode);
			this.leftNode.categorizeRegion(owner);
		} else if (struct.isGreaterThan()) {
			assert Logs.addNode("Line is greater than this node's line.");
			if (this.rightNode != null) {
				assert Logs.openNode("Deferring to right node");
				this.rightNode.addLine(owner, pointA, pointB);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return;
			}
			this.rightNode = new DiscreteRegionBSPNode(this.root, pointA, pointB);
			assert Logs.addNode("Creating new right node.", this.rightNode);
			this.rightNode.categorizeRegion(owner);
		}
		assert Logs.closeNode();
	}

	public synchronized void addRegion(DiscreteRegion region) {
		assert Logs.openNode("BSP Region Additions", "Adding region to BSP tree");
		assert Logs.addNode(region);
		assert Logs.addSnapNode("Current node (" + this.pointA + ", " + this.pointB + ")", this);
		Polygons.optimizePolygon(region);
		PointSideStruct struct = Polygons.getPointSideList(region, this.pointA, this.pointB);
		if (struct.isStraddling()) {
			assert Logs.addNode("Region is straddling this node's line, splitting.");
			this.root.removeRegion(region);
			DiscreteRegion splitPolygon = Polygons.splitPolygonUsingEdge(region, this.pointA, this.pointB, true);
			if (splitPolygon == null) {
				assert Logs.addNode("Unexpected null region from split.");
				assert Logs.addNode(struct);
			}
			this.root.addRegion(region);
			this.root.addRegion(splitPolygon);
			assert Logs.closeNode();
			return;
		}
		if (struct.isLessThan()) {
			assert Logs.addNode("Region is less than this node's line.");
			if (struct.hasIndeterminates()) {
				assert Logs.addNode("Region has points that are colinear with this node's line, so adding it to left neighbors.");
				this.leftNeighbors.add(region);
				region.addRegionNeighbors(this.rightNeighbors);
			}
			if (this.leftNode != null) {
				assert Logs.openNode("Deferring to left node");
				this.leftNode.addRegion(region);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return;
			}
			List<Point> pointList = region.getPoints();
			for (int i = 0; i < pointList.size(); i++) {
				this.addLine(region, pointList.get(i), pointList.get((i + 1) % pointList.size()));
			}
		} else if (struct.isGreaterThan()) {
			assert Logs.addNode("Region is greater than this node's line.");
			if (struct.hasIndeterminates()) {
				assert Logs.addNode("Region has points that are colinear with this node's line, so adding it to right neighbors.");
				this.rightNeighbors.add(region);
				region.addRegionNeighbors(this.leftNeighbors);
			}
			if (this.rightNode != null) {
				assert Logs.openNode("Deferring to right node");
				this.rightNode.addRegion(region);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return;
			}
			List<Point> pointList = region.getPoints();
			for (int i = 0; i < pointList.size(); i++) {
				this.addLine(region, pointList.get(i), pointList.get((i + 1) % pointList.size()));
			}
		}
		assert Logs.closeNode();
	}

	public synchronized void addRegions(Collection<DiscreteRegion> regions) {
		for (DiscreteRegion region : regions) {
			this.addRegion(region);
		}
	}

	public synchronized void addToTempList(Collection<DiscreteRegion> regions) {
		assert Logs.addSnapNode("Temporary Region List Additions", "Adding regions to temporary region list", regions);
		this.tempList.addAll(regions);
	}

	public synchronized void addToTempList(DiscreteRegion region) {
		assert Logs.addSnapNode("Temporary Region List Additions", "Adding region to temporary region list", region);
		this.tempList.add(region);
	}

	public synchronized void categorizeRegion(DiscreteRegion region) {
		assert Logs.openNode("Region Categorizations", "Categorizing Region");
		assert Logs.addNode(region);
		assert Logs.addNode(this);
		Polygons.optimizePolygon(region);
		PointSideStruct struct = Polygons.getPointSideList(region, this.pointA, this.pointB);
		assert Logs.addNode(struct);
		if (struct.isLessThan() && struct.hasIndeterminates()) {
			assert Logs.addNode("Region has points which are less than or equal to this node's line, adding to left neighbors.");
			this.leftNeighbors.add(region);
			region.addRegionNeighbors(this.rightNeighbors);
		}
		if (struct.isGreaterThan() && struct.hasIndeterminates()) {
			assert Logs.addNode("Region has points which are greater than or equal to this node's line, adding to right neighbors.");
			this.rightNeighbors.add(region);
			region.addRegionNeighbors(this.leftNeighbors);
		}
		assert Logs.closeNode();
	}

	public synchronized void clearTempList() {
		assert Logs.addNode("Clearing temporary region list");
		this.tempList.clear();
	}

	public synchronized List<Asset> getAllAssets() {
		List<Asset> assets = new LinkedList<Asset>();
		for (DiscreteRegion region : this.leftNeighbors) {
			assets.addAll(((ArchetypeMapNode) region.getProperty("Archetypes")).getAllAssets());
		}
		for (DiscreteRegion region : this.rightNeighbors) {
			assets.addAll(((ArchetypeMapNode) region.getProperty("Archetypes")).getAllAssets());
		}
		if (this.leftNode != null) {
			assets.addAll(this.leftNode.getAllAssets());
		}
		if (this.rightNode != null) {
			assets.addAll(this.rightNode.getAllAssets());
		}
		return assets;
	}

	public synchronized Set<DiscreteRegion> getPotentialList(DiscreteRegion region) {
		assert Logs.openNode("BSP Potential List Creations", "Retrieving Potentially-Intersecting List");
		assert Logs.addSnapNode("Testing Region", region);
		assert Logs.addSnapNode("Current node (" + this.pointA + ", " + this.pointB + ")", this);
		Polygons.optimizePolygon(region);
		PointSideStruct struct = Polygons.getPointSideList(region, this.pointA, this.pointB);
		if (struct.isStraddling()) {
			assert Logs.addNode("Region is straddling this line, returning full list.");
			Set<DiscreteRegion> polys = new HashSet<DiscreteRegion>();
			polys.addAll(this.getRegionList());
			assert Logs.closeNode();
			return polys;
		} else if (struct.isLessThan()) {
			assert Logs.addNode("Region is less than this line.");
			if (this.leftNode != null) {
				assert Logs.openNode("Deferring to left node");
				Set<DiscreteRegion> returnList = this.leftNode.getPotentialList(region);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return returnList;
			}
			assert Logs.closeNode("Left node is null, so returning left neighbors.", this.leftNeighbors);
			return this.leftNeighbors;
		} else if (struct.isGreaterThan()) {
			assert Logs.addNode("Region is greater than this line.");
			if (this.rightNode != null) {
				assert Logs.openNode("Deferring to right node");
				Set<DiscreteRegion> returnList = this.rightNode.getPotentialList(region);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return returnList;
			}
			assert Logs.closeNode("Right node is null, so returning right neighbors.", this.rightNeighbors);
			return this.rightNeighbors;
		}
		throw new AssertionError("Defaulted in getPotentialList in DiscreteRegionBSPNode");
	}

	public DiscreteRegion getRegion(Point point) {
		Set<DiscreteRegion> set = this.getRegions(point);
		if (set.size() > 1) {
			throw new IllegalArgumentException("More than one polygon found for supposedly single-polygon query (" + point + ")");
		} else if (set.isEmpty()) {
			throw new IllegalStateException("No polygon found at location (" + point + ")");
		}
		return set.iterator().next();
	}

	public Set<DiscreteRegion> getRegionList() {
		Set<DiscreteRegion> list = new HashSet<DiscreteRegion>();
		list.addAll(this.leftNeighbors);
		list.addAll(this.rightNeighbors);
		if (this.leftNode != null) {
			list.addAll(this.leftNode.getRegionList());
		}
		if (this.rightNode != null) {
			list.addAll(this.rightNode.getRegionList());
		}
		return list;
	}

	public Set<DiscreteRegion> getRegions(Point point) {
		assert Logs.openNode("BSP Polygon Retrievals", "Finding polygon by point: " + point);
		double value = Polygons.testPointAgainstLine(point, this.pointA, this.pointB);
		assert Logs.addNode("Point-side test result: " + value);
		Set<DiscreteRegion> polyList = new HashSet<DiscreteRegion>();
		if (Points.isGreaterThan(value, 0.0d)) {
			assert Logs.addNode("Value is greater than zero.");
			if (this.rightNode != null) {
				assert Logs.openNode("Deferring to right node");
				Set<DiscreteRegion> set = this.rightNode.getRegions(point);
				assert Logs.closeNode();
				assert Logs.closeNode("Returning region set (" + set.size() + " region(s))", set);
				return set;
			} else {
				assert Logs.addSnapNode("Adding all right neighbors.", this.rightNeighbors);
				polyList.addAll(this.rightNeighbors);
			}
		}
		if (Points.isLessThan(value, 0.0d)) {
			assert Logs.addNode("Value is less than zero.");
			if (this.leftNode != null) {
				assert Logs.openNode("Deferring to left node");
				Set<DiscreteRegion> set = this.leftNode.getRegions(point);
				assert Logs.closeNode();
				assert Logs.closeNode("Returning region set (" + set.size() + " region(s))", set);
				return set;
			} else {
				assert Logs.addSnapNode("Adding all left neighbors.", this.leftNeighbors);
				polyList.addAll(this.leftNeighbors);
			}
		}
		if (Points.areEqual(Point.System.EUCLIDEAN, value, 0.0d)) {
			assert Logs.addNode("Value is equal to zero, adding both lists.");
			polyList.addAll(this.leftNeighbors);
			polyList.addAll(this.rightNeighbors);
		}
		assert Logs.closeNode("Returning region set (" + polyList.size() + " region(s))", polyList);
		return polyList;
	}

	public synchronized Set<DiscreteRegion> getTempList() {
		return this.tempList;
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("BSP Tree Node (" + this.pointA + ", " + this.pointB + ")");
		assert Logs.addSnapNode("Left Neighbors", this.leftNeighbors);
		assert Logs.addSnapNode("Right Neighbors", this.rightNeighbors);
		if (this.leftNode == null) {
			assert Logs.addNode("Left node: null");
		} else {
			assert Logs.addSnapNode("Left node", this.leftNode);
		}
		if (this.rightNode == null) {
			assert Logs.addNode("Right node: null");
		} else {
			assert Logs.addSnapNode("Right node", this.rightNode);
		}
		assert Logs.closeNode();
	}

	public synchronized void removeFromTempList(DiscreteRegion region) {
		assert Logs.addSnapNode("Temporary Region List Removals", "Removing region from temporary region list", region);
		this.tempList.remove(region);
	}

	public synchronized void removeRegion(DiscreteRegion region) {
		assert Logs.openNode("BSP Region Removals", "Removing region from BSP tree");
		assert Logs.addNode(region);
		assert Logs.addSnapNode("Current node (" + this.pointA + ", " + this.pointB + ")", this);
		PointSideStruct struct = Polygons.getPointSideList(region, this.pointA, this.pointB);
		assert Logs.addNode(struct);
		if (struct.isLessThan()) {
			assert Logs.addNode("Region is less than this node's line.");
			if (struct.hasIndeterminates()) {
				assert Logs.addNode("Removing region from left neighbors.");
				this.leftNeighbors.remove(region);
				Iterator<DiscreteRegion> polys = this.rightNeighbors.iterator();
				while (polys.hasNext()) {
					(polys.next()).removeRegionNeighbor(region);
				}
				assert Logs.addSnapNode("Left neighbors", this.leftNeighbors);
			}
			if (this.leftNode != null) {
				assert Logs.openNode("Deferring to left node");
				this.leftNode.removeRegion(region);
				assert Logs.closeNode();
			}
		}
		if (struct.isGreaterThan()) {
			assert Logs.addNode("Region is greater than this node's line.");
			if (struct.hasIndeterminates()) {
				assert Logs.addNode("Removing region from right neighbors.");
				this.rightNeighbors.remove(region);
				Iterator<DiscreteRegion> polys = this.leftNeighbors.iterator();
				while (polys.hasNext()) {
					(polys.next()).removeRegionNeighbor(region);
				}
				assert Logs.addSnapNode("Left neighbors", this.rightNeighbors);
			}
			if (this.rightNode != null) {
				assert Logs.openNode("Deferring to right node");
				this.rightNode.removeRegion(region);
				assert Logs.closeNode();
			}
		}
		assert Logs.closeNode();
	}
}
