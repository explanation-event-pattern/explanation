package explanation.model;

public class ConstraintValue {
  private int isPrall;
  private int forward;
  private int back;

  public ConstraintValue(int isPrall) {
    super();
    this.isPrall = isPrall;
    this.forward = Integer.MAX_VALUE;
    this.back = Integer.MIN_VALUE;
  }

  public ConstraintValue(int forward, int back) {
    super();
    this.isPrall = 0;
    this.forward = forward;
    this.back = back;
  }

  public int getIsPrall() {
    return isPrall;
  }

  public void setIsPrall(int isPrall) {
    this.isPrall = isPrall;
  }

  public int getForward() {
    return forward;
  }

  public void setForward(int forward) {
    this.forward = forward;
  }

  public int getBack() {
    return back;
  }

  public void setBack(int back) {
    this.back = back;
  }

  @Override
  public String toString() {
    return "ConstraintValue [forward=" + forward + ", back=" + back + "]";
  }


}
