function createSensitiveString() {
  return "holus";
}

var sensitiveString = createSensitiveString();
var containerNotTainted = {
  name: sensitiveString,
};
var propReadShouldBeTainted = containerNotTainted.name;
console.log(propReadShouldBeTainted);


