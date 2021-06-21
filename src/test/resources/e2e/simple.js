function entryFunc(someArg) {
  console.log(someArg);
}

function require(query) {
  return {
    entry: entryFunc
  }
}

const a = require("./dog");
a.entry("hola");
