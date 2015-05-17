package gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import gui.event.KeyEvent_KeyDown;
import gui.event.KeyEvent_KeyUp;
import gui.event.RiffInterface_ClickEvent;
import gui.event.RiffInterface_DragEvent;
import gui.event.RiffInterface_Event;
import gui.event.RiffInterface_KeyListener;
import gui.event.RiffInterface_MouseDownEvent;
import gui.event.RiffInterface_MouseListener;
import gui.event.RiffInterface_MouseUpEvent;
import gui.style.Stylesheet;
import gui.style.StylesheetProperty;
import gui.style.dimensions.StylesheetAbsoluteHeightElement;
import gui.style.dimensions.StylesheetAbsoluteWidthElement;
import gui.style.dimensions.StylesheetMagnitude;
import logging.Logs;
import script.ScriptEnvironment;

public class InterfaceElement_Root extends InterfaceElement implements Interface_Container {
	private final JPanel drawingPanel;
	private final List<RiffInterface_MouseListener> mouseListeners = new LinkedList<RiffInterface_MouseListener>();
	private GraphicalElement focusedElement;
	private final List<GraphicalElement> elements = new LinkedList<GraphicalElement>();

	public InterfaceElement_Root(ScriptEnvironment environment, JPanel drawingPanel) {
		super(environment, null, null);
		this.drawingPanel = drawingPanel;
	}

	@Override
	public void add(GraphicalElement element) {
		assert Logs.openNode("Interface-Element Addition");
		assert Logs.addSnapNode("This Element", this);
		assert Logs.addSnapNode("Added Element", element);
		this.elements.add(element);
		element.setParent(this);
		assert Logs.closeNode();
	}

	public void add(RiffInterface_MouseListener element) {
		if (element instanceof InterfaceElement) {
			this.add((InterfaceElement) element);
		}
		this.mouseListeners.add(element);
	}

	@Override
	public void clear() {
		this.elements.clear();
	}

	public void dispatchEvent(RiffInterface_Event rawEvent) {
		if (rawEvent instanceof KeyEvent_KeyUp) {
			KeyEvent_KeyUp event = (KeyEvent_KeyUp) rawEvent;
			if (this.focusedElement == null) {
				return;
			}
			if (this.focusedElement instanceof RiffInterface_KeyListener) {
				((RiffInterface_KeyListener) this.focusedElement).riffKeyEvent(event);
			}
		} else if (rawEvent instanceof KeyEvent_KeyDown) {
			KeyEvent_KeyDown event = (KeyEvent_KeyDown) rawEvent;
			if (this.focusedElement == null) {
				return;
			}
			if (this.focusedElement instanceof RiffInterface_KeyListener) {
				((RiffInterface_KeyListener) this.focusedElement).riffKeyEvent(event);
			}
		} else if (rawEvent instanceof RiffInterface_MouseDownEvent) {
			RiffInterface_MouseDownEvent event = (RiffInterface_MouseDownEvent) rawEvent;
			GraphicalElement element = this.getElement(event.getX(), event.getY());
			if (element == null) {
				this.focusedElement = null;
				return;
			}
			if (element.isFocusable()) {
				this.focusedElement = element;
			}
			if (element instanceof RiffInterface_MouseListener) {
				((RiffInterface_MouseListener) element).riffMouseEvent(event);
			}
		} else if (rawEvent instanceof RiffInterface_MouseUpEvent) {
			RiffInterface_MouseUpEvent event = (RiffInterface_MouseUpEvent) rawEvent;
			GraphicalElement element = this.getElement(event.getX(), event.getY());
			if (element == null) {
				return;
			}
			if (element instanceof RiffInterface_MouseListener) {
				((RiffInterface_MouseListener) element).riffMouseEvent(event);
			}
		} else if (rawEvent instanceof RiffInterface_ClickEvent) {
			RiffInterface_ClickEvent event = (RiffInterface_ClickEvent) rawEvent;
			GraphicalElement element = this.getElement(event.getX(), event.getY());
			if (element == null) {
				this.focusedElement = null;
				return;
			}
			if (element.isFocusable()) {
				this.focusedElement = element;
			}
			if (element instanceof RiffInterface_MouseListener) {
				((RiffInterface_MouseListener) element).riffMouseEvent(event);
			}
		} else if (rawEvent instanceof RiffInterface_DragEvent) {
			RiffInterface_DragEvent event = (RiffInterface_DragEvent) rawEvent;
			if (this.focusedElement == null) {
				return;
			}
			if (this.focusedElement instanceof RiffInterface_MouseListener) {
				((RiffInterface_MouseListener) this.focusedElement).riffMouseEvent(event);
			}
		} else {
			assert Logs.addNode("No applicable listener found");
		}
	}

