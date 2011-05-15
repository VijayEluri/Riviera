package com.bluespot.geom.vectors;

import com.bluespot.geom.Axis;

/**
 * Represents a three-dimensional vector, of varying precision. Vectors may or
 * may not be mutable, but mutability must never change for a given vector.
 * <p>
 * This vector interface uses a slightly unusual recursive type definition. This
 * lets us have a common interface for vectors, while still avoiding the
 * performance penalties of boxing. Unfortunately, that means there are many
 * methods that are not part of this interface, but are still implied for
 * {@link Vector3} implementations. Refer to {@link Vector3i}, {@link Vector3d},
 * or {@link Vector3f} for further reference.
 * <p>
 * Most operations support several different variants:
 * 
 * <pre>
 * // Operate on this object, using all values from the specified vector.
 * void add(V value);
 * 
 * // Operate on this object, using the specified value for all axes.
 * void add(primitive value);
 * 
 * // Operate on this object, using the values from the specified vector
 * // to modify this object's values at the specified axes. 
 * void add(Axis axis, V value);
 * 
 * // Operate on this object, using the primitive value to modify this
 * // vector's values at the specified axes. 
 * void add(Axis axis, primitive value);
 * 
 * // Operate on a copy, using all values from the specified vector. 
 * V added(V value)
 *  
 * // Operate on a copy, using the specified value for all axes.
 * V added(primitive value);
 * 
 * // Operate on a copy, using the values from the specified vector
 * // to modify the copy's values at the specified axes. 
 * V added(Axis axis, V value);
 * 
 * // Operate on a copy, using the primitive value to modify the
 * // copy's values at the specified axes. 
 * V added(Axis axis, primitive value);
 * </pre>
 * 
 * Some operations may be omitted if they don't make sense. They should also be
 * omitted if some other more common mechanism provides them. For example,
 * {@code cleared()} is already implemented through an {@code origin()} method.
 * <p>
 * The naming convention for operations is to name the creating methods with the
 * -ed suffix (inverted, cleared, etc.), though exceptions exist: setX's
 * immutable variant is withX. Be pragmatic and use what reads well.
 * 
 * @author Aaron Faanes
 * @param <V>
 *            the type of vector object. This should be recursive.
 * @see AbstractVector3
 * @see Vector3d
 * @see Vector3f
 * @see Vector3i
 * @see Vectors
 */
public interface Vector3<V extends Vector3<V>> {

	/**
	 * Return whether this vector can be directly modified. This value is a
	 * constant for a given instance.
	 * 
	 * @return {@code true} if this vector can be directly modified
	 */
	public boolean isMutable();

	/**
	 * Copy the specified vector to this vector.
	 * 
	 * @param vector
	 *            the vector that will be copied
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void set(V vector);

	/**
	 * Copy another vector's value at the specified axis.
	 * 
	 * @param axis
	 *            the axis to copy
	 * @param vector
	 *            the vector from which to copy
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void set(Axis axis, V vector);

	/**
	 * Return a mutable vector that uses the specified vector's values for the
	 * specified axes.
	 * 
	 * @param axis
	 *            the axes that will be modified
	 * @param vector
	 *            the vector that will be added
	 * @return a modified, mutable copy of this vector
	 */
	public V with(Axis axis, V vector);

	/**
	 * Add the specified vector's value to this vector.
	 * 
	 * @param vector
	 *            the vector that will be added
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void add(V vector);

	/**
	 * Add another vector's value at the specified axis.
	 * 
	 * @param axis
	 *            the axis to copy
	 * @param vector
	 *            the vector from which to copy
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void add(Axis axis, V vector);

	/**
	 * Return a mutable vector that has the specified vector's values added to
	 * it.
	 * 
	 * @param vector
	 *            the vector that will be added
	 * @return a mutable vector at this position, but translated by the
	 *         specified vector's values
	 */
	public V added(V vector);

	/**
	 * Return a mutable vector at this position, but with the specified
	 * translation.
	 * 
	 * @param axis
	 *            the axis to copy
	 * @param vector
	 *            the vector from which to copy
	 * @return a mutable vector translated from this vector
	 */
	public V added(Axis axis, V vector);

	/**
	 * Multiply this vector's values by the specified vector's values. This
	 * modified vector will become {@code (x*vector.x, y*vector.y, z*vector.z)}
	 * 
	 * @param vector
	 *            the vector that will be added
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void multiply(V vector);

	/**
	 * Multiply this vector's values by the specified factor. This modified
	 * vector will become {@code (x*factor, y*factor, z*factor)}
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void multiply(double factor);

	/**
	 * Multiply this vector's values at the specified axis, using the values
	 * from the specified vector.
	 * 
	 * @param axis
	 *            the axis to multiply
	 * @param vector
	 *            the vector used in multiplication
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void multiply(Axis axis, V vector);

	/**
	 * Multiply this vector's values at the specified axis by the specified
	 * factor.
	 * 
	 * @param axis
	 *            the axis to multiply
	 * @param factor
	 *            the factor of multiplication
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void multiply(Axis axis, double factor);

	/**
	 * Return a mutable copy of this vector, multiplied by the specified vectors
	 * values.
	 * 
	 * @param vector
	 *            the vector that will be added
	 * @return a mutable vector at this position, but translated by the
	 *         specified vector's values
	 */
	public V multiplied(V vector);

