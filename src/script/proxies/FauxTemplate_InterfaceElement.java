package script.proxies;

import java.util.LinkedList;
import java.util.List;

import gui.InterfaceElement;
import gui.style.Stylesheet;
import inspect.Nodeable;
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

public class FauxTemplate_InterfaceElement extends FauxTemplate implements Nodeable, ScriptConvertible<InterfaceElement> {
	public static final String INTERFACEELEMENTSTRING = "InterfaceElement";
	private InterfaceElement element;

	public FauxTemplate_InterfaceElement(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, INTERFACEELEMENTSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), true);
	}

	public FauxTemplate_InterfaceElement(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
	}

	public FauxTemplate_InterfaceElement(ScriptEnvironment env, ScriptValueType type, ScriptValueType extended, List<ScriptValueType> implemented, boolean isAbstract) {
		super(env, type, extended, implemented, isAbstract);
	}

	// Nodeable implementation
	@Override
	public InterfaceElement convert(ScriptEnvironment env) {
		return this.getElement();
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Interface Element Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_InterfaceElement template = (FauxTemplate_InterfaceElement) rawTemplate;
		ScriptValue returning;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		ScriptValue value;
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_InterfaceElement) this.createObject(ref, template);
			}
			switch (params.size()) {
			// Intentionally out of order to allow for case 2 to run case 1's code.
			case 2:
				value = params.get(1);
				template.getElement().setClassStylesheet((Stylesheet) value.getValue());
			case 1:
				value = params.get(0);
				template.getElement().setUniqueStylesheet((Stylesheet) value.getValue());
				break;
			}
		} else if (name.equals("getUniqueStylesheet")) {
			returning = Conversions.wrapStylesheet(this.getEnvironment(), template.getElement().getUniqueStylesheet());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("getClassStylesheet")) {
			returning = Conversions.wrapStylesheet(this.getEnvironment(), template.getElement().getClassStylesheet());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("setUniqueStylesheet")) {
			template.getElement().setUniqueStylesheet(Conversions.getStylesheet(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setClassStylesheet")) {
			template.getElement().setClassStylesheet(Conversions.getStylesheet(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		}
		params.clear();
		returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public InterfaceElement getElement() {
		return this.element;
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing interface element faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING)));
		this.addConstructor(this.getType(), fxnParams);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING)));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new LinkedList<ScriptValue>();
		this.addFauxFunction("getUniqueStylesheet", ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING), new LinkedList<ScriptValue>(), ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getClassStylesheet", ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING), new LinkedList<ScriptValue>(), ScriptKeywordType.PUBLIC, false, false);
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), Stylesheet.STYLESHEETSTRING)));
		this.addFauxFunction("setUniqueStylesheet", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setClassStylesheet", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_InterfaceElement(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Interface Element Faux Template");
		super.nodificate();
		if (this.getElement() == null) {
			assert Logs.addNode("Interface Element: null");
		} else {
			assert Logs.addNode(this.getElement());
		}
		assert Logs.closeNode();
	}

	public void setElement(InterfaceElement element) {
		assert Logs.openNode("Interface Element Faux Template Changes", "Changing Interface Element");
		assert Logs.addNode(this);
		assert Logs.addNode(element);
		this.element = element;
		assert Logs.closeNode();
	}
}
