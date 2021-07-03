function entryFunc(someWrappedArg) {
  console.log(someWrappedArg.f);
}

function require(query) {
  return {
    entryFunc
  }
}

const a = require("./dog");
a.entryFunc({f: "hola"});
