/**
 * 
 */
package com.bluespot.geom.vectors;

import com.bluespot.geom.Axis;

/**
 * A {@link Vector3} in {@code double} precision. Be aware that while this class
 * implements {@link #equals(Object)} appropriately, it may yield unexpected
 * results due to the inherent imprecision of floating-point values.
 * 
 * @author Aaron Faanes
 * 
 * @see Vector3f
 * @see Vector3i
 */
public class Vector3d extends AbstractVector3<Vector3d> {

	/**
	 * Create a mutable {@link Vector3d} using the specified value for all axes.
	 * 
	 * @param v
	 *            the value used for all axes
	 * @return a mutable {@code Vector3d}
	 * @throw {@link IllegalArgumentException} if {@code v} is {@code NaN}
	 */
	public static Vector3d mutable(double v) {
		return Vector3d.mutable(v, v, v);
	}

	/**
	 * Create a frozen {@link Vector3d} using the specified value for all axes.
	 * 
	 * @param v
	 *            the value used for all axes
	 * @return a frozen {@code Vector3d}
	 * @throw {@link IllegalArgumentException} if {@code v} is {@code NaN}
	 */
	public static Vector3d frozen(double v) {
		return Vector3d.mutable(v, v, v);
	}

	/**
	 * Create a mutable {@link Vector3d} using the specified values.
	 * 
	 * @param x
	 *            the x component
	 * @param y
	 *            the y component
	 * @param z
	 *            the z component
	 * @return a new mutable {@code Vector3d}
	 * @throw {@link IllegalArgumentException} if any component is {@code NaN}
	 */
	public static Vector3d mutable(double x, final double y, final double z) {
		return new Vector3d(true, x, y, z);
	}

	public static Vector3d mutable(double x, double y) {
		return mutable(x, y, 0);
	}

	/**
	 * Create a frozen {@link Vector3d} using the specified values.
	 * 
	 * @param x
	 *            the x component
	 * @param y
	 *            the y component
	 * @param z
	 *            the z component
	 * @return a frozen {@code Vector3d}
	 * @throw {@link IllegalArgumentException} if any component is {@code NaN}
	 */
	public static Vector3d frozen(double x, final double y, final double z) {
		return new Vector3d(false, x, y, z);
	}

	public static Vector3d frozen(double x, double y) {
		return frozen(x, y, 0);
	}

	/**
	 * Create a mutable {@link Vector3d} from the specified vector.
	 * 
	 * @param vector
	 *            the vector that is copied
	 * @return a mutable {@code Vector3d}
	 * @throw {@link NullPointerException} if {@code vector} is null
	 */
	public static Vector3d mutable(Vector3i vector) {
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		return new Vector3d(true, vector.x(), vector.y(), vector.z());
	}

	/**
	 * Create a frozen {@link Vector3d} from the specified vector.
	 * 
	 * @param vector
	 *            the vector that is copied
	 * @return a frozen {@code Vector3d}
	 * @throw {@link NullPointerException} if {@code vector} is null
	 */
	public static Vector3d frozen(Vector3i vector) {
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		return new Vector3d(false, vector.x(), vector.y(), vector.z());
	}

	/**
	 * Create a mutable {@link Vector3d} from the specified vector.
	 * 
	 * @param vector
	 *            the vector that is copied
	 * @return a mutable {@code Vector3d}
	 * @throw {@link NullPointerException} if {@code vector} is null
	 */
	public static Vector3d mutable(Vector3f vector) {
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		return new Vector3d(true, vector.x(), vector.y(), vector.z());
	}

	/**
	 * Create a frozen {@link Vector3d} from the specified vector.
	 * 
	 * @param vector
	 *            the vector that is copied
	 * @return a frozen {@code Vector3d}
	 * @throw {@link NullPointerException} if {@code vector} is null
	 */
	public static Vector3d frozen(Vector3f vector) {
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		return new Vector3d(false, vector.x(), vector.y(), vector.z());
	}

