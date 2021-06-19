function createSensitiveNativeObject(name) {
  return name + "tainted";
}

let anotherTaintedMessage = createSensitiveNativeObject("pablo");
let taintedByBinaryOp = anotherTaintedMessage + "more taint inside";
console.log(taintedByBinaryOp);