package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import gui.style.Stylesheet;
import gui.style.StylesheetBackgroundColorElement;
import gui.style.StylesheetBorderElement;
import gui.style.StylesheetFontElement;
import gui.style.StylesheetFontSizeElement;
import gui.style.StylesheetFontStyleElement;
import gui.style.StylesheetMarginElement;
import gui.style.StylesheetPaddingElement;
import gui.style.StylesheetProperty;
import gui.style.dimensions.StylesheetAbsoluteHeightElement;
import gui.style.dimensions.StylesheetAbsoluteWidthElement;
import gui.style.dimensions.StylesheetMagnitude;
import inspect.Nodeable;
import logging.Logs;
import script.ScriptEnvironment;
import script.parsing.ScriptKeywordType;

public class InterfaceElement implements Nodeable, GraphicalElement {
	private Stylesheet classStylesheet, uniqueStylesheet;
	private int xAnchor, yAnchor;
	private Interface_Container parent;
	private ScriptEnvironment environment;

	public InterfaceElement(ScriptEnvironment environment, Stylesheet uniqueStylesheet, Stylesheet classStylesheet) {
		this.environment = environment;
		this.uniqueStylesheet = uniqueStylesheet;
		this.classStylesheet = classStylesheet;
	}

	public void addXAnchor(int addingAmount) {
		this.xAnchor += addingAmount;
	}

	public void addYAnchor(int addingAmount) {
		this.yAnchor += addingAmount;
	}

	public Color getBackgroundColor() {
		return ((StylesheetBackgroundColorElement) this.getStyleElement(StylesheetProperty.BACKGROUNDCOLOR)).getColor();
	}

