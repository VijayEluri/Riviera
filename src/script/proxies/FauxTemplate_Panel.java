package script.proxies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import gui.InterfaceElement;
import gui.InterfaceElement_Panel;
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

public class FauxTemplate_Panel extends FauxTemplate_InterfaceElement implements ScriptConvertible<InterfaceElement>, Nodeable {
	public static final String PANELSTRING = "Panel";

	public FauxTemplate_Panel(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, PANELSTRING), ScriptValueType.createType(env, FauxTemplate_InterfaceElement.INTERFACEELEMENTSTRING), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Panel(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
		this.setElement(new InterfaceElement_Panel(env, null, null));
	}

	// Nodeable and ScriptConvertible implementations
	@Override
	public InterfaceElement convert(ScriptEnvironment env) {
		return this.getElement();
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Panel Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Panel template = (FauxTemplate_Panel) rawTemplate;
		ScriptValue returning;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Panel) this.createObject(ref, template);
			}
		} else if (name.equals("add")) {
			template.getPanel().add(Conversions.getGraphicalElement(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("getTerrestrial")) {
			returning = Conversions.wrapTerrestrial(ref.getEnvironment(), template.getPanel().getTerrestrial());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("setTerrestrial")) {
			template.getPanel().setTerrestrial(Conversions.getTerrestrial(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setRiffDali")) {
			template.getPanel().setRiffDali(Conversions.getTemplate(params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("drawRegion")) {
			template.getPanel().drawRegion(Conversions.getDiscreteRegion(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("fillRegion")) {
			template.getPanel().fillRegion(Conversions.getDiscreteRegion(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("drawTransformedRegion")) {
			template.getPanel().drawTransformedRegion(Conversions.getDiscreteRegion(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("fillTransformedRegion")) {
			template.getPanel().fillTransformedRegion(Conversions.getDiscreteRegion(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("drawString")) {
			template.getPanel().drawString(Conversions.getString(this.getEnvironment(), params.get(0)), Conversions.getColor(this.getEnvironment(), params.get(1)), Conversions.getPoint(this.getEnvironment(), params.get(2)));
			assert Logs.closeNode();
			return null;
		}
		returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public InterfaceElement_Panel getPanel() {
		return (InterfaceElement_Panel) this.getElement();
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing panel faux template");
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

		fxnParams = new ArrayList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_GraphicalElement.GRAPHICALELEMENTSTRING)));
		this.addFauxFunction("add", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		this.addFauxFunction("getTerrestrial", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Terrestrial.TERRESTRIALSTRING), Collections.<ScriptValue> emptyList(), ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Terrestrial.TERRESTRIALSTRING)));
		this.addFauxFunction("setTerrestrial", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_RiffDali.RIFFDALISTRING)));
		this.addFauxFunction("setRiffDali", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING)));
		this.addFauxFunction("drawRegion", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING)));
		this.addFauxFunction("fillRegion", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING)));
		this.addFauxFunction("drawTransformedRegion", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING)));
		this.addFauxFunction("fillTransformedRegion", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Color.COLORSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		this.addFauxFunction("drawString", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);

		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Panel(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Panel Faux Template");
		super.nodificate();
		assert Logs.closeNode();
	}
}
