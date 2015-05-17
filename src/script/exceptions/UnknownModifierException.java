/**
 * 
 */
package script.exceptions;

import java.util.List;

import logging.Logs;
import script.parsing.Referenced;

public class UnknownModifierException extends ScriptException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7862513783807915122L;
	private List<Object> modifiers;

	public UnknownModifierException(Referenced ref, List<Object> modifiers) {
		super(ref);
		this.modifiers = modifiers;
	}

	@Override
	public void getExtendedInformation() {
		assert Logs.addSnapNode("These modifiers (or what are believed to be modifiers) are unparseable to the compiler", this.modifiers);
	}

	@Override
	public String getName() {
		return "Unknown Modifier(s)";
	}
}