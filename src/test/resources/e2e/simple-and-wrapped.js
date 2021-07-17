function wrappedEntryFunc(someWrappedArg) {
  console.log(someWrappedArg.f);
}

function simpleEntryFunc(arg) {
  console.log(arg);
}

function require(query) {
  return {wrappedEntryFunc, simpleEntryFunc}
}

const a = require("./dog");
a.simpleEntryFunc("holis");
a.wrappedEntryFunc({f: "hola"});
