package explanation.util;

import explanation.model.Event;

import java.util.Comparator;

public class SortByTime implements Comparator {

  @Override
  public int compare(Object o1, Object o2) {
    // TODO Auto-generated method stub
    Event s1 = (Event) o1;
    Event s2 = (Event) o2;
    if (s1.getfTime() > s2.getfTime()) {
      return 1;
    }

    return -1;
  }

}
