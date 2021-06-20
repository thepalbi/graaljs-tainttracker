function createSensitiveNullObject() {
  return undefined;
}

let o = createSensitiveNullObject();
// This should propagate taint!
let a = !o;
console.log(a);