	/**
	 * Interpolates between this vector and the destination. Offsets that are
	 * not between zero and one are handled specially:
	 * <ul>
	 * <li>If {@code offset <= 0}, a copy of {@code src} is returned
	 * <li>If {@code offset >= 1}, a copy of {@code dest} is returned
	 * </ul>
	 * This special behavior allows clients to reliably detect when
	 * interpolation is complete.
	 * 
	 * @param src
	 *            the starting vector
	 * @param dest
	 *            the ending vector
	 * @param offset
	 *            the percentage of distance between the specified points
	 * @return a mutable {@link Vector3d} that lies between src and dest
	 * @throw {@link NullPointerException} if either vector is null
	 * @throw {@link IllegalArgumentException} if {@code offset} is NaN
	 */
	public static Vector3d interpolated(Vector3d src, Vector3d dest, float offset) {
		if (src == null) {
			throw new NullPointerException("src must not be null");
		}
		if (dest == null) {
			throw new NullPointerException("dest must not be null");
		}
		if (Float.isNaN(offset)) {
			throw new IllegalArgumentException("offset must not be NaN");
		}
		if (offset <= 0f) {
			return src.toMutable();
		}
		if (offset >= 1f) {
			return dest.toMutable();
		}
		return mutable(src.x + (dest.x - src.x) * offset,
				src.y + (dest.y - src.y) * offset,
				src.z + (dest.z - src.z) * offset);
	}

	private static final Vector3d ORIGIN = Vector3d.frozen(0);

	/**
	 * Returns a frozen vector at the origin.
	 * 
	 * @return a frozen vector with components {@code (0, 1, 0)}
	 */
	public static Vector3d origin() {
		return ORIGIN;
	}

	/**
	 * Return a frozen vector with values of 1 at the specified axes. This is
	 * normally used to create unit vectors, but {@code axis} values of multiple
	 * axes are allowed.
	 * 
	 * @param axis
	 *            the axes with values of 1
	 * @return a frozen unit vector
	 */
	public static Vector3d unit(Axis axis) {
		return origin().with(axis, 1).toFrozen();
	}

	private static final Vector3d UP = Vector3d.frozen(0, 1, 0);

	/**
	 * Returns a frozen vector that points up the y axis.
	 * 
	 * @return a frozen vector with components {@code (0, 1, 0)}
	 */
	public static Vector3d up() {
		return UP;
	}

	private static final Vector3d FORWARD = Vector3d.frozen(0, 0, -1);

	/**
	 * Returns a frozen vector that points down the z axis.
	 * 
	 * @return a frozen vector with components {@code (0, 0, -1)}
	 */
	public static Vector3d forward() {
		return FORWARD;
	}

	private static final Vector3d LEFT = Vector3d.frozen(-1, 0, 0);

	/**
	 * Returns a frozen vector that points down the negative x axis.
	 * 
	 * @return a frozen vector with components {@code (-1, 0, 0)}
	 */
	public static final Vector3d left() {
		return LEFT;
	}

	private static final Vector3d RIGHT = Vector3d.frozen(1, 0, 0);

	/**
	 * Returns a frozen vector that points down the positive x axis.
	 * 
	 * @return a frozen vector with components {@code (1, 0, 0)}
	 */
	public static Vector3d right() {
		return RIGHT;
	}

	private static final Vector3d DOWN = UP.inverted().toFrozen();

	/**
	 * Returns a frozen vector that points down the negative Y axis.
	 * 
	 * @return a frozen vector with components {@code (0, -1, 0)}
	 */
	public static final Vector3d down() {
		return DOWN;
	}

	/**
	 * The internal z component
	 */
	private double z;

	/**
	 * The internal y component
	 */
	private double y;

	/**
	 * The internal x component
	 */
	private double x;

