class A {
  private int x;
  private int y;

  protected String name;

  public void setX(int val) { // Noncompliant {{Refactor this setter so that it actually refers to the field "x".}}
    if (val >= 0 && val < MAX) {
      this.y = val;
    }
  }

  public int getY() { // Noncompliant  {{Refactor this getter so that it actually refers to the field "y".}}
    return this.x;
  }

  int get() {
    return 0;
  }

  boolean is() {
    return true;
  }

  void set(int i) {

  }

  int getMax() {
    int max = 42;
    return max;
  }
}


class B extends A {

  public String getName() {
    return name;
  }

}
