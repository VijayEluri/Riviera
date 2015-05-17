package script.parsing;

import inspect.Inspectable;
import script.ScriptEnvironment;
import script.parsing.tokens.RiffToken;
import script.parsing.tokens.RiffTokenVisitor;

@Inspectable
public class ScriptLine extends ScriptElement implements RiffToken {
	private String string;

	public ScriptLine(ScriptEnvironment env, String filename, int num, String string) {
		super(env, filename, num, string, string.length());
		this.string = string;
	}

	public ScriptLine(String string, ScriptLine otherLine, int oLO) {
		super(otherLine, oLO, string.length());
		this.string = string;
	}

	@Inspectable
	public String getString() {
		return this.string;
	}

	public void setString(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return this.getFilename() + "@" + this.getLineNumber() + ": \"" + this.string + '"';
	}

	@Override
	public void accept(RiffTokenVisitor visitor) {
		visitor.visitUnparsed(this.getString());
	}
}
