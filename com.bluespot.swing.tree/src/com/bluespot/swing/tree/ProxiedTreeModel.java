package com.bluespot.swing.tree;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.bluespot.tree.Tree;

/**
 * A TreeModel based on Node and TreeModelDispatcher.
 * <p>
 * This TreeModel relies on a <code>Node</code> for representation and a
 * <code>TreeModelDispatcher</code> for handling changes made to the tree.
 * <p>
 * If you want to use this, I'd recommend looking at
 * <code>DispatchingTreeWalker</code>. It expects a TreeModelDispatcher and
 * emits to it whenever it modifies a tree.
 * 
 * @author Aaron Faanes
 * 
 * @param <T>
 *            The type of the underlying tree.
 */
public class ProxiedTreeModel<T> implements TreeModel {

	private final TreeModelDispatcher dispatcher = new TreeModelDispatcher(this);

	private Tree<T> root;

	public ProxiedTreeModel() {
		this((T) (null));
	}

	public ProxiedTreeModel(final T value) {
		this.root = new Tree<T>(value);
		this.root.setWalkerFactory(DispatchingTreeWalker.<T> createFactory(this.getDispatcher()));
	}

	public ProxiedTreeModel(final Tree<T> root) {
		this.root = root;
	}

	public void addTreeModelListener(final TreeModelListener l) {
		this.dispatcher.addListener(l);
	}

	public Tree<?> getChild(final Object parent, final int index) {
		if (!(parent instanceof Tree<?>)) {
			throw new ClassCastException();
		}
		if (index < 0) {
			throw new IllegalArgumentException("Index cannot be less than 0");
		}
		final Tree<?> node = (Tree<?>) parent;
		return node.get(index);
	}

	public int getChildCount(final Object parent) {
		if (!(parent instanceof Tree<?>)) {
			return 0;
		}
		final Tree<?> node = (Tree<?>) parent;
		return node.size();
	}

	public TreeModelDispatcher getDispatcher() {
		return this.dispatcher;
	}

	public int getIndexOfChild(final Object parent, final Object child) {
		if (!(child instanceof Tree<?>)) {
			throw new ClassCastException();
		}
		if (!(parent instanceof Tree<?>)) {
			return -1;
		}
		final Tree<?> node = (Tree<?>) parent;
		return node.indexOf(child);
	}

	public Tree<T> getRoot() {
		return this.root;
	}

	public boolean isLeaf(final Object nodeObject) {
		final Tree<?> node = (Tree<?>) nodeObject;
		return !node.isRoot() && node.isEmpty();
	}

	public void removeTreeModelListener(final TreeModelListener l) {
		this.dispatcher.removeListener(l);
	}

	public void valueForPathChanged(final TreePath path, final Object newValue) {
		throw new UnsupportedOperationException();
	}

}
