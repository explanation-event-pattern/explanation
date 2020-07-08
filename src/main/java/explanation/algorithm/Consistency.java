package explanation.algorithm;

import explanation.model.ConstraintLabel;
import explanation.model.ConstraintValue;
import explanation.model.Parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

public class Consistency {
  private HashMap<Parallel, ArrayList<String>> corresponding;
  private HashMap<ConstraintLabel, ConstraintValue> cons;
  private HashMap<String, Integer> map;
  private Random ran;

  public Consistency(HashMap<Parallel, ArrayList<String>> corresponding,
                     HashMap<ConstraintLabel, ConstraintValue> cons,
                     HashMap<String, Integer> map, Random ran) {
    super();
    this.corresponding = corresponding;
    this.cons = cons;
    this.map = map;
    this.ran = new Random();
    this.ran = ran;
  }

  private int[][] minimalNet(HashMap<String, String> eql, HashMap<String, Integer> map) {
    int[][] bound = new int[map.size()][map.size()];
    int length = bound.length;
    for (int i = 0; i < length; i++) {
      for (int j = 0; j < length; j++) {
        if (i != j) {
          bound[i][j] = Integer.MAX_VALUE;
        } else {
          bound[i][j] = 0;
        }

      }
    }

    Iterator<Entry<ConstraintLabel, ConstraintValue>> itercon = cons.entrySet()
        .iterator();
    while (itercon.hasNext()) {
      Entry entry = itercon.next();
      ConstraintLabel key = (ConstraintLabel) entry.getKey();
      ConstraintValue val = (ConstraintValue) entry.getValue();
      int i = map.get(key.getForwardLabel());
      int j = map.get(key.getBackLabel());
      bound[i][j] = val.getBack();
      bound[j][i] = -val.getForward();
    }

    Iterator<Entry<String, String>> ite = eql.entrySet().iterator();
    while (ite.hasNext()) {
      Entry entry = ite.next();
      String key = (String) entry.getKey();
      String val = (String) entry.getValue();
      int i = map.get(key);
      int j = map.get(val);
      bound[i][j] = 0;
      bound[j][i] = 0;
    }

    for (int k = 0; k < length; k++) {
      for (int i = 0; i < length; i++) {
        for (int j = 0; j < length; j++) {
          if (i == j) {
            continue;
          }

          if (bound[i][k] != Integer.MAX_VALUE
              && bound[k][j] != Integer.MAX_VALUE) {
            bound[i][j] = Math.min(bound[i][j], bound[i][k] + bound[k][j]);
          }

        }
      }
    }

    return bound;

  }

  private boolean combine(HashMap<String, String> eql, ArrayList<Parallel> par, int i) {
    if (i >= par.size()) {
      System.out.println(eql);
      int[][] bound = minimalNet(eql, map);

      for (int n = 0; n < bound.length; n++) {
        for (int m = i; m < bound.length; m++) {
          if ((bound[n][m] + bound[m][n]) < 0) {
            return false;
          }
        }
      }

      return true;

    }

    Parallel first = par.get(i);
    ArrayList<String> beh = corresponding.get(first);
    for (String second : beh) {
      eql.put(first.getName(), second);
      if (combine(eql, par, ++i)) {
        return true;
      }
      eql.remove(first, second);
      i--;
    }

    return false;
  }

  private boolean RanCombine(HashMap<String, String> eql, ArrayList<Parallel> par) {

    for (Parallel first : par) {
      ArrayList<String> beh = corresponding.get(first);
      int index = ran.nextInt(beh.size());
      eql.put(first.getName(), beh.get(index));
    }

    int[][] bound = minimalNet(eql, map);

    for (int n = 0; n < bound.length; n++) {
      for (int m = n; m < bound.length; m++) {
        if ((bound[n][m] + bound[m][n]) < 0) {
          return false;
        }
      }
    }

    return true;

  }

  private boolean setIndexs(ArrayList<Parallel> par, int[] indexs, int i,
                            int[] all) throws IOException {
    if (i == all.length) {
      HashMap<String, String> eql = getEql(par, indexs);
      int[][] bound = minimalNet(eql, map);

      for (int n = 0; n < bound.length; n++) {
        for (int m = n; m < bound.length; m++) {
          if ((bound[n][m] + bound[m][n]) < 0) {
            return false;
          }
        }
      }

      return true;
    }

    for (int j = 0; j < all[i]; j++) {
      indexs[i] = j;
      if (setIndexs(par, indexs, i + 1, all)) {
        return true;
      }

    }
    return false;

  }

  private HashMap<String, String> getEql(ArrayList<Parallel> par, int[] indexs) {
    HashMap<String, String> eql = new HashMap<String, String>();
    for (int j = 0; j < par.size(); j++) {
      Parallel first = par.get(j);
      ArrayList<String> beh = corresponding.get(first);
      int index = indexs[j];
      eql.put(first.getName(), beh.get(index));
    }

    return eql;
  }

  public boolean isConsistency(int num) throws IOException {
    if (num <= 0) {
      if (corresponding.size() > 0) {
        HashMap<String, String> eql = new HashMap<>();
        ArrayList<Parallel> par1 = new ArrayList<>(corresponding.keySet());
        return combine(eql, par1, 0);
      } else {
        HashMap<String, String> eql = new HashMap<>();
        ArrayList<Parallel> par1 = new ArrayList<>();
        return combine(eql, par1, 0);
      }
    } else if (num > 10000) {
      if (corresponding.size() > 0) {
        int[] all = new int[corresponding.size()];
        ArrayList<Parallel> par1 = new ArrayList<>(corresponding.keySet());
        for (int i = 0; i < all.length; i++) {
          all[i] = corresponding.get(par1.get(i)).size();
        }
        int[] indexs = new int[all.length];
        return setIndexs(par1, indexs, 0, all);

      } else {
        HashMap<String, String> eql = new HashMap<>();
        ArrayList<Parallel> par1 = new ArrayList<>();
        return combine(eql, par1, 0);
      }

    } else {
      for (int i = 0; i < num; i++) {
        if (corresponding.size() > 0) {
          HashMap<String, String> eql = new HashMap<>();
          ArrayList<Parallel> par1 = new ArrayList<>(corresponding.keySet());
          if (RanCombine(eql, par1)) {
            return true;
          }
        } else {
          HashMap<String, String> eql = new HashMap<>();
          ArrayList<Parallel> par1 = new ArrayList<>();
          return combine(eql, par1, 0);
        }
      }
      return false;
    }

  }

}
