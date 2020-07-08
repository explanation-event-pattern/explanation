package explanation.model;

public class Parallel implements Comparable<Parallel> {
  private String name;
  private int mark;

  public Parallel(String name) {
    super();
    this.name = name;
  }

  public Parallel(String name, int mark) {
    super();
    this.name = name;
    this.mark = mark;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMark() {
    return mark;
  }

  public void setMark(int mark) {
    this.mark = mark;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Parallel other = (Parallel) obj;
    if (name == null) {
      if (other == null)
        return true;
    } else if (name.equals(other.name))
      return true;

    return false;
  }

  @Override
  public int compareTo(Parallel o) {

    return mark > o.mark ? 1 : (mark == o.mark ? 0 : -1);
  }

  @Override
  public String toString() {
    return "Parallel [name=" + name + ", mark=" + mark + "]";
  }

}
