const { Sim } = require("./simcore.js");
const A = require("./autorun.js");
function runAuto(auto, verbose) {
  const sim = new Sim(); sim.reset(auto.alliance); A.placeRobot(sim, auto.start);
  const run = new A.AutoRunner(sim, auto); const ends = []; let lastI = run.i, maxCross = 0, nan = false;
  for (let f = 0; f < 30 * 60 && !run.done; f++) {
    const inp = run.update(1 / 60); sim.step(1 / 60, inp);
    if ([inp.fwd, inp.str, inp.rot].some(v => !Number.isFinite(v))) nan = true;
    if (run.path) maxCross = Math.max(maxCross, run.path._lastCross || 0);
    if (run.i !== lastI) { const p = A.toPedro(sim.rob.x, sim.rob.y, sim.rob.th); ends.push({ step: lastI + 1, t: +run.t.toFixed(2), x: +p.x.toFixed(1), y: +p.y.toFixed(1), h: +(p.h / A.D2R).toFixed(0) }); lastI = run.i; }
  }
  return { done: run.done, t: +run.t.toFixed(1), log: run.log.map(l => l.msg), ends, nan };
}
module.exports = runAuto;
if (require.main === module) {
  const cases = {
    "straight right": [{ x: 40, y: 82, h: 0 }],
    "straight up":    [{ x: 9, y: 120, h: 0 }],
    "down then right":[{ x: 30, y: 30, h: 0 }, { x: 60, y: 20, h: 0 }],
    "turn 90 while moving": [{ x: 40, y: 60, h: 90 }],
    "turn 180": [{ x: 40, y: 82, h: 180 }],
    "turn 180 in place-ish": [{ x: 12, y: 82, h: 180 }],
    "to wall corner": [{ x: 2, y: 2, h: 0 }],
    "through HIVE": [{ x: 130, y: 82, h: 0 }],
    "short hop": [{ x: 11, y: 82, h: 0 }],
    "same point": [{ x: 9, y: 82, h: 0 }],
    "far diagonal": [{ x: 120, y: 20, h: -45 }],
    "backwards": [{ x: 9, y: 50, h: 0 }, { x: 9, y: 100, h: 0 }],
    "tangent curve": [{ x: 40, y: 40, h: 0, heading: "tangent", controls: [{ x: 40, y: 82 }] }],
    "constant 90": [{ x: 30, y: 40, h: 90, heading: "constant" }],
    "zigzag": [{ x: 30, y: 60, h: 0 }, { x: 15, y: 40, h: 0 }, { x: 30, y: 20, h: 0 }, { x: 15, y: 15, h: 0 }],
    "h 270": [{ x: 30, y: 60, h: 270 }],
    "h -170 to 170": [{ x: 30, y: 82, h: -170 }, { x: 40, y: 60, h: 170 }],
  };
  for (const [name, paths] of Object.entries(cases)) {
    const auto = { alliance: "red", start: { x: 9, y: 82, h: 0 }, steps: paths.map(p => ({ type: "path", heading: "linear", controls: [], maxPower: 1, ...p })) };
    const r = runAuto(auto);
    const bad = !r.done || r.log.length || r.nan;
    console.log((bad ? "BAD " : "ok  ") + name.padEnd(24), "done", r.done, "t", r.t, r.nan ? "NaN!" : "", JSON.stringify(r.ends), r.log.join(" | "));
  }
}
