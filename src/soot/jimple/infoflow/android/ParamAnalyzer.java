package soot.jimple.infoflow.android;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import nu.analysis.DefAnalysisMap;
//import nu.analysis.IntraProcedureAnalysis;
//import nu.analysis.values.CallRetValue;
//import nu.analysis.values.ConstantValue;
//import nu.analysis.values.InstanceFieldValue;
//import nu.analysis.values.RightValue;

import com.sun.xml.internal.bind.v2.model.core.ID;

import soot.Immediate;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.ConcreteRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.Expr;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResConfig;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResPackage;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResType;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.shimple.ShimpleExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.util.queue.QueueReader;

public class ParamAnalyzer {
	
	
	private String[] EventNames = {
			"onClick", "onLongClick", "onFocusChange", "onKey", 
			"onTouch", "onKeyDown", "onKeyUp", "onTrackballEvent",
			"onTouchEvent", "onFocusChanged"};
	
	private String[] SetEventListenerMethodNames = {
			"setOnDragListener", "setOnClickListener", "setOnApplyWindowInsetsListener",
			"setOnCreateContextMenuListener", "setOnEditorActionListener", "setOnFocusChangeListener",
			"setOnGenericMotionListener", "setOnHoverListener", "setOnKeyListener",
			"setOnLongClickListener", "setOnSystemUiVisibilityChangeListener", "setOnTouchListener"};
	

	//TODO: 1. Modify the following method to trace back to the last method where the parameter comes from
	/**
	 * Find the trigger 
	 * 
	 * @return The results of the data flow analysis
	 */
	private SootMethod findPrecessorTrigger(SootMethod method){
		SootMethod triggerMethod = null;
		
		return triggerMethod;
	}
	
	// Can trace back to the last method by using returnType, 11/10/2016
	
	/**
	 * Read in the Android Methods from the Method List and retrieve the parameters
	 * 
	 * @return The map of the parameter types associated with methods
	 */
	public Map<String,String> processParameters(SootMethod method) {
		Map<String,String> paramTypes = new HashMap<String,String>();
		if(method.getName().equals("findViewById")){
			List<Type> params = method.getParameterTypes();
			for(Type a : params){
				if(a.toString().equals("String")||a.toString().equals("int")||a.toString().equals("java.lang.String")){
					paramTypes.put(method.getName(), a.toString());
					System.out.println("Exists constant args for findViewById(): "+a);
				}
				else{
					paramTypes.put(method.getName(), a.toString());
					System.out.println("Exists non-constant args for findViewById(): "+a);
				}
			}
		}
		else{
			System.out.println(method.getName()+"is not findViewById().");
		}

		return null;
	}
}

//NOTE: Please feel free to contact Xiang Pan and Xuechao Du if there's any problem with 