	@Override
	public InterfaceElement getContainerElement() {
		return this;
	}

	public GraphicalElement getElement(int x, int y) {
		for (GraphicalElement element : this.getElements()) {
			if (element.getDrawingBounds().contains(x, y)) {
				return element;
			}
		}
		return null;
	}

	@Override
	public java.util.List<GraphicalElement> getElements() {
		return Collections.unmodifiableList(this.elements);
	}

	public Graphics2D getGraphics() {
		return (Graphics2D) this.drawingPanel.getGraphics();
	}

	@Override
	public int getInternalHeight() {
		StylesheetMagnitude<?> element = (StylesheetMagnitude<?>) this.getStyleElement(StylesheetProperty.HEIGHT);
		if (element instanceof StylesheetAbsoluteHeightElement) {
			return ((Integer) element.getMagnitude()).intValue();
		} else {
			return (int) (((Double) element.getMagnitude()).doubleValue() * this.drawingPanel.getHeight() - this.getVerticalFluffMagnitude());
		}
	}

	@Override
	public int getInternalWidth() {
		StylesheetMagnitude<?> element = (StylesheetMagnitude<?>) this.getStyleElement(StylesheetProperty.WIDTH);
		if (element instanceof StylesheetAbsoluteWidthElement) {
			return ((Integer) element.getMagnitude()).intValue();
		} else {
			return (int) (((Double) element.getMagnitude()).doubleValue() * this.drawingPanel.getWidth() - this.getHorizontalFluffMagnitude());
		}
	}

	@Override
	public InterfaceElement_Root getRoot() {
		return this;
	}

	@Override
	public Object getStyleElement(StylesheetProperty code) {
		Stylesheet sheet = this.getUniqueStylesheet();
		if (sheet == null) {
			this.setUniqueStylesheet(new Stylesheet(this.getEnvironment()));
			sheet = this.getUniqueStylesheet();
		}
		Object element = this.getUniqueStylesheet().getElement(code);
		if (element != null) {
			return element;
		}
		element = code.defaultValue();
		sheet.addElement(code, element);
		return element;
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Root Interface Element");
		super.nodificate();
		assert Logs.addSnapNode("Elements: " + this.elements.size() + " element(s)", this.elements);
		assert Logs.closeNode();
	}

	@Override
	public void paint(Graphics2D g2d) {
		assert Logs.openNode("Paint Operations", "Painting operation: " + this.getElements().size() + " element(s)");
		this.setXAnchor(0);
		this.setYAnchor(0);
		super.paint(g2d);
		int xAnchorOffset = this.getXAnchor();
		int yAnchorOffset = this.getYAnchor();
		int nextLineAnchorOffset = 0;
		int width = this.getInternalWidth();
		for (GraphicalElement element : this.getElements()) {
			Rectangle rect = element.getDrawingBounds();
			if (rect.getWidth() > width - xAnchorOffset) {
				element.setPreferredWidth(width - xAnchorOffset);
				rect = element.getDrawingBounds();
				if (rect.getWidth() > width - xAnchorOffset) {
					yAnchorOffset += nextLineAnchorOffset;
					xAnchorOffset = this.getXAnchor();
					nextLineAnchorOffset = 0;
				}
			}
			element.setXAnchor(xAnchorOffset);
			element.setYAnchor(yAnchorOffset);
			xAnchorOffset += rect.getWidth();
			if (nextLineAnchorOffset < rect.getHeight()) {
				nextLineAnchorOffset = (int) rect.getHeight();
			}
			element.paint(g2d);
		}
		assert Logs.closeNode();
	}

	public void repaint() {
		this.drawingPanel.repaint();
	}
}