	public int getBottomBorderMagnitude() {
		return ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERBOTTOM)).getMagnitude();
	}

	public int getBottomFluffMagnitude() {
		return this.getBottomMarginMagnitude() + this.getBottomBorderMagnitude() + this.getBottomPaddingMagnitude();
	}

	public int getBottomMarginMagnitude() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINBOTTOM)).getMagnitude();
	}

	public int getBottomPaddingMagnitude() {
		return ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGBOTTOM)).getMagnitude();
	}

	public Stylesheet getClassStylesheet() {
		return this.classStylesheet;
	}

	public Font getCurrentFont() {
		return new Font(((StylesheetFontElement) this.getStyleElement(StylesheetProperty.FONTNAME)).getFontName(), ((StylesheetFontStyleElement) this.getStyleElement(StylesheetProperty.FONTSTYLE)).getStyle(), ((StylesheetFontSizeElement) this.getStyleElement(StylesheetProperty.FONTSIZE)).getFontSize());
	}

	public Color getCurrentTextColor() {
		return (Color) this.getStyleElement(StylesheetProperty.COLOR);
	}

	@Override
	public Rectangle getDrawingBounds() {
		return new Rectangle(this.xAnchor, this.yAnchor, this.getInternalWidth() - 1, this.getInternalHeight() - 1);
	}

	@Override
	public ScriptEnvironment getEnvironment() {
		return this.environment;
	}

	public int getFullHeight() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINTOP)).getMagnitude() + ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERTOP)).getMagnitude() + ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGTOP)).getMagnitude() + this.getInternalHeight() + ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGBOTTOM)).getMagnitude() + ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERBOTTOM)).getMagnitude() + ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINBOTTOM)).getMagnitude();
	}

	public int getFullWidth() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINLEFT)).getMagnitude() + ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERLEFT)).getMagnitude() + ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGLEFT)).getMagnitude() + this.getInternalWidth() + ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGRIGHT)).getMagnitude() + ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERRIGHT)).getMagnitude() + ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINRIGHT)).getMagnitude();
	}

	public int getHorizontalFluffMagnitude() {
		return this.getLeftFluffMagnitude() + this.getRightFluffMagnitude();
	}

	public int getInternalHeight() {
		StylesheetMagnitude<?> element = (StylesheetMagnitude<?>) this.getStyleElement(StylesheetProperty.HEIGHT);
		if (element instanceof StylesheetAbsoluteHeightElement) {
			return ((Integer) element.getMagnitude()).intValue();
		} else {
			return (int) (((Double) element.getMagnitude()).doubleValue() * this.getParent().getContainerElement().getInternalHeight() - this.getVerticalFluffMagnitude());
		}
	}

	public int getInternalWidth() {
		StylesheetMagnitude<?> element = (StylesheetMagnitude<?>) this.getStyleElement(StylesheetProperty.WIDTH);
		if (element instanceof StylesheetAbsoluteWidthElement) {
			return ((Integer) element.getMagnitude()).intValue();
		} else {
			return (int) (((Double) element.getMagnitude()).doubleValue() * this.getParent().getContainerElement().getInternalWidth() - this.getHorizontalFluffMagnitude());
		}
	}

	public int getLeftBorderMagnitude() {
		return ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERLEFT)).getMagnitude();
	}

	public int getLeftFluffMagnitude() {
		return this.getLeftMarginMagnitude() + this.getLeftBorderMagnitude() + this.getLeftPaddingMagnitude();
	}

	public int getLeftMarginMagnitude() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINLEFT)).getMagnitude();
	}

	public int getLeftPaddingMagnitude() {
		return ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGLEFT)).getMagnitude();
	}

	public Object getNonRecursiveStyleElement(StylesheetProperty code) {
		Object element = null;
		if (this.getUniqueStylesheet() != null) {
			element = this.getUniqueStylesheet().getElement(code);
			if (element != null) {
				return element;
			}
		}
		if (this.getClassStylesheet() != null) {
			element = this.getClassStylesheet().getElement(code);
		}
		return element;
	}

	@Override
	public Interface_Container getParent() {
		return this.parent;
	}

	public int getRightBorderMagnitude() {
		return ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERRIGHT)).getMagnitude();
	}

	public int getRightFluffMagnitude() {
		return this.getRightMarginMagnitude() + this.getRightBorderMagnitude() + this.getRightPaddingMagnitude();
	}

	public int getRightMarginMagnitude() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINRIGHT)).getMagnitude();
	}

	public int getRightPaddingMagnitude() {
		return ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGRIGHT)).getMagnitude();
	}

	public InterfaceElement_Root getRoot() {
		if (this.getParent() == null) {
			return null;
		}
		return this.getParent().getContainerElement().getRoot();
	}

	public Object getStyleElement(StylesheetProperty code) {
		Object element = null;
		if (this.getUniqueStylesheet() != null) {
			element = this.getUniqueStylesheet().getElement(code);
			if (element != null) {
				return element;
			}
		}
		if (this.getClassStylesheet() != null) {
			element = this.getClassStylesheet().getElement(code);
			if (element != null) {
				return element;
			}
		}
		return this.getParent().getContainerElement().getStyleElement(code);
	}

	public int getTopBorderMagnitude() {
		return ((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERTOP)).getMagnitude();
	}

	public int getTopFluffMagnitude() {
		return this.getTopMarginMagnitude() + this.getTopBorderMagnitude() + this.getTopPaddingMagnitude();
	}

	public int getTopMarginMagnitude() {
		return ((StylesheetMarginElement) this.getStyleElement(StylesheetProperty.MARGINTOP)).getMagnitude();
	}

	public int getTopPaddingMagnitude() {
		return ((StylesheetPaddingElement) this.getStyleElement(StylesheetProperty.PADDINGTOP)).getMagnitude();
	}

	public Stylesheet getUniqueStylesheet() {
		return this.uniqueStylesheet;
	}

	public int getVerticalFluffMagnitude() {
		return this.getTopFluffMagnitude() + this.getBottomFluffMagnitude();
	}

	public int getXAnchor() {
		return this.xAnchor;
	}

	public int getYAnchor() {
		return this.yAnchor;
	}

	@Override
	public boolean isFocusable() {
		return false;
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Interface Element");
		assert Logs.addSnapNode("Unique Stylesheet", this.uniqueStylesheet);
		assert Logs.addSnapNode("Class Stylesheet", this.classStylesheet);
		assert Logs.closeNode();
	}

	@Override
	public void paint(Graphics2D g2d) {
		if (!((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERLEFT)).getStyle().equals(ScriptKeywordType.none)) {
			g2d.setColor(((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERLEFT)).getColor());
			int xPos = this.getXAnchor() + this.getLeftMarginMagnitude();
			int yPos = this.getYAnchor() + this.getTopMarginMagnitude();
			int width = this.getLeftBorderMagnitude();
			int height = this.getTopBorderMagnitude() + this.getTopPaddingMagnitude() + this.getInternalHeight() + this.getBottomPaddingMagnitude() + this.getBottomBorderMagnitude();
			g2d.fill(new Rectangle(xPos, yPos, width, height));
		}
		if (!((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERRIGHT)).getStyle().equals(ScriptKeywordType.none)) {
			g2d.setColor(((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERRIGHT)).getColor());
			int xPos = this.getXAnchor() + this.getLeftFluffMagnitude() + this.getInternalWidth() + this.getRightPaddingMagnitude();
			int yPos = this.getYAnchor() + this.getTopMarginMagnitude();
			int width = this.getRightBorderMagnitude();
			int height = this.getTopBorderMagnitude() + this.getTopPaddingMagnitude() + this.getInternalHeight() + this.getBottomPaddingMagnitude() + this.getBottomBorderMagnitude();
			g2d.fill(new Rectangle(xPos, yPos, width, height));
		}
		if (!((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERTOP)).getStyle().equals(ScriptKeywordType.none)) {
			g2d.setColor(((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERTOP)).getColor());
			int xPos = this.getXAnchor() + this.getLeftMarginMagnitude();
			int yPos = this.getYAnchor() + this.getTopMarginMagnitude();
			int width = this.getLeftBorderMagnitude() + this.getLeftPaddingMagnitude() + this.getInternalWidth() + this.getRightPaddingMagnitude() + this.getRightBorderMagnitude();
			int height = this.getLeftBorderMagnitude();
			g2d.fill(new Rectangle(xPos, yPos, width, height));
		}
		if (!((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERBOTTOM)).getStyle().equals(ScriptKeywordType.none)) {
			g2d.setColor(((StylesheetBorderElement) this.getStyleElement(StylesheetProperty.BORDERBOTTOM)).getColor());
			int xPos = this.getXAnchor() + this.getLeftMarginMagnitude();
			int yPos = this.getYAnchor() + this.getTopFluffMagnitude() + this.getInternalHeight() + this.getBottomPaddingMagnitude();
			int width = this.getLeftBorderMagnitude() + this.getLeftPaddingMagnitude() + this.getInternalWidth() + this.getRightPaddingMagnitude() + this.getRightBorderMagnitude();
			int height = this.getLeftBorderMagnitude();
			g2d.fill(new Rectangle(xPos, yPos, width, height));
		}
		this.addXAnchor(this.getLeftFluffMagnitude());
		this.addYAnchor(this.getTopFluffMagnitude());
		g2d.setColor(this.getBackgroundColor());
		g2d.fill(new Rectangle(this.getXAnchor(), this.getYAnchor(), this.getInternalWidth(), this.getInternalHeight()));
		g2d.setFont(this.getCurrentFont());
		g2d.setColor(this.getCurrentTextColor());
	}

	public void setClassStylesheet(Stylesheet sheet) {
		this.classStylesheet = sheet;
	}

	@Override
	public void setParent(Interface_Container container) {
		this.parent = container;
	}

	@Override
	public void setPreferredWidth(int width) {
		assert Logs.openNode("Setting Preferred Width (" + width + ")");
		assert Logs.addSnapNode("Element", this);
		if (null == this.getUniqueStylesheet()) {
			this.uniqueStylesheet = new Stylesheet(this.environment);
		}
		this.getUniqueStylesheet().addElement(StylesheetProperty.WIDTH, new StylesheetAbsoluteWidthElement(width));
		Logs.closeNode();
	}

	public void setUniqueStylesheet(Stylesheet sheet) {
		this.uniqueStylesheet = sheet;
	}

	@Override
	public void setXAnchor(int xAnchor) {
		this.xAnchor = xAnchor;
	}

	@Override
	public void setYAnchor(int yAnchor) {
		this.yAnchor = yAnchor;
	}
}
