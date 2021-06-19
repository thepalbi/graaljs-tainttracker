function createSensitiveNativeObject() {
  return 41;
}

let o = createSensitiveNativeObject();
// This should propagate taint!
o++;
console.log(o);