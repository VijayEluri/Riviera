package script.values;

import java.util.List;

import logging.Logs;
import script.ScriptEnvironment;
import script.exceptions.ScriptException;
import script.parsing.Referenced;
import script.parsing.ScriptKeywordType;

public class ScriptFunction_Constructor extends RiffScriptFunction {
	private ScriptEnvironment environment;

	public ScriptFunction_Constructor(ScriptValueType returnType, List<ScriptValue> paramList, ScriptKeywordType permission) {
		super(returnType, paramList, permission, false, true);
		this.environment = returnType.getEnvironment();
	}

	@Override
	public void execute(Referenced ref, List<ScriptValue> valuesGiven) throws ScriptException {
		assert Logs.openNode("Constructor Iterations", "Constructor Expression Iteration");
		ScriptTemplate_Abstract object = this.getEnvironment().getTemplate(this.getReturnType()).createObject(ref, null);
		this.getEnvironment().advanceStack(object, this);
		super.execute(ref, valuesGiven);
		this.setReturnValue(ref, object);
		this.getEnvironment().retreatStack();
		assert Logs.closeNode();
	}

	public ScriptEnvironment getEnvironment() {
		return this.environment;
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Constructor-Function Script-Element");
		super.nodificate();
		assert Logs.closeNode();
	}
}
