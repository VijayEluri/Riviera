package script.proxies;

import java.util.LinkedList;
import java.util.List;

import Ace;
import Archetype;
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

public class FauxTemplate_Archetype extends FauxTemplate implements ScriptConvertible<Archetype>, Nodeable {
	public static final String ARCHETYPESTRING = "Archetype";
	private Archetype archetype;

	public FauxTemplate_Archetype(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, ARCHETYPESTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Archetype(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
	}

	// Nodeable and ScriptConvertible interfaces
	@Override
	public Archetype convert(ScriptEnvironment env) {
		return this.archetype;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing archetype faux template function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Archetype template = (FauxTemplate_Archetype) rawTemplate;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Archetype) this.createObject(ref, template);
			}
			template.setArchetype(new Archetype(Conversions.getString(this.getEnvironment(), params.get(0))));
			params.clear();
		} else if (name.equals("addParent")) {
			template.getArchetype().addParent(Conversions.getAce(this.getEnvironment(), params.get(0)));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("getName")) {
			ScriptValue returning = Conversions.wrapString(this.getEnvironment(), template.getArchetype().getName());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("getParents")) {
			List<ScriptValue> parents = new LinkedList<ScriptValue>();
			for (Ace parent : template.getArchetype().getParents()) {
				parents.add(Conversions.wrapAce(this.getEnvironment(), parent));
			}
			// This getRiffList line might not be necessary; it used to have a variable
			// assigned to it, but it was never used.
			Conversions.wrapList(this.getEnvironment(), parents);
			assert Logs.closeNode();
			return null;
		}
		ScriptValue returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public Archetype getArchetype() {
		return this.archetype;
	}

	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing archetype faux template");
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Ace.ACESTRING)));
		this.addFauxFunction("addParent", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getName", ScriptValueType.STRING, new LinkedList<ScriptValue>(), ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getParents", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_List.LISTSTRING), new LinkedList<ScriptValue>(), ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Archetype(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Archetype Faux Script-Element");
		super.nodificate();
		assert Logs.addNode(this.archetype);
		assert Logs.closeNode();
	}

	public void setArchetype(Archetype archetype) {
		this.archetype = archetype;
	}
}
