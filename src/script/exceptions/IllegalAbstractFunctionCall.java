/**
 * 
 */
package script.exceptions;

import logging.Logs;
import script.parsing.Referenced;
import script.values.ScriptTemplate_Abstract;

class IllegalAbstractFunctionCall extends ScriptException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7118648667674833605L;
	private ScriptTemplate_Abstract template;

	public IllegalAbstractFunctionCall(Referenced ref, ScriptTemplate_Abstract template) {
		super(ref);
		this.template = template;
	}

	@Override
	public void getExtendedInformation() {
		assert Logs.addSnapNode("A call was made to an abstract function in this template", this.template);
	}

	@Override
	public String getName() {
		return "Invalid Abstract Function Call";
	}
}