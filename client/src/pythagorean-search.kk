// This example finds pythagorean triples 
// up to some user-input maximum side-length.
// Try inputting '20'.

// It utilises multi-shot resumptions, where an effect handler
// can respond with multiple values.

// Define an effect for requesting an integer in [1,2...n-1]
// the handler gets to decide which value to answer with.
effect control choose-up-to(n : int) : int;

fun is-triple(a, b, c) {
  a * a + b * b == c * c;
}

// Find pythagorean triples with side-lengths below n.
fun find-triple(n) {
  // This code doesn't specify how a,b,c should be picked
  val c = choose-up-to(n);
  val b = choose-up-to(c);
  val a = choose-up-to(b);
  // It just tests whether they form a triple, printing if so
  if is-triple(a, b, c)
  then { print-int(a); print-int(b); println-int(c); }
  else ();
}

// a helper function to loop
fun for(start, stop, action) {
  if start >= stop
  then ()
  else {
    action(start);
    for(start + 1, stop, action)
  };
};

// Implement an exhaustive search:
fun choose-by-trying-each(action) {
  // This handler for 'choose-up-to' resumes multiple-times 
  // trying action() with each possible value!
  with handler {
    control choose-up-to(m) {
      // Try each possible side-length
      for(1, m) fn(i) {
        // Each time we resume, find-triple continues and
        // tries that side-length, printing if it has found 
        // a right-triange.
        resume(i);
        // Once find-triple completes, `resume` returns here
        // and we can try the next candidate.
      };
    };
  };
  action();
}

fun main() {
  val limit = read-int(());
  with choose-by-trying-each;
  find-triple(limit);
}