	/**
	 * Constructs a vector using the specified coordinates.
	 * 
	 * @param mutable
	 *            whether this vector can be directly modified
	 * @param x
	 *            the x-coordinate of this vector
	 * @param y
	 *            the y-coordinate of this vector
	 * @param z
	 *            the z-coordinate of this vector
	 * @throws IllegalArgumentException
	 *             if any component is {@code NaN}
	 */
	private Vector3d(final boolean mutable, final double x, final double y, final double z) {
		super(mutable);
		if (java.lang.Double.isNaN(x)) {
			throw new IllegalArgumentException("x is NaN");
		}
		if (java.lang.Double.isNaN(y)) {
			throw new IllegalArgumentException("y is NaN");
		}
		if (java.lang.Double.isNaN(z)) {
			throw new IllegalArgumentException("z is NaN");
		}
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Returns the x-coordinate of this vector.
	 * 
	 * @return the x-coordinate of this vector
	 */
	public double x() {
		return this.x;
	}

	/**
	 * Sets the x component to the specified value.
	 * 
	 * @param value
	 *            the new x value
	 * @return the old x value
	 * @throw UnsupportedOperationException if this vector is not mutable
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public double setX(double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		double old = this.x;
		this.x = value;
		return old;
	}

	/**
	 * Returns a new mutable copy of this vector. The returned vector will be at
	 * the same position as this one, but with the x value set to the specified
	 * value.
	 * 
	 * @param value
	 *            the new x value
	 * @return a mutable vector that uses the specified value for its x axis
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d withX(double value) {
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		Vector3d result = this.toMutable();
		result.setX(value);
		return result;
	}

	/**
	 * Add the specified x value to this vector.
	 * 
	 * @param offset
	 *            the value to add
	 * @return the old x value
	 * @throw UnsupportedOperationException if this vector is not mutable
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public double addX(double offset) {
		return this.setX(this.x() + offset);
	}

	/**
	 * Return a mutable vector that has the same position as this one, except
	 * for the specified translation.
	 * 
	 * @param offset
	 *            the value to add
	 * @return a new mutable vector at {@code (x + offset, y, z)}
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d addedX(double offset) {
		Vector3d vector = this.toMutable();
		vector.addX(offset);
		return vector;
	}

	/**
	 * Subtract the specified value from this vector's X axis.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return the old value at the X axis
	 * @see #subtractedX(int)
	 * @throw UnsupportedOperationException if this vector is not mutable
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public double subtractX(double offset) {
		return this.setX(this.x() - offset);
	}

	/**
	 * Return a mutable vector at this vector's position, but with the specified
	 * translation.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return a mutable vector at {@code (x - offset, y, z)}
	 * @see #subtractX(int)
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d subtractedX(double offset) {
		return this.withX(this.x() - offset);
	}

	/**
	 * Multiply the specified x value of this vector.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return the old x value
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 * @throw UnsupportedOperationException if this vector is not mutable
	 */
	public double multiplyX(double factor) {
		return this.setX(this.x() * factor);
	}

	/**
	 * Return a mutable copy of this vector, with a multiplied x value.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return a mutable vector at {@code (x * offset, y, z)}
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d mulipliedX(double factor) {
		Vector3d vector = this.toMutable();
		vector.multiplyX(factor);
		return vector;
	}

	/**
	 * Returns the y-coordinate of this vector.
	 * 
	 * @return the y-coordinate of this vector
	 */
	public double y() {
		return this.y;
	}

	/**
	 * Sets the y position to the specified value.
	 * 
	 * @param value
	 *            the new y value
	 * @return the old y value
	 * @throw UnsupportedOperationException if the vector is not mutable
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public double setY(double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		double old = this.y;
		this.y = value;
		return old;
	}

	/**
	 * Returns a translated mutable vector. The returned vector will be at the
	 * same position as this one, but with the y value set to the specified
	 * value.
	 * 
	 * @param value
	 *            the new y value
	 * @return a mutable vector that uses the specified value for its y axis
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d withY(double value) {
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		Vector3d result = this.toMutable();
		result.setY(value);
		return result;
	}

	/**
	 * Add the specified y value to this vector.
	 * 
	 * @param offset
	 *            the value to add
	 * @return the old y value
	 */
	public double addY(double offset) {
		return this.setY(this.y() + offset);
	}

	/**
	 * Return a mutable vector that has the same position as this one, except
	 * for the specified translation.
	 * 
	 * @param offset
	 *            the value to add
	 * @return a vector at {@code (x, y + offset, z)}
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d addedY(double offset) {
		Vector3d vector = this.toMutable();
		vector.addY(offset);
		return vector;
	}

	/**
	 * Subtract the specified value from this vector's Y axis.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return the old value at the Y axis
	 * @see #subtractedY(int)
	 * @throw UnsupportedOperationException if this vector is not mutable
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public double subtractY(double offset) {
		return this.setY(this.y() - offset);
	}

	/**
	 * Return a mutable vector at this vector's position, but with the specified
	 * translation.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return a mutable vector at {@code (x, y - offset, z)}
	 * @see #subtractY(int)
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d subtractedY(double offset) {
		return this.withY(this.y() - offset);
	}

	/**
	 * Multiply the specified y value of this vector.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return the old y value
	 */
	public double multiplyY(double factor) {
		return this.setY(this.y() * factor);
	}

	/**
	 * Return a mutable copy of this vector, with a multiplied y value.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return a mutable vector at {@code (x, y * offset, z)}
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d mulipliedY(double factor) {
		Vector3d vector = this.toMutable();
		vector.multiplyY(factor);
		return vector;
	}

	/**
	 * Returns the z-coordinate of this vector.
	 * 
	 * @return the z-coordinate of this vector
	 */
	public double z() {
		return this.z;
	}

	/**
	 * Sets the z position to the specified value.
	 * 
	 * @param value
	 *            the new z value
	 * @return the old z value
	 * @throw UnsupportedOperationException if the vector is not mutable
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public double setZ(double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		double old = this.z;
		this.z = value;
		return old;
	}

	/**
	 * Returns a translated mutable vector. The returned vector will be at the
	 * same position as this one, but with the z value set to the specified
	 * value.
	 * 
	 * @param value
	 *            the new z value
	 * @return a mutable vector that uses the specified value for its z axis
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d withZ(double value) {
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value must not be NaN");
		}
		Vector3d result = this.toMutable();
		result.setZ(value);
		return result;
	}

	/**
	 * Add the specified z value to this vector.
	 * 
	 * @param offset
	 *            the value to add
	 * @return the old z value
	 */
	public double addZ(double offset) {
		return this.setZ(this.z() + offset);
	}

	/**
	 * Return a mutable vector that has the same position as this one, except
	 * for the specified translation.
	 * 
	 * @param offset
	 *            the value to add
	 * @return a vector at {@code (x, y, z + offset)}
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d addedZ(double offset) {
		Vector3d vector = this.toMutable();
		vector.addZ(offset);
		return vector;
	}

	/**
	 * Subtract the specified value from this vector's Z axis.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return the old value at the Z axis
	 * @see #subtractedZ(int)
	 * @throw UnsupportedOperationException if this vector is not mutable
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public double subtractZ(double offset) {
		return this.setZ(this.z() - offset);
	}

	/**
	 * Return a mutable vector at this vector's position, but with the specified
	 * translation.
	 * 
	 * @param offset
	 *            the value to subtract
	 * @return a mutable vector at {@code (x, y, z - offset)}
	 * @see #subtractZ(int)
	 * @throw IllegalArgumentException if {@code offset} is NaN
	 */
	public Vector3d subtractedZ(double offset) {
		return this.withZ(this.z() - offset);
	}

	/**
	 * Multiply the specified z value of this vector.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return the old z value
	 */
	public double multiplyZ(double factor) {
		return this.setZ(this.z() * factor);
	}

	/**
	 * Return a mutable copy of this vector, with a multiplied z value.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return a mutable vector at {@code (x, y, z * offset)}
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public Vector3d mulipliedZ(double factor) {
		Vector3d vector = this.toMutable();
		vector.multiplyZ(factor);
		return vector;
	}

	@Override
	public void set(Vector3d vector) {
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		this.setX(vector.x());
		this.setY(vector.y());
		this.setZ(vector.z());
	}

	/**
	 * Sets all of this vector's values to the specified value.
	 * 
	 * @param value
	 *            the value that will be used
	 * @throw IllegalArgumentException if {@code value} is NaN
	 */
	public void set(double value) {
		this.setX(value);
		this.setY(value);
		this.setZ(value);
	}

	/**
	 * Sets all of this vector's values to the specified values.
	 * 
	 * @param x
	 *            the new x value
	 * @param y
	 *            the new y value
	 * @param z
	 *            the new z value
	 * @throw IllegalArgumentException if any value is NaN. All values are
	 *        checked before any are used.
	 */
	public void set(double x, double y, double z) {
		if (Double.isNaN(x)) {
			throw new IllegalArgumentException("x must not be NaN");
		}
		if (Double.isNaN(y)) {
			throw new IllegalArgumentException("y must not be NaN");
		}
		if (Double.isNaN(z)) {
			throw new IllegalArgumentException("z must not be NaN");
		}
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	@Override
	public void set(Axis axis, Vector3d vector) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		switch (axis) {
		case X:
			this.setX(vector.x());
			return;
		case Y:
			this.setY(vector.y());
			return;
		case Z:
			this.setZ(vector.z());
			return;
		case XY:
			this.setX(vector.x());
			this.setY(vector.y());
			return;
		case XZ:
			this.setX(vector.x());
			this.setZ(vector.z());
			return;
		case YZ:
			this.setY(vector.y());
			this.setZ(vector.z());
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Sets values at the specified axes to the specified value.
	 * 
	 * @param axis
	 *            the axes that will be modified
	 * @param value
	 *            the added value
	 */
	public void set(Axis axis, double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		switch (axis) {
		case X:
			this.setX(value);
			return;
		case Y:
			this.setY(value);
			return;
		case Z:
			this.setZ(value);
			return;
		case XY:
			this.setX(value);
			this.setY(value);
			return;
		case XZ:
			this.setX(value);
			this.setZ(value);
			return;
		case YZ:
			this.setY(value);
			this.setZ(value);
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Return a mutable copy of this vector, with the copy's axis values set to
	 * the specified value.
	 * 
	 * @param axis
	 *            the axes that are modified
	 * @param value
	 *            the new axis value
	 * @return a modified, mutable copy of this vector
	 */
	public Vector3d with(Axis axis, double value) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		Vector3d result = this.toMutable();
		result.set(axis, value);
		return result;
	}

	@Override
	public void add(Vector3d vector) {
		this.addX(vector.x());
		this.addY(vector.y());
		this.addZ(vector.z());
	}

	/**
	 * Adds the specified value to all of this vector's values.
	 * 
	 * @param value
	 *            the value that will be used
	 */
	public void add(double value) {
		this.addX(value);
		this.addY(value);
		this.addZ(value);
	}

	@Override
	public void add(Axis axis, Vector3d vector) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		switch (axis) {
		case X:
			this.addX(vector.x());
			return;
		case Y:
			this.addY(vector.y());
			return;
		case Z:
			this.addZ(vector.z());
			return;
		case XY:
			this.addX(vector.x());
			this.addY(vector.y());
			return;
		case XZ:
			this.addX(vector.x());
			this.addZ(vector.z());
			return;
		case YZ:
			this.addY(vector.y());
			this.addZ(vector.z());
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Adds the specified value to the specified axes.
	 * 
	 * @param axis
	 *            the axes that will be modified
	 * @param value
	 *            the added value
	 */
	public void add(Axis axis, double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		switch (axis) {
		case X:
			this.addX(value);
			return;
		case Y:
			this.addY(value);
			return;
		case Z:
			this.addZ(value);
			return;
		case XY:
			this.addX(value);
			this.addY(value);
			return;
		case XZ:
			this.addX(value);
			this.addZ(value);
			return;
		case YZ:
			this.addY(value);
			this.addZ(value);
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Returns a mutable vector that's translated by the specified amount.
	 * 
	 * @param value
	 *            the value that will be used
	 * @return a mutable vector that's at this position, but translated by the
	 *         specified amount
	 */
	public Vector3d added(double value) {
		Vector3d result = this.toMutable();
		result.add(value);
		return result;
	}

	/**
	 * Returns a mutable vector at this position, plus the specified
	 * translation.
	 * 
	 * @param axis
	 *            the axes that will be translated
	 * @param value
	 *            the added value
	 * @return a mutable vector translated from this position
	 */
	public Vector3d added(Axis axis, double value) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		Vector3d result = this.toMutable();
		result.add(axis, value);
		return result;
	}

	@Override
	public void subtract(Vector3d vector) {
		this.subtractX(vector.x());
		this.subtractY(vector.y());
		this.subtractZ(vector.z());
	}

	/**
	 * Subtracts the specified value from each of this vector's values.
	 * 
	 * @param value
	 *            the value that will be used
	 */
	public void subtract(double value) {
		this.subtractX(value);
		this.subtractY(value);
		this.subtractZ(value);
	}

	@Override
	public void subtract(Axis axis, Vector3d vector) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		switch (axis) {
		case X:
			this.subtractX(vector.x());
			return;
		case Y:
			this.subtractY(vector.y());
			return;
		case Z:
			this.subtractZ(vector.z());
			return;
		case XY:
			this.subtractX(vector.x());
			this.subtractY(vector.y());
			return;
		case XZ:
			this.subtractX(vector.x());
			this.subtractZ(vector.z());
			return;
		case YZ:
			this.subtractY(vector.y());
			this.subtractZ(vector.z());
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Subtracts the specified value from the specified axes.
	 * 
	 * @param axis
	 *            the axes that will be modified
	 * @param value
	 *            the subtracted value
	 */
	public void subtract(Axis axis, double value) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		switch (axis) {
		case X:
			this.subtractX(value);
			return;
		case Y:
			this.subtractY(value);
			return;
		case Z:
			this.subtractZ(value);
			return;
		case XY:
			this.subtractX(value);
			this.subtractY(value);
			return;
		case XZ:
			this.subtractX(value);
			this.subtractZ(value);
			return;
		case YZ:
			this.subtractY(value);
			this.subtractZ(value);
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	/**
	 * Returns a mutable vector that's translated by the specified amount.
	 * 
	 * @param value
	 *            the value that will be used
	 * @return a mutable vector that's at this position, but translated by the
	 *         specified amount
	 */
	public Vector3d subtracted(double value) {
		Vector3d result = this.toMutable();
		result.add(value);
		return result;
	}

	/**
	 * Returns a mutable vector at this position, minus the specified
	 * translation.
	 * 
	 * @param axis
	 *            the axes that will be translated
	 * @param value
	 *            the subtracted value
	 * @return a mutable vector translated from this position
	 */
	public Vector3d subtracted(Axis axis, double value) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		Vector3d result = this.toMutable();
		result.subtract(axis, value);
		return result;
	}

	@Override
	public void multiply(Vector3d vector) {
		this.multiplyX(vector.x());
		this.multiplyY(vector.y());
		this.multiplyZ(vector.z());
	}

	@Override
	public void multiply(double factor) {
		this.multiplyX(factor);
		this.multiplyY(factor);
		this.multiplyZ(factor);
	}

	@Override
	public void multiply(Axis axis, Vector3d vector) {
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		if (vector == null) {
			throw new NullPointerException("vector must not be null");
		}
		switch (axis) {
		case X:
			this.multiplyX(vector.x());
			return;
		case Y:
			this.multiplyY(vector.y());
			return;
		case Z:
			this.multiplyZ(vector.z());
			return;
		case XY:
			this.multiplyX(vector.x());
			this.multiplyY(vector.y());
			return;
		case XZ:
			this.multiplyX(vector.x());
			this.multiplyZ(vector.z());
			return;
		case YZ:
			this.multiplyY(vector.y());
			this.multiplyZ(vector.z());
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	@Override
	public void multiply(Axis axis, double factor) {
		if (!this.isMutable()) {
			throw new UnsupportedOperationException("vector is not mutable");
		}
		if (axis == null) {
			throw new NullPointerException("Axis must not be null");
		}
		switch (axis) {
		case X:
			this.multiplyX(factor);
			return;
		case Y:
			this.multiplyY(factor);
			return;
		case Z:
			this.multiplyZ(factor);
			return;
		case XY:
			this.multiplyX(factor);
			this.multiplyY(factor);
			return;
		case XZ:
			this.multiplyX(factor);
			this.multiplyZ(factor);
			return;
		case YZ:
			this.multiplyY(factor);
			this.multiplyZ(factor);
			return;
		}
		throw new IllegalArgumentException("Axis is invalid");
	}

	@Override
	public double length() {
		return Math.sqrt(Math.pow(this.x(), 2) + Math.pow(this.y(), 2) + Math.pow(this.z(), 2));
	}

	@Override
	public void normalize() {
		float len = (float) this.length();
		this.set(this.x() / len,
				this.y() / len,
				this.z() / len);
	}

	@Override
	public void interpolate(Vector3d dest, float offset) {
		if (dest == null) {
			throw new NullPointerException("dest must not be null");
		}
		if (offset >= 1f) {
			this.set(dest);
		} else if (offset >= 0f) {
			this.x += (dest.x - this.x) * offset;
			this.y += (dest.y - this.y) * offset;
			this.z += (dest.z - this.z) * offset;
		}
	}

	@Override
	public void cross(Vector3d other) {
		this.set(this.y() * other.z() - other.y() * this.z(),
				-this.x() * other.z() + other.x() * this.z(),
				this.x() * other.y() - other.x() * this.y());
	}

	@Override
	public void clear() {
		this.set(0d);
	}

	@Override
	public void clear(Axis axis) {
		this.set(axis, 0d);
	}

	@Override
	public Vector3d toMutable() {
		return Vector3d.mutable(x, y, z);
	}

	@Override
	public Vector3d toFrozen() {
		if (!this.isMutable()) {
			return this;
		}
		return Vector3d.frozen(x, y, z);
	}

	@Override
	public Vector3d getThis() {
		return this;
	}

	@Override
	public boolean at(Vector3d vector) {
		if (vector == null) {
			return false;
		}
		return this.x() == vector.x() &&
				this.y() == vector.y() &&
				this.z() == vector.z();
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
		if (this.isMutable() != other.isMutable()) {
			return false;
		}
		if (this.x() != other.x()) {
			return false;
		}
		if (this.y() != other.y()) {
			return false;
		}
		if (this.z() != other.z()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 13;
		result = 31 * result + (this.isMutable() ? 1 : 0);
		final long xLong = java.lang.Double.doubleToLongBits(this.x());
		final long yLong = java.lang.Double.doubleToLongBits(this.y());
		final long zLong = java.lang.Double.doubleToLongBits(this.z());
		result = 31 * result + (int) (xLong ^ (xLong >>> 32));
		result = 31 * result + (int) (yLong ^ (yLong >>> 32));
		result = 31 * result + (int) (zLong ^ (zLong >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return String.format("Vector3d[%s (%f, %f, %f)]", this.isMutable() ? "mutable" : "frozen", this.x(), this.y(), this.z());
	}

}
