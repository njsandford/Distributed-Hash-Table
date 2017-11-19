public enum Task {
  WORDS("words"), LETTERS("letters"), LINES("lines");

  private String value;
  private Task(String value) { this.value = value; }
  public String getValue() { return this.value; }
}
