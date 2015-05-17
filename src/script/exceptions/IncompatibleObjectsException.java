/**
 * 
 */
package script.exceptions;

import logging.Logs;
import script.parsing.Referenced;
import script.values.ScriptValue;

public class IncompatibleObjectsException extends ScriptException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1863802775557161344L;
	private ScriptValue lhs, rhs;

	public IncompatibleObjectsException(Referenced ref, ScriptValue lhs, ScriptValue rhs) {
		super(ref);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public void getExtendedInformation() {
		assert Logs.addNode("The following two objects/primitives are incomparable.");
		assert Logs.addNode(this.lhs);
		assert Logs.addNode(this.rhs);
	}

	@Override
	public String getName() {
		return "Incomparable Objects Exception";
	}
}