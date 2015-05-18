package geom.points;

import java.util.LinkedList;
import java.util.List;

import asset.Scenario;
import inspect.Nodeable;
import logging.Logs;

public class PointPath extends Point implements Nodeable {
	private List<Point> points = new LinkedList<Point>();
	private List<Double> movementCosts = new LinkedList<Double>();
	private long startTime;
	private Scenario scenario;

	public PointPath(Scenario scenario) {
		this(null, scenario);
	}

	public PointPath(String name, Scenario scenario) {
		super(null);
		this.scenario = scenario;
		this.setStartTime(this.scenario.getGameTime());
	}

	public void addPoint(Point point, Double velocity) {
		this.points.add(Point.createPoint(point, point.getX(), point.getY(), point.getZ()));
		this.movementCosts.add(velocity);
	}

	public Point getCurrentPoint() {
		assert Logs.openNode("Path Point Retrievals", "Getting path point");
		assert Logs.addNode(this);
		while (this.points.size() > 1) {
			double distance = (this.getScenario().getGameTime() - this.startTime) * this.getVelocity(this.movementCosts.get(0)); // distance traveled
			double total = Points.getDistance(this.points.get(0), this.points.get(1));
			double offset = distance / total;
			if (distance >= total) {
				this.startTime += (long) (total / this.getVelocity(this.movementCosts.get(0)));
				this.movementCosts.remove(0);
				this.points.remove(0);
				continue;
			}
			Point point = Point.createPoint(
					this.points.get(0),
					this.points.get(0).getX() + (this.points.get(1).getX() - this.points.get(0).getX()) * offset,
					this.points.get(0).getY() + (this.points.get(1).getY() - this.points.get(0).getY()) * offset,
					this.points.get(0).getZ() + (this.points.get(1).getZ() - this.points.get(0).getZ()) * offset
					);
			assert Logs.closeNode();
			return point;
		}
		assert Logs.closeNode();
		return this.points.get(0);
	}

	public double getLastMovementCost() {
		return this.movementCosts.get(this.movementCosts.size() - 1).doubleValue();
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public long getStartTime() {
		return this.startTime;
	}

	@Override
	public Point.System getSystem() {
		return Point.System.EUCLIDEAN;
	}

	public long getTotalTime() {
		long time = 0;
		for (int i = 0; i < this.points.size() - 1; i++) {
			double total = Points.getDistance(this.points.get(i), this.points.get(i + 1));
			time += (long) (total / this.getVelocity(this.movementCosts.get(i)));
		}
		return time;
	}

	public double getVelocity(double movementCost) {
		return movementCost / 10.0d;
	}

	// Point implementation
	@Override
	public double getX() {
		return this.getCurrentPoint().getX();
	}

	@Override
	public double getY() {
		return this.getCurrentPoint().getY();
	}

	@Override
	public double getZ() {
		return this.getCurrentPoint().getZ();
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Path");
		assert Logs.addSnapNode("Points", this.points);
		assert Logs.addSnapNode("Movement Costs", this.movementCosts);
		assert Logs.addNode("Start time: " + this.startTime);
		assert Logs.closeNode();
	}

	public void removeLastPoint() {
		if (this.points.size() > 0) {
			this.points.remove(this.points.size() - 1);
		}
		if (this.movementCosts.size() > 0) {
			this.movementCosts.remove(0);
		}
	}

	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	public void setStartTime(long time) {
		this.startTime = time;
	}

	@Override
	public void setX(double x) {
		throw new UnsupportedOperationException("Unsupported operation");
	}

	@Override
	public void setY(double y) {
		throw new UnsupportedOperationException("Unsupported operation");
	}

	@Override
	public void setZ(double z) {
		throw new UnsupportedOperationException("Unsupported operation");
	}
}
