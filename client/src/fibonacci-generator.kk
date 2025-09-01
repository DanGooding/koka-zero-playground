// define a new effect 'yield-int'
// this sends an int outwards to the handler (like an exception)
// the result type is unit (void)
// so the handler can resume from this effect by passing ().
effect control yield-int(x : int) : ();

// a recursive function that yields the fibonacci numbers
fun generate-fibs() {
  fun generate-fibs-from(current, next) {
    yield-int(current);
    generate-fibs-from(next, current + next);
  };
  generate-fibs-from(0, 1);
}

fun main() {
  // define a handler for the yield-int effect
  with control yield-int(n) { // for each yielded value n:
    if n <= 100
    then {
      // print the fibonacci numbers below 100
      print-int(n);
      resume(());  // keep generating
    }else {
      ();  // stop generating once we hit 100
    };
  };
  // call this under the above handler
  generate-fibs();
}