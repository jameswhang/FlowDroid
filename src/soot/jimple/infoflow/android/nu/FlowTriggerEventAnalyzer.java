package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.util.MultiMap;

public class FlowTriggerEventAnalyzer {
	
	private String apkFileLocation;
	private MultiMap<ResultSinkInfo, ResultSourceInfo> infoflowResultMap;
	private InfoflowResults infoflowResult;
	private StaticValueService staticValueService;
	private Set<SootMethod> triggerMethods;
	private ParamAnalyzer paramAnalyzer;
	
	public FlowTriggerEventAnalyzer(InfoflowResults results, String apkFileLocation) {
		this.infoflowResult = results;
		this.infoflowResultMap = results.getResults();
		this.apkFileLocation = apkFileLocation;
		this.staticValueService = new StaticValueService();
		this.triggerMethods = new HashSet<SootMethod>();
		this.paramAnalyzer = new ParamAnalyzer();
	}
	
	public void RunCallGraphAnalysis() {
		CallGraph cgraph = Scene.v().getCallGraph();
		for (ResultSinkInfo sink : this.infoflowResultMap.keySet()) {
			Set<ResultSourceInfo> sources = this.infoflowResultMap.get(sink);
			for (ResultSourceInfo source : sources) {
				boolean srcAdded = false;
				if (source.getSource().containsInvokeExpr()) { // is a call to something
					//System.out.println("[NUTEXT] Looking at source: " + source.getSource().getInvokeExpr().getMethod().getSignature());
					SootMethod invokedMethod = source.getSource().getInvokeExpr().getMethod();
					ValueBox invokedValue = source.getSource().getInvokeExprBox();
					Iterator<Edge> edges = cgraph.edgesInto(invokedMethod);
					
					if (edges.hasNext()) {
						SootMethod triggerMethodFromSource = findUITriggerMethodFromSource(cgraph, invokedMethod);
						
						if (triggerMethodFromSource != null) {
							this.triggerMethods.add(triggerMethodFromSource);
							if (!srcAdded) {
								this.triggerMethods.add(invokedMethod);
								srcAdded = true;
							}
							System.out.println("[NUTEXT] Found source trigger: "+source+" tiggered by " + triggerMethodFromSource.getSignature() + " with argument: " + invokedValue.toString());
						}
						ArrayList<SootMethod> triggerMethodBetweenSourceAndSink = findTriggerMethodsFromSinkToSource(cgraph, invokedMethod, sink.getSink().getInvokeExpr().getMethod());
						if (!triggerMethodBetweenSourceAndSink.isEmpty() && !srcAdded) {
							this.triggerMethods.add(invokedMethod);
						}
						for (SootMethod m : triggerMethodBetweenSourceAndSink) {
							this.triggerMethods.add(m);
							System.out.println("[NUTEXT] Found source trigger: "+source+" tiggered by " + m.getSignature());
						}
					}
				}
			}
		}
	}
	// Manual check for non constant method triggers.
	/*
	public void BacktrackMethodCall() {
		CallGraph cgraph = Scene.v().getCallGraph();
		for ()
	}
	*/
	
