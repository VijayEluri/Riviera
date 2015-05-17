/**
 * 
 */
package script.exceptions;

import java.util.List;

import logging.Logs;
import script.ScriptEnvironment;
import script.parsing.Referenced;
import script.values.RiffScriptFunction;

public class FunctionNotFoundScriptException extends ScriptException {
	private static final long serialVersionUID = 4051248649703169850L;
	private String name;
	private List<?> params;

	public FunctionNotFoundScriptException(Object ref, String name, List<?> params) {
		this(((Referenced) ref).getEnvironment(), ref, name, params);
	}

	public FunctionNotFoundScriptException(ScriptEnvironment env, Object ref, String name, List<?> params) {
		super(env, ref);
		this.name = name;
		this.params = params;
	}

	public FunctionNotFoundScriptException(ScriptEnvironment env, String name, List<?> params) {
		super(env);
		this.name = name;
		this.params = params;
	}

	@Override
	public void getExtendedInformation() {
		assert Logs.addNode("The function, " + RiffScriptFunction.getDisplayableFunctionName(this.name) + ", was not found");
	}

	@Override
	public String getName() {
		return "Function not found (" + RiffScriptFunction.getDisplayableFunctionName(this.name) + ")";
	}

	public List<?> getParams() {
		return this.params;
	}
}