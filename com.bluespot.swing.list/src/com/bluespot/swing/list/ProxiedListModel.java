package com.bluespot.swing.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.bluespot.dispatcher.StatefulDispatcher;
import com.bluespot.util.ListProxy;

public class ProxiedListModel<E> extends ListProxy<E> implements MutableListModel<E> {

	StatefulDispatcher<ListDataEvent, ListDataListener> dispatcher = new StatefulDispatcher<ListDataEvent, ListDataListener>();

	public ProxiedListModel() {
		this(Collections.<E> emptyList());
	}

	public ProxiedListModel(final List<E> list) {
		super(new ArrayList<E>(list));
	}

	@Override
	public boolean add(final E e) {
		final boolean collectionChanged = super.add(e);
		if (collectionChanged) {
			this.fireIntervalAdded(this.size() - 1, this.size() - 1);
		}
		return collectionChanged;
	}

	@Override
	public void add(final int index, final E element) {
		super.add(index, element);
		this.fireIntervalAdded(index, index);
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		final boolean collectionChanged = super.addAll(c);
		if (collectionChanged) {
			this.fireIntervalAdded(this.size() - c.size(), this.size() - 1);
		}
		return collectionChanged;
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends E> c) {
		final boolean collectionChanged = super.addAll(index, c);
		if (collectionChanged) {
			this.fireIntervalAdded(index, index + c.size() - 1);
		}
		return collectionChanged;
	}

	public void addListDataListener(final ListDataListener listener) {
		this.dispatcher.addListener(listener);
	}

	@Override
	public void clear() {
		final int oldSize = this.size();
		super.clear();
		if (oldSize > 0) {
			this.fireIntervalRemoved(0, oldSize - 1);
		}
	}

	public E getElementAt(final int index) {
		return this.get(index);
	}

	public int getSize() {
		return this.size();
	}

	@Override
	public E remove(final int index) {
		final E oldElement = super.remove(index);
		this.fireIntervalRemoved(index, index);
		return oldElement;
	}

	@Override
	public boolean remove(final Object o) {
		final int index = this.indexOf(o);
		if (index < 0) {
			// The object isn't in our list; return silently.
			return false;
		}
		// We rely on remove(index) to emit our event.
		this.remove(index);
		return true;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		int minimumBounds = -1;
		int maximumBounds = -1;
		for (final Object o : c) {
			int index = this.indexOf(o);
			if (index >= 0) {
				minimumBounds = minimumBounds == -1 ? index : Math.min(minimumBounds, index);
			}
			index = this.lastIndexOf(o);
			if (index >= 0) {
				maximumBounds = maximumBounds == -1 ? index : Math.max(maximumBounds, index);
			}
		}
		if (minimumBounds != -1 || maximumBounds != -1) {
			// The list will be affected.
			super.removeAll(c);
			this.fireContentsChanged(minimumBounds, maximumBounds);
			return true;
		}
		return false;
	}

	public void removeListDataListener(final ListDataListener listener) {
		this.dispatcher.addListener(listener);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		int index = 0;
		int minimumBounds = -1;
		int maximumBounds = -1;
		for (final E element : this) {
			if (c.contains(element) == false) {
				// It is not contained in the given collection, therefore
				// it will be removed.
				if (minimumBounds == -1) {
					minimumBounds = index;
				}
				if (maximumBounds == -1 || maximumBounds < index) {
					maximumBounds = index;
				}
			}
			index++;
		}
		if (minimumBounds != -1 || maximumBounds != -1) {
			// The list will be affected.
			super.retainAll(c);
			this.fireContentsChanged(minimumBounds, maximumBounds);
			return true;
		}
		return false;
	}

	@Override
	public E set(final int index, final E element) {
		final E oldElement = super.set(index, element);
		if (!element.equals(oldElement)) {
			this.fireContentsChanged(index, index);
		}
		return oldElement;
	}

	@Override
	public List<E> subList(final int offset, final int toIndex) {
		final ProxiedListModel<E> sublist = new ProxiedListModel<E>(super.subList(offset, toIndex));

		final List<E> sourceList = this.getSourceList();

		sublist.addListDataListener(new ListDataListener() {

			public void contentsChanged(final ListDataEvent e) {
				final int lowerBound = offset + e.getIndex0();
				final int upperBound = offset + e.getIndex1();
				for (int index = e.getIndex0(); index <= e.getIndex1(); index++) {
					sourceList.set(offset + index, sublist.get(index));
				}
				ProxiedListModel.this.fireContentsChanged(lowerBound, upperBound);
			}

			public void intervalAdded(final ListDataEvent e) {
				final int lowerBound = offset + e.getIndex0();
				final int upperBound = offset + e.getIndex1();
				sourceList.addAll(lowerBound, sublist.subList(e.getIndex0(), e.getIndex1() + 1));
				ProxiedListModel.this.fireIntervalAdded(lowerBound, upperBound);
			}

			public void intervalRemoved(final ListDataEvent e) {
				final int lowerBound = offset + e.getIndex0();
				final int upperBound = offset + e.getIndex1();
				sourceList.subList(lowerBound, upperBound + 1).clear();
				ProxiedListModel.this.fireIntervalRemoved(lowerBound, upperBound);
			}

		});

		this.addListDataListener(new ListDataListener() {

			public void contentsChanged(final ListDataEvent e) {
				for (int index = e.getIndex0(); index <= e.getIndex1(); index++) {
					if (!this.withinRange(index)) {
						continue;
					}
					sublist.set(index - offset, ProxiedListModel.this.get(index));
				}
			}

			public int endIndex() {
				return offset + sublist.size();
			}

			public void intervalAdded(final ListDataEvent e) {
				// Do nothing!

			}

			public void intervalRemoved(final ListDataEvent e) {
				// Do nothing!
			}

			public boolean withinRange(final int index) {
				return offset <= index && index < this.endIndex();
			}
		});

		return sublist;
	}

	protected void fire(final ListDataEventType eventType, final int startIndex, final int endIndex) {
		final ListDataEvent event = eventType.newEvent(this, startIndex, endIndex);
		this.dispatcher.dispatch(eventType, event);
	}

	protected void fireContentsChanged(final int startIndex, final int endIndex) {
		this.fire(ListDataEventType.CONTENTS_CHANGED, startIndex, endIndex);
	}

	protected void fireIntervalAdded(final int startIndex, final int endIndex) {
		this.fire(ListDataEventType.INTERVAL_ADDED, startIndex, endIndex);
	}

	protected void fireIntervalRemoved(final int startIndex, final int endIndex) {
		this.fire(ListDataEventType.INTERVAL_REMOVED, startIndex, endIndex);
	}

}
