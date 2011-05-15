package com.bluespot.geom.vectors;

/**
 * A vector represents magnitudes in three dimensions. This class is in double
 * precision.
 * 
 * @author Aaron Faanes
 * 
 */
public class Vector3d {

	/**
	 * Represents a vector of unit length facing up the Y-axis. Specifically, it
	 * is the vector {@code (0, 1, 0)}.
	 */
	public static final Vector3d UP = new Vector3d(0, 1, 0);

	/**
	 * Represents a vector of unit length facing down the Z-axis. Specifically,
	 * it is the vector {@code (0, 0, -1)}.
	 */
	public static final Vector3d FORWARD = new Vector3d(0, 0, -1);

	private final double x;
	private final double y;
	private final double z;

	/**
	 * Constructs a new {@link Vector3d} from the specified values. The values
	 * may be any number except {@code NaN}.
	 * 
	 * @param x
	 *            the magnitude of the X-axis
	 * @param y
	 *            the magnitude of the Y-axis
	 * @param z
	 *            the magnitude of the Z-axis
	 * @throws IllegalArgumentException
	 *             if any argument is {@code NaN}.
	 */
	public Vector3d(final double x, final double y, final double z) {
		if (Double.isNaN(x)) {
			throw new IllegalArgumentException("x is NaN");
		}
		if (Double.isNaN(y)) {
			throw new IllegalArgumentException("y is NaN");
		}
		if (Double.isNaN(z)) {
			throw new IllegalArgumentException("z is NaN");
		}
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Returns the magnitude of the X-axis for this vector.
	 * 
	 * @return the magnitude of the X-axis for this vector
	 */
	public double getX() {
		return this.x;
	}

	/**
	 * Returns the magnitude of the Y-axis for this vector.
	 * 
	 * @return the magnitude of the Y-axis for this vector
	 */
	public double getY() {
		return this.y;
	}

	/**
	 * Returns the magnitude of the Z-axis for this vector.
	 * 
	 * @return the magnitude of the Z-axis for this vector
	 */
	public double getZ() {
		return this.z;
	}

	public Vector3d inverted() {
		return new Vector3d(-this.getX(), -this.getY(), -this.getZ());
	}

	public Vector3d crossProduct(final Vector3d other) {
		if (other == null) {
			throw new NullPointerException("other is null");
		}

		final double crossX = this.getY() * other.getZ() - other.getY() * this.getZ();
		final double crossY = -this.getX() * other.getZ() + other.getX() * this.getZ();
		final double crossZ = this.getX() * other.getY() - other.getX() * this.getY();

		return new Vector3d(crossX, crossY, crossZ);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Vector3d)) {
			return false;
		}
		final Vector3d other = (Vector3d) obj;
		if (this.getX() != other.getX()) {
			return false;
		}
		if (this.getY() != other.getY()) {
			return false;
		}
		if (this.getZ() != other.getZ()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 17;
		final long xLong = Double.doubleToLongBits(this.getX());
		final long yLong = Double.doubleToLongBits(this.getY());
		final long zLong = Double.doubleToLongBits(this.getZ());
		result = 31 * result + (int) (xLong ^ (xLong >>> 32));
		result = 31 * result + (int) (yLong ^ (yLong >>> 32));
		result = 31 * result + (int) (zLong ^ (zLong >>> 32));
		return result;
	}
}
