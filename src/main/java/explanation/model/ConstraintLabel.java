package explanation.model;

public class ConstraintLabel {
  private String forwardLabel;
  private String backLabel;
  private double inter;
  private int num;

  public ConstraintLabel(String forwardLabel, String backLabel) {
    super();
    this.forwardLabel = forwardLabel;
    this.backLabel = backLabel;
    this.num = 0;
  }

  public int getNum() {
    return num;
  }


  public void setNum(int num) {
    this.num = num;
  }


  public double getInter() {
    return inter;
  }


  public void setInter(double inter) {
    this.inter = inter;
  }

  public String getForwardLabel() {
    return forwardLabel;
  }

  public void setForwardLabel(String forwardLabel) {
    this.forwardLabel = forwardLabel;
  }

  public String getBackLabel() {
    return backLabel;
  }

  public void setBackLabel(String backLabel) {
    this.backLabel = backLabel;
  }


  @Override
  public String toString() {
    return "ConstraintLabel [forwardLabel=" + forwardLabel + ", backLabel=" + backLabel + ", inter=" + inter
        + ", num=" + num + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((backLabel == null) ? 0 : backLabel.hashCode());
    result = prime * result + ((forwardLabel == null) ? 0 : forwardLabel.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {

    ConstraintLabel other = (ConstraintLabel) obj;
    if (backLabel.equals(other.backLabel) && forwardLabel.equals(other.forwardLabel)) {
      return true;
    }

    return false;

  }


}
