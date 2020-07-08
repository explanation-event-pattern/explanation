package explanation.algorithm;

import explanation.model.ConstraintLabel;
import explanation.model.ConstraintValue;
import explanation.model.Event;
import explanation.model.Parallel;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Transition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Modification {

  public static double modify(int bindingNum, boolean isApro, boolean isSimple, HashMap<ConstraintLabel, ConstraintValue> cons, HashMap<String, Event> timeSeries, HashMap<Parallel, ArrayList<String>> corresponding) throws IOException {
    double fMeasure;

    if (!isApro) {
      Exact exact = new Exact(timeSeries, cons, corresponding);
      fMeasure = exact.calculate(isSimple);
    } else {
      Approximate appro = new Approximate(bindingNum, timeSeries, cons, corresponding);
      fMeasure = appro.calculate();
    }


    return fMeasure;
  }

  public static double modifyAndWrite(String path, int bindingNum, boolean isApro, boolean isSimple, HashMap<ConstraintLabel, ConstraintValue> cons, HashMap<String, Event> timeSeries, HashMap<Parallel, ArrayList<String>> corresponding) throws IOException {
    double fMeasure;

    if (!isApro) {
      Exact exact = new Exact(timeSeries, cons, corresponding);
      fMeasure = exact.calculate(isSimple, path);
    } else {
      Approximate appro = new Approximate(bindingNum, timeSeries, cons, corresponding);
      fMeasure = appro.calculate(path);
    }


    return fMeasure;
  }

  public static HashMap<Parallel, ArrayList<String>> parBuild(String parrel, PetriNet model) throws IOException {
    File file = new File(parrel);
    FileReader fr = new FileReader(file);
    BufferedReader br = new BufferedReader(fr);
    String line = null;
    HashSet<String> forward = new HashSet<String>();
    HashSet<String> back = new HashSet<String>();
    while ((line = br.readLine()) != null) {
      String[] spl = line.split(",");
      forward.add(spl[0]);
      back.add(spl[1]);
    }

    br.close();
    fr.close();

    HashMap<Parallel, ArrayList<String>> equal = new HashMap<Parallel, ArrayList<String>>();
    ArrayList<Transition> trans = model.getTransitions();
    for (Transition tmp : trans) {
      String label = tmp.getIdentifier();
      Iterator it = null;
      ArrayList<String> beh = new ArrayList<String>();
      if (forward.contains(label)) {
        HashSet<Place> b = tmp.getSuccessors();
        it = b.iterator();
        while (it.hasNext()) {
          Place no = (Place) it.next();
          Iterator tmpIter = no.getSuccessors().iterator();
          while (tmpIter.hasNext()) {
            Transition t = (Transition) tmpIter.next();
            beh.add(t.getIdentifier());
          }
        }
        Parallel par = new Parallel(label, 1);
        equal.put(par, beh);
      } else if (back.contains(label)) {
        HashSet<Place> b = tmp.getPredecessors();
        it = b.iterator();
        while (it.hasNext()) {
          Place no = (Place) it.next();
          Iterator tmpIter = no.getPredecessors().iterator();
          while (tmpIter.hasNext()) {
            Transition t = (Transition) tmpIter.next();
            beh.add(t.getIdentifier());
          }
        }
        Parallel par = new Parallel(label, 2);
        equal.put(par, beh);
      }


    }

    return equal;

  }


}
