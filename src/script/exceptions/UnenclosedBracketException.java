/**
 * 
 */
package script.exceptions;

import script.parsing.Referenced;

public class UnenclosedBracketException extends ScriptException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4450711851110352808L;

	public UnenclosedBracketException(Referenced elem) {
		super(elem);
	}

	@Override
	public String getName() {
		return "Unenclosed Bracket";
	}
}