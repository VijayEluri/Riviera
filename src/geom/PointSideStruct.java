/**
 * 
 */
package geom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import geom.points.Point;
import inspect.Nodeable;
import logging.Logs;

class PointSideStruct implements Nodeable {
	private final List<Point> left;
	private final List<Point> right;
	private final List<Point> indeterminates;
	double leftWeight, rightWeight;

	public PointSideStruct() {
		this(Collections.<Point> emptyList(), Collections.<Point> emptyList(), Collections.<Point> emptyList());
	}

	public PointSideStruct(final List<Point> left, final List<Point> right, final List<Point> indet) {
		if (left == null) {
			throw new NullPointerException("list must not be null");
		}
		this.left = new ArrayList<Point>(left);
		if (right == null) {
			throw new NullPointerException("right must not be null");
		}
		this.right = new ArrayList<Point>(right);
		if (indet == null) {
			throw new NullPointerException("indet must not be null");
		}
		this.indeterminates = new ArrayList<Point>(indet);
	}

	public void addIndeterminate(Point point) {
		this.indeterminates.add(point);
	}

	public void addLeft(Point point, double value) {
		this.left.add(point);
		this.leftWeight += value;
	}

	public void addRight(Point point, double value) {
		this.right.add(point);
		this.rightWeight += value;
	}

	public List<Point> getIndeterminates() {
		return Collections.unmodifiableList(this.indeterminates);
	}

	public List<Point> getLeftPoints() {
		return Collections.unmodifiableList(this.left);
	}

	public List<Point> getRightPoints() {
		return Collections.unmodifiableList(this.right);
	}

	public boolean hasIndeterminates() {
		return !this.indeterminates.isEmpty();
	}

	public boolean isColinear() {
		return this.left.isEmpty() && this.right.isEmpty();
	}

	public boolean isGreaterThan() {
		return this.left.isEmpty() && !this.right.isEmpty();
	}

	public boolean isLessThan() {
		return !this.left.isEmpty() && this.right.isEmpty();
	}

	public boolean isStraddling() {
		return !this.left.isEmpty() && !this.right.isEmpty();
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Point-Side Test Results");
		assert Logs.addSnapNode("Left-side Points (Weight: " + this.leftWeight + "): " + this.left.size() + " element(s)", this.left);
		assert Logs.addSnapNode("Right-side Points (Weight: " + this.rightWeight + "): " + this.right.size() + " element(s)", this.right);
		assert Logs.addSnapNode("Indeterminate Points: " + this.indeterminates.size() + " element(s)", this.indeterminates);
		assert Logs.closeNode();
	}

	public void validate() {
		if (Math.abs(this.rightWeight) < Math.pow(10, -5) && this.right.size() > 0) {
			assert Logs.addNode("Right-side list is insignificant, clearing.");
			this.right.clear();
		}
		if (Math.abs(this.leftWeight) < Math.pow(10, -5) && this.left.size() > 0) {
			assert Logs.addNode("Left-side list is insignificant, clearing.");
			this.left.clear();
		}
	}
}