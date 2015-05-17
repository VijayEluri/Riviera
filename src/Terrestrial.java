package com.dafrito.rfe;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import geom.DiscreteRegion;
import geom.DiscreteRegionBSPNode;
import geom.PolygonPipeline;
import geom.Polygons;
import geom.SplitterThread;
import geom.points.Point;
import geom.points.PointPath;
import geom.points.Points;
import logging.Logs;
import script.Conversions;
import script.ScriptEnvironment;
import script.exceptions.ScriptException;
import script.operations.ScriptExecutable_CallFunction;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;

public class Terrestrial implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5769369184511259491L;
	private double radius;
	private DiscreteRegionBSPNode tree;
	private volatile int openThreads = 0;

	public Terrestrial(double radius) {
		this.radius = radius;
	}

	public void add(DiscreteRegion region) {
		this.openThreads++;
		PolygonPipeline pipeline = new PolygonPipeline(this, region);
		pipeline.start();
	}

	public synchronized void addValidatedRegions(List<DiscreteRegion> regions) {
		if (regions == null || regions.size() == 0) {
			return;
		}
		assert Logs.openNode("Validated Region Additions", "Adding Validated Regions (" + regions.size() + " region(s))");
		if (this.getTree() == null) {
			this.setTree(new DiscreteRegionBSPNode(regions.get(0)));
			if (regions.size() == 1) {
				assert Logs.closeNode();
				this.decrementOpenThreads();
				return;
			}
		}
		SplitterThread thread = new SplitterThread(this, this.getTree(), regions, true);
		thread.start();
		assert Logs.closeNode();
	}

	public void decrementOpenThreads() {
		this.openThreads--;
	}

	public PointPath getPath(ScriptEnvironment env, Scenario scenario, ScriptTemplate_Abstract evaluator, Asset asset, Point currentPoint, Point destinationPoint) throws ScriptException {
		while (this.openThreads != 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
		assert Logs.openNode("Pathfinding", "Getting path (" + currentPoint + " to " + destinationPoint + ")");
		PointPath path = new PointPath(scenario);
		DiscreteRegion startingRegion;
		assert this.getTree() != null : "BSP Tree is null!";
		DiscreteRegion currentRegion = startingRegion = this.getTree().getRegion(currentPoint);
		DiscreteRegion destination = this.getTree().getRegion(destinationPoint);
		List<ScriptValue> params = new LinkedList<ScriptValue>();
		params.add(Conversions.wrapDiscreteRegion(env, currentRegion));
		params.add(Conversions.wrapAsset(env, asset));
		path.addPoint(currentPoint, Conversions.getDouble(env, ScriptExecutable_CallFunction.callFunction(env, null, evaluator, "evaluateMovementCost", params)));
		int ticker = 0;
		Stack<DiscreteRegion> regionPath = new Stack<DiscreteRegion>();
		regionPath.push(currentRegion);
		Set<DiscreteRegion> used = new HashSet<DiscreteRegion>();
		List<DiscreteRegion> availableNeighbors = new LinkedList<DiscreteRegion>();
		List<Point> nearestNeighborPoints = new LinkedList<Point>();
		List<Double> movementCosts = new LinkedList<Double>();
		while (!currentRegion.equals(destination)) {
			if (ticker > 100) {
				throw new IllegalStateException("Pathfinder iteration tolerance exceeded.");
			} else {
				ticker++;
			}
			assert Logs.openNode("Pathfinder iteration " + ticker);
			assert Logs.addSnapNode("Current region", currentRegion);
			if (currentRegion.getNeighbors().size() == 0) {
				assert Logs.closeNode("Current region has no neighbors, returning null list");
				path = null;
				break;
			}
			availableNeighbors.clear();
			nearestNeighborPoints.clear();
			assert Logs.openNode("Current region's neighbors (" + currentRegion.getNeighbors().size() + " neighbor(s))");
			for (DiscreteRegion neighbor : currentRegion.getNeighbors()) {
				assert Logs.openNode("Discrete Region (" + neighbor.getPoints().size() + " point(s))");
				assert Logs.addSnapNode("Properties", neighbor.getProperties());
				assert Logs.addSnapNode("Points", neighbor.getPoints());
				assert Logs.closeNode();
			}
			assert Logs.closeNode();
			assert Logs.openNode("Getting valid neighbors");
			for (DiscreteRegion neighbor : currentRegion.getNeighbors()) {
				if (used.contains(neighbor)) {
					continue;
				}
				assert Logs.addSnapNode("Placing neighbor in neighbors list", neighbor);
				assert Logs.openNode("Retrieving nearest colinear point");
				Point[] line = Polygons.getAdjacentEdge(currentRegion, neighbor);
				Point point = Polygons.getMinimumPointBetweenLine(line[0], line[1], currentPoint);
				assert Logs.addNode("Adding neighbor and point (" + point + ")", neighbor);
				availableNeighbors.add(neighbor);
				nearestNeighborPoints.add(point);
				assert Logs.closeNode();
			}
			assert Logs.closeNode("Available neighbors (" + availableNeighbors.size() + " neighbor(s))", availableNeighbors);
			if (availableNeighbors.isEmpty()) {
				assert Logs.addNode("Stepping back");
				if (currentRegion.equals(startingRegion)) {
					assert Logs.closeNode("No route available.");
					path = null;
					break;
				}
				path.removeLastPoint();
				used.add(currentRegion);
				assert regionPath.size() != 0;
				currentRegion = regionPath.pop();
				assert Logs.closeNode("No neighbors available, stepping back");
				continue;
			}
			movementCosts.clear();
			assert Logs.openNode("Getting movement costs (" + availableNeighbors.size() + " neighbor(s))");
			for (DiscreteRegion region : availableNeighbors) {
				params.clear();
				params.add(Conversions.wrapDiscreteRegion(env, region));
				params.add(Conversions.wrapAsset(env, asset));
				movementCosts.add(Conversions.getDouble(env, ScriptExecutable_CallFunction.callFunction(env, null, evaluator, "evaluateMovementCost", params)));
			}
			assert Logs.closeNode("Movement costs", movementCosts);
			double minimumValue = Double.POSITIVE_INFINITY;
			int optimum = -1;
			assert Logs.openNode("Getting optimum region (" + availableNeighbors.size() + " option(s))");
			for (int i = 0; i < availableNeighbors.size(); i++) {
				double value = Points.getDistance(currentPoint, nearestNeighborPoints.get(i)) * path.getLastMovementCost();
				assert Logs.addNode("Movement cost from current location to border of current region (" + value + ")");
				value += Points.getDistance(nearestNeighborPoints.get(i), destinationPoint) * movementCosts.get(i);
				assert Logs.addSnapNode("Current neighbor (Total movement cost: " + value + ")", availableNeighbors.get(i));
				if (Points.isLessThan(value, minimumValue)) {
					assert Logs.addNode("Value is less than current minimum, setting as new value (" + minimumValue + " to " + value + ")");
					minimumValue = value;
					optimum = i;
				}
			}
			assert Logs.closeNode();
			currentPoint = nearestNeighborPoints.get(optimum);
			path.addPoint(currentPoint, movementCosts.get(optimum));
			used.add(currentRegion);
			currentRegion = availableNeighbors.get(optimum);
			assert Logs.addSnapNode("New optimum region (At optimum point: " + currentPoint + ")", currentRegion);
			regionPath.push(currentRegion);
			assert Logs.addSnapNode("Region Path", regionPath);
			assert Logs.closeNode("Current path", path);
		}
		if (path != null) {
			params.clear();
			params.add(Conversions.wrapDiscreteRegion(env, destination));
			params.add(Conversions.wrapAsset(env, asset));
			path.addPoint(destinationPoint, Conversions.getDouble(env, ScriptExecutable_CallFunction.callFunction(env, null, evaluator, "evaluateMovementCost", params)));
			assert Logs.closeNode("Path", path);
		} else {
			throw new NoSuchElementException("No route available");
		}
		return path;
	}

	public double getRadius() {
		return this.radius;
	}

	public DiscreteRegionBSPNode getTree() {
		return this.tree;
	}

	public void setTree(DiscreteRegionBSPNode tree) {
		this.tree = tree;
	}
}
