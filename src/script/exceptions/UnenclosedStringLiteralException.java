/**
 * 
 */
package script.exceptions;

import script.parsing.Referenced;

public class UnenclosedStringLiteralException extends ScriptException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7575185880647475669L;

	public UnenclosedStringLiteralException(Referenced elem) {
		super(elem);
	}

	@Override
	public String getName() {
		return "Unenclosed String";
	}
}