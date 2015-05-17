package script.proxies;

import java.util.LinkedList;
import java.util.List;

import gui.style.Stylesheets;
import inspect.Nodeable;
import logging.Logs;
import script.Conversions;
import script.ScriptEnvironment;
import script.exceptions.ScriptException;
import script.operations.ScriptExecutable_CallFunction;
import script.parsing.Referenced;
import script.parsing.ScriptKeywordType;
import script.values.RiffScriptFunction;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Faux;

public class FauxTemplate_RiffDali extends FauxTemplate implements Nodeable {
	public static final String RIFFDALISTRING = "RiffDali";

	public FauxTemplate_RiffDali(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, RIFFDALISTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), true);
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing RiffDali Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		ScriptValue returning = null;
		assert Logs.addSnapNode("Template provided", rawTemplate);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name.equals("parseColor")) {
			returning = Conversions.wrapColor(this.getEnvironment(), Stylesheets.getColor(Conversions.getString(this.getEnvironment(), params.get(0))));
		} else if (name.equals("paintPanel")) {
			List<ScriptValue> list = Conversions.getList(params.get(1));
			List<ScriptValue> paramList = new LinkedList<ScriptValue>();
			for (ScriptValue value : list) {
				paramList.clear();
				paramList.add(value);
				ScriptExecutable_CallFunction.callFunction(this.getEnvironment(), ref, params.get(0), "drawRegion", paramList);
			}
		} else {
			returning = this.getExtendedFauxClass().execute(ref, name, params, rawTemplate);
		}
		assert Logs.closeNode();
		return returning;
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing RiffDali faux template");
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		List<ScriptValue> fxnParams = FauxTemplate.createEmptyParamList();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addFauxFunction("parseColor", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Color.COLORSTRING), fxnParams, ScriptKeywordType.PUBLIC, false, true);
		fxnParams = FauxTemplate.createEmptyParamList();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Panel.PANELSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_List.LISTSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_List.LISTSTRING)));
		this.addFauxFunction("paintPanel", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("RiffDali Faux Template");
		super.nodificate();
		assert Logs.closeNode();
	}
}
