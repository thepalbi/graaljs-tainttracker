function createSensitiveNullObject() {
  return null;
}

let o = createSensitiveNullObject();
// This should propagate taint!
let a = !o;
console.log(a);