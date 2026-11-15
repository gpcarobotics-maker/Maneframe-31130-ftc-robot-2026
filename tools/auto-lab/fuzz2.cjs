const A = require("./autorun.js"), runAuto = require("./fuzz.cjs");
let seed = 7; const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
let n = 0, bad = 0, blockedSkipped = 0;
for (let k = 0; k < 40000 && n < 300; k++) {
  const alliance = rnd() < 0.5 ? "red" : "blue";
  const start = alliance === "red" ? { x: 9, y: 20 + rnd() * 100, h: 0 } : { x: 135, y: 20 + rnd() * 100, h: 180 };
  const steps = []; let prev = start, ok = true;
  const m = 1 + Math.floor(rnd() * 3);
  for (let j = 0; j < m; j++) {
    const heading = ["linear", "linear", "constant", "tangent"][Math.floor(rnd() * 4)];
    const st = { type: "path", x: Math.round((10 + rnd() * 124) * 2) / 2, y: Math.round((10 + rnd() * 124) * 2) / 2, h: Math.round(rnd() * 360 - 180), heading, controls: rnd() < 0.4 ? [{ x: 10 + rnd() * 124, y: 10 + rnd() * 124 }] : [], maxPower: rnd() < 0.3 ? 0.6 : 1 };
    if (A.pathBlock(prev, st)) { ok = false; break; }
    steps.push(st); prev = { x: st.x, y: st.y, h: heading === "tangent" ? A.tangentEnd({ start, steps }, steps.length - 1) : st.h };
  }
  if (!ok || A.obstacleAt(start.x, start.y, start.h * A.D2R)) { blockedSkipped++; continue; }
  n++;
  const r = runAuto({ alliance, start, steps });
  if (!r.done || r.log.length || r.nan) { bad++; if (bad <= 12) console.log("BAD", JSON.stringify({ start, steps: steps.map(s => [s.x, s.y, s.h, s.heading[0], s.controls.length ? s.controls.map(c => [c.x.toFixed(0), c.y.toFixed(0)]) : 0, s.maxPower]) }), r.log.join(" | "), r.nan ? "NaN" : "", r.done ? "" : "not done"); }
}
console.log(`clear random autos: ${n}, bad: ${bad}, skipped as blocked: ${blockedSkipped}`);
