package script.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import inspect.Nodeable;
import logging.Logs;
import script.ScriptEnvironment;
import script.parsing.Referenced;
import script.parsing.ScriptElement;

/**
 * 
 * @author Aaron Faanes
 * @see ScriptException
 */
public class InternalException extends RuntimeException implements Nodeable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6803130804744137902L;
	private int offset, lineNumber, length;
	private String line, filename, message;
	private final ScriptEnvironment environment;
	private final Object object;

	public InternalException(Referenced ref, String message) {
		this(ref.getDebugReference().getEnvironment(), ref.getDebugReference(), message);
	}

	public InternalException(ScriptEnvironment env, Exception object) {
		this(env, object, "");
	}

	public InternalException(ScriptEnvironment env, Object element, String message) {
		this.environment = env;
		this.object = element;
		this.filename = null;
		this.line = null;
		this.offset = 0;
		this.lineNumber = -1;
		this.length = -1;
		this.message = message;
	}

	public InternalException(ScriptEnvironment env, ScriptElement element, String message) {
		this.object = null;
		this.environment = env;
		this.message = message;
		if (element != null) {
			this.filename = element.getFilename();
			this.lineNumber = element.getLineNumber();
			this.line = element.getOriginalString();
			this.offset = element.getOffset();
			this.length = element.getLength();
		} else {
			this.offset = 0;
			this.line = null;
			this.filename = null;
			this.lineNumber = -1;
			this.length = -1;
		}
	}

	public InternalException(ScriptEnvironment env, String message) {
		this.environment = env;
		this.object = null;
		this.filename = null;
		this.line = null;
		this.offset = 0;
		this.lineNumber = 0;
		this.length = -1;
		this.message = message;
	}

	public InternalException(String message) {
		this((ScriptEnvironment) null, message);
	}

	public InternalException(String message, Exception exception) {
		this(null, exception, message);
	}

	public ScriptEnvironment getEnvironment() {
		return this.environment;
	}

	public String getFilename() {
		return this.filename;
	}

	public String getFragment() {
		return this.getOriginalString().substring(this.getOffset(), this.getOffset() + this.getLength());
	}

	public int getLength() {
		return this.length;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	@Override
	public String getMessage() {
		return "(Internal Error) " + this.getName();
	}

	public String getName() {
		return this.message;
	}

	public int getOffset() {
		return this.offset;
	}

	public String getOriginalString() {
		return this.line;
	}

	public boolean isAnonymous() {
		return this.filename == null;
	}

	@Override
	public void nodificate() {
		boolean debug = false;
		assert debug = true;
		if (!debug) {
			return;
		}
		assert Logs.openNode("Exceptions and Errors", this.getMessage());
		if (this.object != null) {
			assert Logs.addNode(this.object);
		}
		StringWriter writer;
		this.printStackTrace(new PrintWriter(writer = new StringWriter()));
		String[] messages = writer.toString().split("\n");
		boolean flag = false;
		int added = 0;
		for (int i = 0; i < messages.length; i++) {
			if (!flag && messages[i].trim().indexOf("at") == 0) {
				flag = true;
				assert Logs.openNode("Call-stack");
			}
			if (flag && added == 5) {
				assert Logs.openNode("Full Call-Stack");
			}
			if (messages[i].trim().indexOf("^") != 0) {
				assert Logs.addNode(messages[i].trim());
			}
			if (flag) {
				added++;
			}
		}
		if (added > 5) {
			assert Logs.closeNode();
		}
		if (flag) {
			assert Logs.closeNode();
		}
		assert Logs.closeNode();
	}

	@Override
	public String toString() {
		if (this.object != null) {
			if (this.object instanceof Exception) {
				StringWriter writer;
				((Exception) this.object).printStackTrace(new PrintWriter(writer = new StringWriter()));
				return this.getMessage() + "\nObject given: " + this.object + "\n" + writer;
			} else {
				return this.getMessage() + "\nObject given: " + this.object;
			}
		}
		if (this.filename == null) {
			return this.getMessage();
		}
		while (this.line.indexOf("\t") == 0 || this.line.indexOf(" ") == 0) {
			this.line = this.line.substring(1);
			this.offset--;
			if (this.offset < 0) {
				this.offset = 0;
			}
		}
		String string = this.filename + ":" + this.lineNumber + ": " + this.getMessage() + "\n\t" + this.line;
		string += "\n\t";
		for (int i = 0; i < this.offset; i++) {
			string += " ";
		}
		string += "^";
		return string;
	}
}
