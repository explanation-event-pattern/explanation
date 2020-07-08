package explanation.algorithm;

import explanation.model.ConstraintLabel;
import explanation.model.ConstraintValue;
import explanation.model.Event;
import explanation.model.Parallel;
import gurobi.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Exact {
  private HashMap<String, Event> timeSeries;
  private HashMap<String, Event> modifySeries;
  private HashMap<ConstraintLabel, ConstraintValue> cons;
  private HashMap<Parallel, ArrayList<String>> corresponding;

  private double max_weight;

  public Exact(HashMap<String, Event> timeSeries,
               HashMap<ConstraintLabel, ConstraintValue> cons,
               HashMap<Parallel, ArrayList<String>> corresponding) {
    super();
    this.timeSeries = timeSeries;
    this.cons = cons;
    this.max_weight = Double.MAX_VALUE;
    this.corresponding = corresponding;
    this.modifySeries = new HashMap<String, Event>();
  }

  public void combine(int tempIndex, GRBVar[] u, GRBVar[] v, GRBModel model,
                      HashMap<String, String> eql, HashMap<Integer, String> ref,
                      HashMap<String, Integer> map, ArrayList<Parallel> par, int i)
      throws GRBException {
    if (i >= par.size()) {
      GRBConstr[] constr = new GRBConstr[eql.size()];
      Iterator eqlIter = eql.entrySet().iterator();
      int j = 0;

      while (eqlIter.hasNext()) {

        Map.Entry entry = (Map.Entry) eqlIter.next();
        String key = (String) entry.getKey();
        String val = (String) entry.getValue();

        GRBLinExpr expr = new GRBLinExpr();
        if (map.containsKey(key) && map.containsKey(val)) {
          expr.addTerm(1.0, u[map.get(key)]);
          expr.addTerm(-1.0, v[map.get(key)]);

          expr.addTerm(1.0, v[map.get(val)]);
          expr.addTerm(-1.0, u[map.get(val)]);

          expr.addConstant(timeSeries.get(val).getfTime());
          expr.addConstant(-timeSeries.get(key).getfTime());
          constr[j++] = model.addConstr(expr, GRB.EQUAL, 0.0,
              "c" + tempIndex++);
        }

      }
      model.update();
      model.write("debug.lp");
      model.optimize();
      System.out.println();

      if (model.get(GRB.IntAttr.Status) != GRB.Status.INFEASIBLE) {
        double tmp_weight = model.get(GRB.DoubleAttr.ObjVal);
        if (tmp_weight < max_weight) {
          modifySeries = new HashMap<String, Event>();
          for (int k = 0; k < u.length; k++) {
            double modify = timeSeries.get(ref.get(k)).getfTime()
                - (u[k].get(GRB.DoubleAttr.X) - v[k].get(GRB.DoubleAttr.X));
            Event tmp = new Event((int) modify);
            modifySeries.put(ref.get(k), tmp);
          }
          max_weight = tmp_weight;

        }
      }

      for (int k = 0; k < j; k++) {
        model.remove(constr[k]);
      }
      model.update();
      if (GRB.DoubleAttr.ObjVal == null) {
        System.out.println();
      }

      return;

    }
    Parallel first = par.get(i);
    ArrayList<String> beh = corresponding.get(first);
    for (String second : beh) {
      eql.put(first.getName(), second);
      combine(tempIndex, u, v, model, eql, ref, map, par, ++i);
      eql.remove(first, second);
      i--;
    }
  }

  public double calculate(boolean isSimple) throws IOException {

    double error = 0.0;
    double dist = 0.0;
    double fault = 0.0;
    int re = 0;

    try {
      GRBEnv env = new GRBEnv("mip1.log");
      GRBModel model = new GRBModel(env);

      GRBVar u[] = new GRBVar[timeSeries.size()];
      GRBVar v[] = new GRBVar[timeSeries.size()];
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      HashMap<Integer, String> ref = new HashMap<Integer, String>();

      Iterator itern = timeSeries.entrySet().iterator();
      int num = 0;
      while (itern.hasNext()) {
        Map.Entry entry = (Map.Entry) itern.next();
        String key = (String) entry.getKey();
        Event tmp = (Event) entry.getValue();
        u[num] = model.addVar(0.0, Integer.MAX_VALUE, 1.0, GRB.INTEGER,
            "u" + num);
        v[num] = model.addVar(0.0, Integer.MAX_VALUE, 1.0, GRB.INTEGER,
            "v" + num);
        map.put(key, num);
        ref.put(num, key);
        num++;
      }

      model.update();

      GRBLinExpr expr = new GRBLinExpr();
      for (int i = 0; i < num; i++) {
        Parallel tmp = new Parallel(ref.get(i));
        if (!corresponding.containsKey(tmp)) {
          expr.addTerm(1.0, u[i]);
          expr.addTerm(1.0, v[i]);
          System.out.print("u" + i + "+");
          System.out.print("v" + i + "+");
        }

      }

      System.out.println();

      model.setObjective(expr, GRB.MINIMIZE);

      Iterator consIter = cons.entrySet().iterator();
      int tempIndex = 0;
      while (consIter.hasNext()) {
        Map.Entry entry = (Map.Entry) consIter.next();
        ConstraintLabel key = (ConstraintLabel) entry.getKey();
        String forward = key.getForwardLabel();
        String backward = key.getBackLabel();
        if (map.containsKey(backward) && map.containsKey(forward)) {
          ConstraintValue val = (ConstraintValue) entry.getValue();
          expr = new GRBLinExpr();

          int tempFirstThingIndex = map.get(forward);
          expr.addTerm(1.0, u[tempFirstThingIndex]);
          expr.addTerm(-1.0, v[tempFirstThingIndex]);

          int tempSecondThingIndex = map.get(backward);
          expr.addTerm(1.0, v[tempSecondThingIndex]);
          expr.addTerm(-1.0, u[tempSecondThingIndex]);

          expr.addConstant(timeSeries.get(backward).getfTime());
          expr.addConstant(-timeSeries.get(forward).getfTime());
          model.addConstr(expr, GRB.LESS_EQUAL, val.getBack(),
              "c" + tempIndex++);
          model.addConstr(expr, GRB.GREATER_EQUAL, val.getForward(),
              "c" + tempIndex++);
        }

      }

      if (!isSimple) {
        if (corresponding.size() > 0) {
          HashMap<String, String> eql = new HashMap<String, String>();
          ArrayList<Parallel> par1 = new ArrayList<Parallel>();
          par1.addAll(corresponding.keySet());
          combine(tempIndex, u, v, model, eql, ref, map, par1, 0);
        } else {
          model.update();
          model.write("debug.lp");
          model.optimize();

          modifySeries = new HashMap<String, Event>();
          for (int k = 0; k < u.length; k++) {
            double modify = timeSeries.get(ref.get(k)).getfTime()
                - (u[k].get(GRB.DoubleAttr.X) - v[k].get(GRB.DoubleAttr.X));
            Event tmp = new Event((int) modify);
            modifySeries.put(ref.get(k), tmp);
          }

        }
      } else {

        model.update();
        model.write("debug.lp");
        model.optimize();

        if (model.get(GRB.IntAttr.Status) != GRB.Status.INFEASIBLE) {
          modifySeries = new HashMap<String, Event>();
          for (int k = 0; k < u.length; k++) {
            double modify = timeSeries.get(ref.get(k)).getfTime()
                - (u[k].get(GRB.DoubleAttr.X) - v[k].get(GRB.DoubleAttr.X));
            Event tmp = new Event((int) modify);
            modifySeries.put(ref.get(k), tmp);
          }
          System.out.println(model.get(GRB.DoubleAttr.ObjVal));
        }

      }

      Iterator result = timeSeries.entrySet().iterator();
      System.out.println(timeSeries.toString());
      System.out.println(modifySeries.toString());
      System.out.println(cons.toString());
      System.out.println();
      while (result.hasNext()) {
        Map.Entry entry = (Map.Entry) result.next();
        String key = (String) entry.getKey();
        Event origin = (Event) entry.getValue();
        if (modifySeries.containsKey(key)) {
          Event later = modifySeries.get(key);
          error += Math.pow((origin.getrTime() - later.getrTime()), 2.0);
          if (origin.getIsAddFault() == 1
              && origin.getfTime() != later.getrTime()) {
            origin.setAddFault(2);
          }
        } else {
          error += Math.pow((origin.getrTime() - origin.getfTime()), 2.0);
        }

      }

      Iterator iterResult = corresponding.keySet().iterator();
      while (iterResult.hasNext()) {
        Parallel tmpPar = (Parallel) iterResult.next();
        if (map.containsKey(tmpPar.getName())
            && modifySeries.containsKey(tmpPar.getName())) {
          error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
              - modifySeries.get(tmpPar.getName()).getrTime()), 2.0);
          re++;
        }
      }

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out
          .println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
    }

    return Math.sqrt(error / (timeSeries.size() - re));

  }

  public double calculate(boolean isSimple, String path) throws IOException {

    double error = 0.0;
    double dist = 0.0;
    double fault = 0.0;
    int re = 0;

    try {
      GRBEnv env = new GRBEnv("mip1.log");
      GRBModel model = new GRBModel(env);

      // Create variables
      GRBVar u[] = new GRBVar[timeSeries.size()];
      GRBVar v[] = new GRBVar[timeSeries.size()];
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      HashMap<Integer, String> ref = new HashMap<Integer, String>();

      @SuppressWarnings("rawtypes")
      Iterator itern = timeSeries.entrySet().iterator();
      int num = 0;
      while (itern.hasNext()) {
        @SuppressWarnings("rawtypes")
        Map.Entry entry = (Map.Entry) itern.next();
        String key = (String) entry.getKey();
        Event tmp = (Event) entry.getValue();
        u[num] = model.addVar(0.0, Integer.MAX_VALUE, 1.0, GRB.INTEGER,
            "u" + num);
        v[num] = model.addVar(0.0, Integer.MAX_VALUE, 1.0, GRB.INTEGER,
            "v" + num);
        map.put(key, num);
        ref.put(num, key);
        num++;
      }

      model.update();

      GRBLinExpr expr = new GRBLinExpr();
      for (int i = 0; i < num; i++) {
        Parallel tmp = new Parallel(ref.get(i));
        if (!corresponding.containsKey(tmp)) {
          int index = map.get(tmp.getName());
          expr.addTerm(1.0, u[index]);
          expr.addTerm(1.0, v[index]);
          System.out.print("u" + i + "+");
          System.out.print("v" + i + "+");
        }

      }

      System.out.println();

      model.setObjective(expr, GRB.MINIMIZE);

      Iterator consIter = cons.entrySet().iterator();
      int tempIndex = 0;
      while (consIter.hasNext()) {
        Map.Entry entry = (Map.Entry) consIter.next();
        ConstraintLabel key = (ConstraintLabel) entry.getKey();
        String forward = key.getForwardLabel();
        String backward = key.getBackLabel();
        if (map.containsKey(backward) && map.containsKey(forward)) {
          ConstraintValue val = (ConstraintValue) entry.getValue();
          expr = new GRBLinExpr();

          int tempFirstThingIndex = map.get(forward);
          expr.addTerm(1.0, u[tempFirstThingIndex]);
          expr.addTerm(-1.0, v[tempFirstThingIndex]);

          int tempSecondThingIndex = map.get(backward);
          expr.addTerm(1.0, v[tempSecondThingIndex]);
          expr.addTerm(-1.0, u[tempSecondThingIndex]);

          expr.addConstant(timeSeries.get(backward).getfTime());
          expr.addConstant(-timeSeries.get(forward).getfTime());
          model.addConstr(expr, GRB.LESS_EQUAL, val.getBack(),
              "c" + tempIndex++);
          model.addConstr(expr, GRB.GREATER_EQUAL, val.getForward(),
              "c" + tempIndex++);
        }

      }

      if (!isSimple) {
        if (corresponding.size() > 0) {
          HashMap<String, String> eql = new HashMap<String, String>();
          ArrayList<Parallel> par1 = new ArrayList<Parallel>();
          par1.addAll(corresponding.keySet());
          combine(tempIndex, u, v, model, eql, ref, map, par1, 0);
        } else {
          model.update();
          model.write("debug.lp");
          model.optimize();

          modifySeries = new HashMap<String, Event>();
          for (int k = 0; k < u.length; k++) {
            double modify = timeSeries.get(ref.get(k)).getfTime()
                - (u[k].get(GRB.DoubleAttr.X) - v[k].get(GRB.DoubleAttr.X));
            Event tmp = new Event((int) modify);
            modifySeries.put(ref.get(k), tmp);
          }

        }
      } else {

        model.update();
        model.write("debug.lp");
        model.optimize();

        if (model.get(GRB.IntAttr.Status) != GRB.Status.INFEASIBLE) {
          modifySeries = new HashMap<String, Event>();
          for (int k = 0; k < u.length; k++) {
            double modify = timeSeries.get(ref.get(k)).getfTime()
                - (u[k].get(GRB.DoubleAttr.X) - v[k].get(GRB.DoubleAttr.X));
            Event tmp = new Event((int) modify);
            modifySeries.put(ref.get(k), tmp);
          }
          System.out.println(model.get(GRB.DoubleAttr.ObjVal));
        }

      }

      Iterator result = timeSeries.entrySet().iterator();
      System.out.println(timeSeries.toString());
      System.out.println(modifySeries.toString());
      System.out.println(cons.toString());
      System.out.println();

      File file = new File(path);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file);
      BufferedWriter bw = new BufferedWriter(fw);

      while (result.hasNext()) {
        Map.Entry entry = (Map.Entry) result.next();
        String key = (String) entry.getKey();
        Event origin = (Event) entry.getValue();
        if (modifySeries.containsKey(key)) {
          Event later = modifySeries.get(key);
          error += Math.pow((origin.getrTime() - later.getrTime()), 2.0);
          if (origin.getIsAddFault() == 1
              && origin.getfTime() != later.getrTime()) {
            origin.setAddFault(2);
          }
          bw.write(key + "," + String.valueOf(later.getrTime()) + "\n");
        } else {
          error += Math.pow((origin.getrTime() - origin.getfTime()), 2.0);
          bw.write(key + "," + String.valueOf(origin.getfTime()) + "\n");
        }

      }

      bw.close();
      fw.close();

      Iterator iterResult = corresponding.keySet().iterator();
      while (iterResult.hasNext()) {
        Parallel tmpPar = (Parallel) iterResult.next();
        if (map.containsKey(tmpPar.getName())) {
          error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
              - modifySeries.get(tmpPar.getName()).getrTime()), 2.0);
          re++;
        }
      }

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out
          .println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
    }

    return Math.sqrt(error / (timeSeries.size() - re));

  }

}