	public void RunCFGAnalysis() {
		for (SootMethod triggerMethod : this.triggerMethods) {
			if (!triggerMethod.hasActiveBody()) {
				continue;
			}
			UnitGraph g = new ExceptionalUnitGraph(triggerMethod.getActiveBody());
			Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
			List<Unit> units = orderer.newList(g, false);
			
			HashMap<Value, InvokeExpr> localInvokeDefs = new HashMap<Value, InvokeExpr>(); // map of local variable => method definition within this method
			HashMap<Value, Value> localAssignDefs = new HashMap<Value, Value>(); // map of local variable => method definition within this method

			for (Unit u : units) {
				Stmt s = (Stmt)u;
				List<ValueBox> defs = s.getDefBoxes();
				if (s.containsInvokeExpr()) {
					for (ValueBox defbox : defs) {
						localInvokeDefs.put(defbox.getValue(), s.getInvokeExpr());
					}
				} else if (s instanceof AssignStmt) {
					AssignStmt as = (AssignStmt)s;
					localAssignDefs.put(as.getLeftOp(), as.getRightOp());
				}
			}
			
			for (Unit u : units) {
				Stmt s = (Stmt)u;
				if (s.containsInvokeExpr()) {
					InvokeExpr e = s.getInvokeExpr();
					SootMethod m = e.getMethod();
					if (m.getName().equals("findViewById")) {
						this.paramAnalyzer.getParameterType(m);
						System.out.println("[NUTEXT] findViewById trigger method signature: " + triggerMethod.getSignature());
						if (this.paramAnalyzer.hasConstantArg(e)) {
							System.out.println("[NUTEXT] findViewById has constant args");
						} else {
							System.out.println("[NUTEXT] findViewById has non-constant args");
							List<Value> args = this.paramAnalyzer.getArguments(e);
							for (Value arg : args) {
								analyzeNonConstantVarDefinition(arg, localInvokeDefs.get(arg), localInvokeDefs, localAssignDefs);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean isThisStmt(Value stmt) {
		// also super jank. Probably need to fix it.
		return stmt.toString().contains("this$0");
	}
	
	public void analyzeNonConstantVarDefinition(Value arg, InvokeExpr e, HashMap<Value, InvokeExpr> localInvokeDefs, HashMap<Value, Value> localAssignDefs) {
		System.out.println("[NUTEXT] Definition for " + arg.toString() + ": " + e.toString() + ", with args of: " + e.getArgs().toString());
		for (Value param : e.getArgs()) {
			System.out.println("[NUTEXT] param: " + param.toString());
			if (localInvokeDefs.containsKey(param)) {
				System.out.println("[NUTEXT] Arg parameter " + param.toString() + " has definition of: " + localInvokeDefs.get(param).toString());
			} else if (localAssignDefs.containsKey(param)) {
				if (isThisStmt(localAssignDefs.get(param))) continue;
				else {
					System.out.println("[NUTEXT] Parameter for " + e.toString() + " is not constant.");
				}
				System.out.println("[NUTEXT] Arg parameter " + param.toString() + " has definition of: " + localAssignDefs.get(param).toString());
			} else {
				if (this.paramAnalyzer.isConstant(param)) {
					System.out.println("[NUTEXT] Found a constant parameter to definition for non-constant parameter: " + param.toString());
				} else {
					System.out.println("[NUTEXT] WARNING: Could not find local definition for a non-constant parameter to a non-constant parameter to findViewById: " + param.toString());
				}
			}
		}
		
	}
	
	
	public ArrayList<SootMethod> findTriggerMethodsFromSinkToSource(CallGraph cgraph, SootMethod sourceMethod, SootMethod sinkMethod) {
		/*
		 * Given a source and sink methods, tracks the call graph to find any triggering methods in the call flow between the source and sink methods
		 * @param cgraph CallGraph object of the Android apk
		 * @param sourceMethod SootMethod object of the source method
		 * @param sinkMethod SootMethod object of the sink method
		 * @return triggerMethod SootMethod object of the method that triggers
		 */
		ArrayList<SootMethod> triggerMethods = new ArrayList<SootMethod>();
		HashSet<String> UIActionsSet = this.staticValueService.getUIEventActionsSet();
		Set<String> visitedNodes = new HashSet<String>();
		
		LinkedList<SootMethod> queue = new LinkedList<SootMethod>();
		queue.add(sinkMethod);
		while(!queue.isEmpty()) {
			SootMethod m = queue.removeFirst();
			if (m.getSignature() == sourceMethod.getSignature()) {
				break;
			}
			if (UIActionsSet.contains(m.getName())) {
				triggerMethods.add(m);
			}
			visitedNodes.add(m.getSignature());
			Iterator<Edge> edges = cgraph.edgesInto(m);
			while(edges.hasNext()) {
				Edge e = edges.next();
				SootMethod pred = e.getSrc().method();
				if (pred != null) {
					if (!visitedNodes.contains(pred.getSignature())) {
						queue.addLast(pred);
					}
				}
			}
		}	
		return triggerMethods;		
	}
	
	public SootMethod findUITriggerMethodFromSource(CallGraph cgraph, SootMethod method) {
		/*
		 * Given a source method, tracks the call graph to find any triggering methods that use UI actions in Android framework like "onClick()"
		 * @param cgraph CallGraph object of the Android apk
		 * @param method SootMethod object of the source method
		 * @return triggerMethod SootMethod object of the method that triggers
		 */
		//System.out.println("findTrigger called for method: " + method.getName());
		LinkedList<SootMethod> queue = new LinkedList<SootMethod>();
		queue.add(method);
		
		HashSet<String> UIActionsSet = this.staticValueService.getUIEventActionsSet();
		Set<String> visitedNodes = new HashSet<String>();
		
		while(!queue.isEmpty()) {
			SootMethod m = queue.removeFirst();
			//System.out.println("[NUTEXT] Found UI trigger of " + m.getName() + " while looking at method: " + method.getName());
			if (UIActionsSet.contains(m.getName())) {
				return m;
			}
			
			visitedNodes.add(m.getSignature());
			Iterator<Edge> edges = cgraph.edgesInto(m);
			while(edges.hasNext()) {
				Edge e = edges.next();
				SootMethod pred = e.getSrc().method();
				if (pred != null) {
					if (!visitedNodes.contains(pred.getSignature())) {
						queue.addLast(pred);
					}
				}
			}
		}
		return null;
	}

}
