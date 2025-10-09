// This example finds pythagorean triples 
// up to some user-input maximum side-length.
// Try inputting '20'.

// It utilises multi-shot resumptions, where an effect handler
// can respond with multiple values.

// Define an effect for requesting an integer in [1,2...n-1]
// the handler gets to decide which value to answer with.
effect control choose-up-to(n : int) : int;

fun is-triple(a, b, c) {
  a * a + b * b == c * c
}

// Find pythagorean triples with side-lengths below n.
fun find-triple(n) {
  // This code doesn't specify how a,b,c should be picked
  val c = choose-up-to(n);
  val b = choose-up-to(c);
  val a = choose-up-to(b);
  // It just tests whether they form a triple, returning if so
  if is-triple(a, b, c)
  then Some((a, b, c))
  else None
}

// Implement an exhaustive search:
fun choose-by-trying-each(try-find-one) {
  // This handler for 'choose-up-to' resumes multiple-times 
  // trying try-find-one() with each possible value!
  with handler {
    control choose-up-to(m) {
      // Each time we are asked for a side-length, try each possibility, collecting the results
      range(1, m).flat-map fn(length) {
        // Each time we resume, find-triple continues and
        // tries that side-length, returning Some if it has found
        // a right-triangle.
        resume(length);
        // Once find-triple completes, `resume` returns here
        // and we can try the next candidate.
      };
    };
  };
  // Each call can find one triangle at most, the flat-map above collects each into a list.
  match try-find-one() {
    None -> [];
    Some(result) -> [result];
  }
}

fun print-triple((a, b, c)) {
    print-int(a); print-int(b); println-int(c);
}

fun main() {
  val limit = read-int(());
  val triples = choose-by-trying-each fn() find-triple(limit);
  triples.print-list(print-triple);
}