	/**
	 * Return a mutable copy of this vector, multiplied by the specified factor
	 * for all axes.
	 * 
	 * @param factor
	 *            the factor of multiplication
	 * @return a mutable copy of this vector, multiplied by the specified factor
	 */
	public V multiplied(double factor);

	/**
	 * Return a mutable vector at this position, but with the specified
	 * translation.
	 * 
	 * @param axis
	 *            the axis to copy
	 * @param vector
	 *            the vector from which to copy
	 * @return a mutable vector translated from this vector
	 */
	public V multiplied(Axis axis, V vector);

	/**
	 * * Return a mutable copy of this vector, multiplied by the specified
	 * factor for the specified axes.
	 * 
	 * @param axis
	 *            the axis that will be multiplied
	 * @param factor
	 *            the factor of multiplication
	 * @return a modified, mutable copy of this vector
	 */
	public V multiplied(Axis axis, double factor);

	/**
	 * Invert this vector. All components are multiplied by {@code -1}.
	 * 
	 * @throw UnsupportedOperationException if this vector is not mutable
	 */
	public void invert();

	/**
	 * Invert the specified components of this vector.
	 * 
	 * @param axis
	 *            the axis to invert
	 * 
	 * @throw UnsupportedOperationException if this vector is not mutable
	 */
	public void invert(Axis axis);

	/**
	 * Returns a new {@link Vector3} that is the inverse of this vector. All
	 * dimensions are multiplied by {@code -1}.
	 * 
	 * @return a new {@code Vector3} that is the inverse of this vector
	 */
	public V inverted();

	/**
	 * Returns a new {@link Vector3} with inverted values for the specified
	 * axes.
	 * 
	 * @param axis
	 *            the axis to invert
	 * @return a new {@code Vector3} with inverted components
	 */
	public V inverted(Axis axis);

	/**
	 * Interpolates between this vector and the destination. This vector will be
	 * modified as a result of this operation. Offsets that are not between zero
	 * and one are handled specially:
	 * <ul>
	 * <li>If {@code offset <= 0}, nothing is modified
	 * <li>If {@code offset >= 1}, this vector is set to {@code destination}
	 * </ul>
	 * This special behavior allows clients to reliably detect when
	 * interpolation is complete.
	 * 
	 * @param dest
	 *            the final vector
	 * @param offset
	 *            the percentage of distance traveled
	 * @see #interpolated(Vector3, float)
	 * @throws UnsupportedOperationException
	 *             if this vector is immutable
	 */
	public void interpolate(V dest, float offset);

	/**
	 * Return a mutable vector that lies between this vector and the specified
	 * destination. The offset may be any value, but interpolation always occurs
	 * between this vector and the specified one: large or negative offset are
	 * handled specially:
	 * <ul>
	 * <li>If {@code offset <= 0}, this vector should be returned
	 * <li>If {@code offset >= 1}, {@code destination} should be returned
	 * </ul>
	 * Returning copies instead of always interpolating allows clients to
	 * reliably detect when interpolation is complete.
	 * 
	 * @param dest
	 *            the final vector
	 * @param offset
	 *            the percentage of distance traveled.
	 * @return a vector that lies between this vector and the destination
	 * @see #interpolate(Vector3, float)
	 */
	public V interpolated(V dest, float offset);

	/**
	 * Modify this vector by calculating a cross product with the specified
	 * vector.
	 * 
	 * @param other
	 *            the vector used to calculate the cross product
	 * @throw UnsupportedOperationException if this vector is not mutable
	 */
	public void cross(V other);

	/**
	 * Returns a new {@link Vector3d} that is the cross product of this vector
	 * with the specified vector.
	 * 
	 * @param other
	 *            the other vector used in the calculation of the cross product
	 * @return a new {@code Vector3} that represents the cross product of these
	 *         two vectors
	 * @throws NullPointerException
	 *             if {@code other} is null
	 */
	public V crossed(V other);

	/**
	 * Clear all values on this vector.
	 */
	public void clear();

	/**
	 * Clear values for the specified axis.
	 * 
	 * @param axis
	 *            the axis whose values will be cleared
	 */
	public void clear(Axis axis);

	/**
	 * Returns a mutable vector that has zeros for the specified axis.
	 * 
	 * @param axis
	 *            the axis whose values will be cleared
	 * @return a mutable vector at this vector's position, but with zeros for
	 *         the specified axes
	 */
	public V cleared(Axis axis);

	/**
	 * Create and return a new, mutable instance of this vector. The returned
	 * vector will have the same position. New instances will be created even if
	 * this vector is already mutable.
	 * 
	 * @return a new mutable instance of this vector
	 */
	public V toMutable();

	/**
	 * Return an immutable instance of this vector. If the vector is already
	 * immutable, then that vector may be returned directly.
	 * 
	 * @return an immutable instance of this vector
	 */
	public V toFrozen();

	/**
	 * Return whether this vector is at the same location as the specified
	 * vector. This behaves similar to {@link #equals(Object)} but ignores the
	 * mutability of the specified vector.
	 * 
	 * @param vector
	 *            the vector that will be compared against
	 * @return {@code true} if this vector is at the same location as the
	 *         specified vector
	 */
	public boolean at(V vector);

}
