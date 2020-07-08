package explanation.model;

public class Event {
  private int rTime;
  private int fTime;
  private int index;
  private String parallel;
  private int mark;
  private int isAddFault;

  public Event(String parallel, int fTime) {
    super();
    this.fTime = fTime;
    this.parallel = parallel;
  }

  public Event(int rTime) {
    super();
    this.rTime = rTime;
    this.mark = 0;
    this.parallel = null;
    this.isAddFault = 0;
  }

  public Event(int rTime, int index) {
    super();
    this.rTime = rTime;
    this.fTime = (int) 0;
    this.index = index;
    this.mark = 0;
    this.parallel = null;
  }

  public int getIsAddFault() {
    return isAddFault;
  }

  public void setIsAddFault(int isAddFault) {
    this.isAddFault = isAddFault;
  }

  public int isAddFault() {
    return isAddFault;
  }

  public void setAddFault(int isAddFault) {
    this.isAddFault = isAddFault;
  }

  public String getParallel() {
    return parallel;
  }

  public void setParallel(String parallel) {
    this.parallel = parallel;
  }

  public int getMark() {
    return mark;
  }

  public void setMark(int mark) {
    this.mark = mark;
  }

  public int getrTime() {
    return rTime;
  }

  public void setrTime(int rTime) {
    this.rTime = rTime;
  }

  public int getfTime() {
    return fTime;
  }

  public void setfTime(int fTime) {
    this.fTime = fTime;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public boolean equals(Object obj) {

    Event other = (Event) obj;
    if (index == other.index) {
      return true;
    }

    return false;

  }

  @Override
  public String toString() {
    return "Event [rTime=" + rTime + ", fTime=" + fTime + ", isAddFault="
        + isAddFault + "]";
  }

}
