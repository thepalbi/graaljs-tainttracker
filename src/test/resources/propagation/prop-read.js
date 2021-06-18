function createSensitiveObject(name) {
  // This whole object is tainted
  // TAINT
  return {
    name: name
  };
}

var obj = createSensitiveObject("pablo");
// TAINT: base of prop read is tainted -> prop itself is tainted
var prop = obj.name;
console.log(prop);


