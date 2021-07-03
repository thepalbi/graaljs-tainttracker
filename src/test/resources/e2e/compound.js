function entryFunc(someArg) {
  console.log(someArg);
}

function entryFunc2(someArg) {
  console.log(someArg);
}

function require(query) {
  return {
    entryFunc,
    entryFunc2
  }
}

const a = require("./dog");
a.entryFunc("hola");
a.entryFunc2("hola");
