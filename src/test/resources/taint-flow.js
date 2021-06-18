function createSensitiveObject(name) {
  return {
    name: name
  };
}

let message = createSensitiveObject("pablo");
console.log(message);

function createSensitiveNativeObject(name) {
  return name + "tainted";
}

let anotherTaintedMessage = createSensitiveNativeObject("pablo");
console.log(anotherTaintedMessage);