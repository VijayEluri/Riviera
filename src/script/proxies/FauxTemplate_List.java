package script.proxies;

import java.util.LinkedList;
import java.util.List;

import inspect.Nodeable;
import logging.CommonString;
import logging.Logs;
import script.Conversions;
import script.ScriptConvertible;
import script.ScriptEnvironment;
import script.exceptions.ScriptException;
import script.parsing.Referenced;
import script.parsing.ScriptKeywordType;
import script.values.RiffScriptFunction;
import script.values.ScriptTemplate;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Faux;

public class FauxTemplate_List extends FauxTemplate implements ScriptConvertible<List<ScriptValue>>, Nodeable {
	public static final String LISTSTRING = "List";
	private List<ScriptValue> list = new LinkedList<ScriptValue>();

	public FauxTemplate_List(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, LISTSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_List(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
	}

	// Convertible and Nodeable implementations
	@Override
	public List<ScriptValue> convert(ScriptEnvironment env) {
		return this.list;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing List Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_List template = (FauxTemplate_List) rawTemplate;
		ScriptValue returning;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_List) this.createObject(ref, template);
			}
			params.clear();
		} else if (name.equals("add")) {
			template.getList().add(params.get(0).getValue());
			assert Logs.closeNode();
			return null;
		} else if (name.equals("addAll")) {
			template.getList().addAll(Conversions.getList(params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("get")) {
			assert Logs.closeNode();
			return template.getList().get(Conversions.getInteger(this.getEnvironment(), params.get(0)));
		} else if (name.equals("size")) {
			assert Logs.closeNode();
			return Conversions.wrapInt(this.getEnvironment(), template.getList().size());
		}
		returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public List<ScriptValue> getList() {
		return this.list;
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing list faux template");
		this.addConstructor(this.getType());
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		List<ScriptValue> fxnParams = FauxTemplate.createEmptyParamList();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.getObjectType(this.getEnvironment())));
		this.addFauxFunction("add", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = FauxTemplate.createEmptyParamList();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), this.getType()));
		this.addFauxFunction("addAll", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = FauxTemplate.createEmptyParamList();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.INT));
		this.addFauxFunction("get", ScriptValueType.getObjectType(this.getEnvironment()), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = FauxTemplate.createEmptyParamList();
		this.addFauxFunction("size", ScriptValueType.INT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_List(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		if (this.list == null) {
			assert Logs.openNode("List Faux Template (0 element(s))");
		} else {
			assert Logs.openNode("List Faux Template (" + this.list.size() + " element(s))");
		}
		super.nodificate();
		assert Logs.addSnapNode(CommonString.ELEMENTS, this.list);
		assert Logs.closeNode();
	}

	public void setList(List<ScriptValue> list) {
		this.list = list;
	}
}
