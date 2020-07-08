package explanation.algorithm;

import explanation.model.ConstraintLabel;
import explanation.model.ConstraintValue;
import explanation.model.Event;
import explanation.model.Parallel;
import explanation.util.SortByTime;
import gurobi.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class Approximate {
  private HashMap<String, Event> timeSeries;
  private HashMap<String, Event> modifySeries;
  private HashMap<ConstraintLabel, ConstraintValue> cons;
  private HashMap<Parallel, ArrayList<String>> corresponding;
  private int bindingNum;

  private double max_weight;

  public Approximate(int bindingNum, HashMap<String, Event> timeSeries,
                     HashMap<ConstraintLabel, ConstraintValue> cons,
                     HashMap<Parallel, ArrayList<String>> corresponding) {
    super();
    this.bindingNum = bindingNum;
    this.timeSeries = timeSeries;
    this.cons = cons;
    this.max_weight = Double.MAX_VALUE;
    this.corresponding = corresponding;
    this.modifySeries = new HashMap<String, Event>();
  }


  private ArrayList<String> sort(ArrayList<String> list, int num,
                                 boolean direction) {
    ArrayList<String> result = new ArrayList<String>();
    ArrayList<Event> sortList = new ArrayList<Event>();
    for (String tmp : list) {
      Event event = timeSeries.get(tmp);
      event.setParallel(tmp);
      sortList.add(event);
    }
    Collections.sort(sortList, new SortByTime());
    if (direction) {
      for (int i = 0; i < num; i++) {
        if (sortList.size() > i) {
          result.add(sortList.get(i).getParallel());
        }

      }
    } else {
      for (int i = sortList.size() - 1; i > sortList.size() - num - 1
          && i > -1; i--) {
        result.add(sortList.get(i).getParallel());
      }
    }

    return result;
  }


  private void aproCombine(HashMap<Parallel, ArrayList<String>> cor,
                           int tempIndex, GRBVar[] u, GRBVar[] v, GRBModel model,
                           HashMap<String, String> eql, HashMap<Integer, String> ref,
                           HashMap<String, Integer> map, ArrayList<Parallel> par, int i)
      throws GRBException {
    if (i >= par.size()) {
      GRBConstr[] constr = new GRBConstr[eql.size()];
      Iterator eqlIter = eql.entrySet().iterator();
      int j = 0;

      while (eqlIter.hasNext()) {

        Entry entry = (Entry) eqlIter.next();
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

      bindingNum--;
      return;

    }
    Parallel first = par.get(i);
    ArrayList<String> beh = cor.get(first);
    for (String second : beh) {
      if (bindingNum <= 0) {
        return;
      }
      eql.put(first.getName(), second);
      aproCombine(cor, tempIndex, u, v, model, eql, ref, map, par, ++i);
      eql.remove(first, second);
      i--;
    }
  }

  public double calculate() throws IOException {

    double error = 0.0;

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
        Entry entry = (Entry) itern.next();
        String key = (String) entry.getKey();
        Event tmp = (Event) entry.getValue();
        u[num] = model.addVar(0.0, Integer.MAX_VALUE, 0.0, GRB.INTEGER,
            "u" + num);
        v[num] = model.addVar(0.0, Integer.MAX_VALUE, 0.0, GRB.INTEGER,
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
        } else {
          System.out.println();
        }
      }

      System.out.println();

      model.setObjective(expr, GRB.MINIMIZE);

      Iterator consIter = cons.entrySet().iterator();
      int tempIndex = 0;
      while (consIter.hasNext()) {
        Entry entry = (Entry) consIter.next();
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

          if (tempFirstThingIndex == 5 && tempSecondThingIndex == 2) {
            System.out.println();
          }

          expr.addConstant(timeSeries.get(backward).getfTime());
          expr.addConstant(-timeSeries.get(forward).getfTime());
          model.addConstr(expr, GRB.LESS_EQUAL, val.getBack(),
              "c" + tempIndex++);
          model.addConstr(expr, GRB.GREATER_EQUAL, val.getForward(),
              "c" + tempIndex++);
        }

      }

      int sortNum = 0;

      for (int i = 0; i < timeSeries.size(); i++) {
        if (i * i >= bindingNum) {
          sortNum = i;
          break;
        }
      }

      Iterator fd = corresponding.entrySet().iterator();
      HashMap<Parallel, ArrayList<String>> cor = new HashMap<Parallel, ArrayList<String>>();
      while (fd.hasNext()) {
        Entry<Parallel, ArrayList<String>> tmp = (Entry<Parallel, ArrayList<String>>) fd
            .next();
        Parallel key = tmp.getKey();
        ArrayList<String> val = tmp.getValue();
        ArrayList<String> sortVal = new ArrayList<String>();
        if (key.getMark() == 1) {
          sortVal = sort(val, sortNum, true);

        } else if (key.getMark() == 2) {
          sortVal = sort(val, sortNum, false);
        }

        cor.put(key, sortVal);
      }

      HashMap<String, String> eql = new HashMap<String, String>();
      ArrayList<Parallel> par1 = new ArrayList<Parallel>();
      par1.addAll(cor.keySet());
      aproCombine(cor, tempIndex, u, v, model, eql, ref, map, par1, 0);

      Iterator result = timeSeries.entrySet().iterator();
      System.out.println(timeSeries.toString());
      System.out.println(modifySeries.toString());
      System.out.println(cons.toString());
      System.out.println();
      while (result.hasNext()) {
        Entry entry = (Entry) result.next();
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
          Event later = modifySeries.get(key);
          error += Math.pow((origin.getrTime() - origin.getfTime()), 2.0);
        }

      }
      Iterator iterResult = corresponding.keySet().iterator();
      while (iterResult.hasNext()) {
        Parallel tmpPar = (Parallel) iterResult.next();
        if (map.containsKey(tmpPar.getName())) {
          if (modifySeries.containsKey(tmpPar.getName())) {
            error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
                - modifySeries.get(tmpPar.getName()).getrTime()), 2.0);
            re++;
          } else {
            error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
                - timeSeries.get(tmpPar.getName()).getfTime()), 2.0);
            re++;
          }

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

  public double calculate(String path) throws IOException {

    double error = 0.0;

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
        Entry entry = (Entry) itern.next();
        String key = (String) entry.getKey();
        Event tmp = (Event) entry.getValue();
        u[num] = model.addVar(0.0, Integer.MAX_VALUE, 0.0, GRB.INTEGER,
            "u" + num);
        v[num] = model.addVar(0.0, Integer.MAX_VALUE, 0.0, GRB.INTEGER,
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
        } else {
          System.out.println();
        }
      }

      System.out.println();

      model.setObjective(expr, GRB.MINIMIZE);

      Iterator consIter = cons.entrySet().iterator();
      int tempIndex = 0;
      while (consIter.hasNext()) {
        Entry entry = (Entry) consIter.next();
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

          if (tempFirstThingIndex == 5 && tempSecondThingIndex == 2) {
            System.out.println();
          }

          expr.addConstant(timeSeries.get(backward).getfTime());
          expr.addConstant(-timeSeries.get(forward).getfTime());
          model.addConstr(expr, GRB.LESS_EQUAL, val.getBack(),
              "c" + tempIndex++);
          model.addConstr(expr, GRB.GREATER_EQUAL, val.getForward(),
              "c" + tempIndex++);
        }

      }

      int sortNum = 0;

      for (int i = 0; i < timeSeries.size(); i++) {
        if (i * i >= bindingNum) {
          sortNum = i;
          break;
        }
      }

      Iterator fd = corresponding.entrySet().iterator();
      HashMap<Parallel, ArrayList<String>> cor = new HashMap<Parallel, ArrayList<String>>();
      while (fd.hasNext()) {
        Entry<Parallel, ArrayList<String>> tmp = (Entry<Parallel, ArrayList<String>>) fd
            .next();
        Parallel key = tmp.getKey();
        ArrayList<String> val = tmp.getValue();
        ArrayList<String> sortVal = new ArrayList<String>();
        if (key.getMark() == 1) {
          sortVal = sort(val, sortNum, true);

        } else if (key.getMark() == 2) {
          sortVal = sort(val, sortNum, false);
        }

        cor.put(key, sortVal);
      }

      HashMap<String, String> eql = new HashMap<String, String>();
      ArrayList<Parallel> par1 = new ArrayList<Parallel>();
      par1.addAll(cor.keySet());
      aproCombine(cor, tempIndex, u, v, model, eql, ref, map, par1, 0);

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
        Entry entry = (Entry) result.next();
        String key = (String) entry.getKey();
        Event origin = (Event) entry.getValue();

        if (modifySeries.containsKey(key)) {
          Event later = modifySeries.get(key);
          error += Math.pow((origin.getrTime() - later.getrTime()), 2.0);
          bw.write(key + "," + String.valueOf(later.getrTime()) + "\n");
          if (origin.getIsAddFault() == 1
              && origin.getfTime() != later.getrTime()) {
            origin.setAddFault(2);
          }
        } else {
          error += Math.pow((origin.getrTime() - origin.getfTime()), 2.0);
          bw.write(key + "," + String.valueOf(origin.getrTime()) + "\n");
        }

      }

      bw.close();
      fw.close();

      Iterator iterResult = corresponding.keySet().iterator();
      while (iterResult.hasNext()) {
        Parallel tmpPar = (Parallel) iterResult.next();
        if (map.containsKey(tmpPar.getName())) {
          if (modifySeries.containsKey(tmpPar.getName())) {
            error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
                - modifySeries.get(tmpPar.getName()).getrTime()), 2.0);
            re++;
          } else {
            error -= Math.pow((timeSeries.get(tmpPar.getName()).getrTime()
                - timeSeries.get(tmpPar.getName()).getfTime()), 2.0);
            re++;
          }

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