package com.bluespot.ide;

import java.awt.Component;

import javax.swing.JPanel;

import com.bluespot.swing.Repeater;
import com.bluespot.swing.ViewStack;

public class PerspectivesPanel extends PerspectiveComponent {

	private final JPanel panel = new JPanel();

	public PerspectivesPanel(final PerspectiveManager manager) {
		super(manager);

		new ViewStack(this.getPanel(), manager.getStateModel()) {

			@Override
			protected String getName(final int index) {
				return manager.getPerspectives().get(index).getName();
			}

		};

		new Repeater<Component, Perspective>(this.getPanel(), manager.getPerspectives()) {

			@Override
			public Component createComponent(final Perspective perspective) {
				return perspective.getComponent();
			}

			@Override
			protected void addComponent(final Component component, final int index, final Perspective perspective) {
				this.getParent().add(component, perspective.getName(), index);
			}

		};

	}

	public JPanel getPanel() {
		return this.panel;
	}
}
