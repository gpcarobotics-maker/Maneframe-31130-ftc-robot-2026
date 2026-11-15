// BioBuzz Auto Lab: a Pedro-style path follower and auto runner on top of SimCore.
// Pedro coordinates: inches, (0,0) at the bottom-left corner (red wall on the left, audience at the bottom),
// +x right, +y up, heading 0 = facing right, counter-clockwise positive.
(function (root, factory) { if (typeof module === "object" && module.exports) module.exports = factory(); else root.AutoRun = factory(); })(this, function () {
  const IN = 0.0254, F = 72 * IN, D2R = Math.PI / 180;
  const wrap = a => { while (a > Math.PI) a -= 2 * Math.PI; while (a < -Math.PI) a += 2 * Math.PI; return a; };
  const clamp = (v, a, b) => v < a ? a : v > b ? b : v;

  // sim world (metres, body front = -y) <-> Pedro (inches, heading of the robot's front)
  const toPedro = (x, y, th) => ({ x: (x + F) / IN, y: (y + F) / IN, h: wrap(th - Math.PI / 2) });
  const toWorld = (px, py, h) => ({ x: px * IN - F, y: py * IN - F, th: h + Math.PI / 2 });

  // Field obstacles in Pedro inches, from SimCore's FIELD: the two HIVE frame base bars, the four FLOWER posts, the walls.
  // The robot is a 17.9 x 17.7 in box (SimCore R.halfX / R.front / R.rear), front along its heading.
  const HW = 0.227 / IN, HL = 0.225 / IN, BAR_HALF = 0.02 / IN, FX = 24.73, FY = 19.47;
  const BARS = [[72 - FX, 72 - FY, 72 + FY], [72 + FX, 72 - FY, 72 + FY]];           // [x, y0, y1], 1.6 in wide
  const FLOWERS = [[48, 144 - 3.2], [144 - 3.2, 96], [3.2, 48], [96, 3.2]], FLOWER_R = 0.06 / IN;
  // what the robot would touch at pose (x, y, h radians); "" when it's clear. margin in inches.
  function obstacleAt(x, y, h, margin = 0, wallMargin = 0) {
    const c = Math.cos(h), s = Math.sin(h);
    const distTo = (px, py) => { const du = (px - x) * c + (py - y) * s, dv = -(px - x) * s + (py - y) * c; return Math.hypot(du - clamp(du, -HL, HL), dv - clamp(dv, -HW, HW)); };
    for (const [fx, fy] of FLOWERS) if (distTo(fx, fy) < FLOWER_R + margin) return "a FLOWER";
    for (const [bx, y0, y1] of BARS) for (let by = y0; by <= y1 + 1e-6; by += 0.5) if (distTo(bx, by) < BAR_HALF + margin) return "the HIVE frame bar";
    for (const [u, v] of [[HL, HW], [HL, -HW], [-HL, HW], [-HL, -HW]]) { const px = x + u * c - v * s, py = y + u * s + v * c; const w = wallMargin - 0.05; if (px < w || px > 144 - w || py < w || py > 144 - w) return "the wall"; }
    return "";
  }
  // where a path first runs into something: {what, x, y} or null
  function pathBlock(start, step) {
    const pts = [{ x: start.x, y: start.y }, ...(step.controls || []), { x: step.x, y: step.y }], h0 = start.h * D2R, h1 = step.h * D2R;
    for (let k = 0; k <= 60; k++) {
      const t = k / 60, p = bez(pts, t);
      let h = step.heading === "constant" ? h1 : step.heading === "tangent" ? (() => { const d = bezD(pts, Math.min(t, 0.999)); return Math.hypot(d.x, d.y) > 1e-6 ? Math.atan2(d.y, d.x) : h0; })() : h0 + wrap(h1 - h0) * t;
      const what = obstacleAt(p.x, p.y, h, 0.3); if (what) return { what, x: p.x, y: p.y, t };
    }
    return null;
  }

  // Bezier of any degree, De Casteljau
  function bez(pts, t) {
    let a = pts.map(p => [p.x, p.y]);
    while (a.length > 1) { const b = []; for (let i = 0; i < a.length - 1; i++) b.push([a[i][0] + (a[i + 1][0] - a[i][0]) * t, a[i][1] + (a[i + 1][1] - a[i][1]) * t]); a = b; }
    return { x: a[0][0], y: a[0][1] };
  }
  function bezD(pts, t) {
    const n = pts.length - 1; if (n < 1) return { x: 1, y: 0 };
    const d = []; for (let i = 0; i < n; i++) d.push({ x: n * (pts[i + 1].x - pts[i].x), y: n * (pts[i + 1].y - pts[i].y) });
    return n === 1 ? d[0] : bez(d, t);
  }

  // A path as Pedro would see it: start pose, control points, end pose, heading mode.
  class PathTrack {
    constructor(start, step) {
      this.pts = [{ x: start.x, y: start.y }, ...(step.controls || []), { x: step.x, y: step.y }];
      this.h0 = start.h; this.h1 = step.h * D2R; this.mode = step.heading || "linear"; this.maxPower = step.maxPower || 1;
      // arc-length table
      const N = 200; this.N = N; this.tab = [0]; let L = 0, prev = bez(this.pts, 0);
      for (let i = 1; i <= N; i++) { const p = bez(this.pts, i / N); L += Math.hypot(p.x - prev.x, p.y - prev.y); this.tab.push(L); prev = p; }
      this.L = Math.max(L, 1e-6); this.t = this.L < 0.5 ? 1 : 0; this.time = 0; this.stuckT = 0;
      this.timeout = 1.5 + this.L / 30 * 2.2 + Math.abs(wrap(this.h1 - this.h0)) / Math.PI * 1.5;   // generous: about 30 in/s average, then double, plus time to turn
    }
    tAt(s) { // arc length -> t
      s = clamp(s, 0, this.L); let lo = 0, hi = this.N;
      while (hi - lo > 1) { const m = (lo + hi) >> 1; if (this.tab[m] < s) lo = m; else hi = m; }
      const f = (s - this.tab[lo]) / Math.max(1e-9, this.tab[hi] - this.tab[lo]); return (lo + f) / this.N;
    }
    sAt(t) { const i = Math.floor(clamp(t, 0, 1) * this.N); return this.tab[Math.min(i, this.N)]; }
    targetHeading(t) {
      if (this.mode === "constant") return this.h1;
      if (this.mode === "tangent") { const d = bezD(this.pts, Math.min(t, 0.999)); return Math.atan2(d.y, d.x); }
      return this.h0 + wrap(this.h1 - this.h0) * clamp(t, 0, 1);  // linear
    }
    endHeading() { return this.mode === "tangent" ? this.targetHeading(1) : this.h1; }
    // closest point, searching forward from the last t
    track(p) {
      let best = this.t, bd = Infinity;
      // search a fixed distance along the path (not a fixed slice of t), so tight curves and hairpins still track
      const s = this.sAt(this.t), t0 = this.tAt(s - 1), t1 = this.tAt(s + 15), dt = Math.max(1e-4, (t1 - t0) / 120);
      for (let t = t0; t <= t1 + 1e-9; t += dt) {
        const q = bez(this.pts, t), d = (q.x - p.x) ** 2 + (q.y - p.y) ** 2; if (d < bd) { bd = d; best = t; }
      }
      this.t = Math.max(this.t, best); return Math.sqrt(bd);
    }
    // one control update: returns {fwd, str, rot, done}
    update(pose, vel, dt) {
      this.time += dt;
      const cross = this.track(pose), s = this.sAt(this.t), remain = this.L - s, end = this.pts[this.pts.length - 1];
      const dEnd = Math.hypot(end.x - pose.x, end.y - pose.y);
      let vx, vy, power;
      if (remain > 3 && this.t < 0.97) {
        const look = bez(this.pts, this.tAt(s + clamp(6 + 0.25 * Math.hypot(vel.x, vel.y), 6, 14)));
        vx = look.x - pose.x; vy = look.y - pose.y;
        const n = Math.hypot(vx, vy) || 1; vx /= n; vy /= n;
        power = Math.min(this.maxPower, 0.18 + 0.045 * remain);     // slow down over the last ~18 in
      } else {                                                      // end: drive at the end point, then hold it
        vx = end.x - pose.x; vy = end.y - pose.y; const n = Math.hypot(vx, vy) || 1; vx /= n; vy /= n;
        power = Math.min(this.maxPower, 0.06 * dEnd + 0.04 * (dEnd > 0.6 ? 1 : 0));
        // damp leftover speed
        vx -= 0.012 * vel.x / Math.max(power, 0.05); vy -= 0.012 * vel.y / Math.max(power, 0.05);
      }
      const hT = this.t >= 0.97 ? this.endHeading() : this.targetHeading(this.t), hErr = wrap(hT - pose.h);
      // world velocity -> robot frame (forward along heading, right = heading - 90 deg)
      const c = Math.cos(pose.h), sn = Math.sin(pose.h);
      const fwd = power * (vx * c + vy * sn), str = power * (vx * sn - vy * c);
      const kick = Math.abs(hErr) > 0.5 * D2R ? Math.sign(hErr) * 0.07 : 0;   // enough to beat wheel friction on the last few degrees
      const rot = -clamp(1.6 * hErr - 0.12 * vel.w + kick, -0.8, 0.8);    // sim: +rot turns clockwise
      const speed = Math.hypot(vel.x, vel.y);
      // pushing but not moving or turning for 0.8 s: something is in the way
      if (this.time > 0.5 && dEnd > 2 && speed < 1.5 && Math.abs(vel.w) < 0.4) this.stuckT += dt; else this.stuckT = 0;
      const stuck = this.stuckT > 0.8, timedOut = this.time > this.timeout;
      const done = (this.t >= 0.97 && dEnd < 1.5 && Math.abs(wrap(this.endHeading() - pose.h)) < 3 * D2R && speed < 4) || timedOut || stuck;
      return { fwd, str, rot, done, timedOut, stuck, cross, dEnd };
    }
  }

  // Runs an auto (a list of steps) against a SimCore Sim. One call per frame.
  class AutoRunner {
    constructor(sim, auto) {
      this.sim = sim; this.auto = auto; this.i = -1; this.stepTime = 0; this.t = 0; this.done = false;
      this.mech = { flyOn: false, intake: 0, fire: false };
      this.pose = { ...auto.start, h: auto.start.h * D2R }; this.log = [];
      this.next();
    }
    est() {   // the robot's own belief (odometry + Limelight), like Pedro's localizer
      const r = this.sim.sense || this.sim.rob;
      const p = toPedro(r.x, r.y, r.th), c = Math.cos(r.th), s = Math.sin(r.th);
      return { pose: p, vel: { x: (r.vx || 0) / IN, y: (r.vy || 0) / IN, w: r.w || 0 } };
    }
    next() {
      this.i++; this.stepTime = 0; this.path = null;
      const st = this.auto.steps[this.i];
      if (!st) { this.done = true; this.mech.fire = false; return; }
      if (st.type === "path") {
        const prev = this.lastPose(this.i);
        this.path = new PathTrack({ x: prev.x, y: prev.y, h: prev.h * D2R }, st);
      }
      if (st.type === "flywheel") this.mech.flyOn = !!st.on;
      if (st.type === "intake") this.mech.intake = st.on ? 1 : 0;
      if (st.type === "shoot") { this.mech.flyOn = true; this.mech.fire = true; }
    }
    lastPose(i) { // the pose a step starts from: the end of the last path before it, or the start pose
      for (let k = i - 1; k >= 0; k--) { const s = this.auto.steps[k]; if (s.type === "path") return { x: s.x, y: s.y, h: s.heading === "tangent" ? tangentEnd(this.auto, k) : s.h }; }
      return this.auto.start;
    }
    // returns the input object for sim.step
    update(dt) {
      this.t += dt; this.stepTime += dt;
      const inp = { fwd: 0, str: 0, rot: 0, fieldCentric: false, flyOn: this.mech.flyOn, auto: true, intake: this.mech.intake, fire: false, eject: false };
      if (this.done) return inp;
      const st = this.auto.steps[this.i];
      if (st.type === "path") {
        const { pose, vel } = this.est(); const o = this.path.update(pose, vel, dt);
        inp.fwd = o.fwd; inp.str = o.str; inp.rot = o.rot;
        if (o.done) {
          if (o.stuck || o.timedOut) {
            const what = obstacleAt(pose.x, pose.y, pose.h, 1.5, 1.5), at = `at (${pose.x.toFixed(0)}, ${pose.y.toFixed(0)})`;
            this.log.push({ t: this.t, msg: o.stuck ? `Step ${this.i + 1} got stuck ${at}, ${o.dEnd.toFixed(0)} in short of its end${what ? ": it ran into " + what : ""}.` : `Step ${this.i + 1} timed out ${o.dEnd.toFixed(1)} in from its end point${what ? ", against " + what : ""}.` });
          }
          this.next();
        }
      } else if (st.type === "shoot") {
        inp.fire = true; if (this.stepTime >= (st.seconds || 2)) { this.mech.fire = false; this.next(); }
      } else if (st.type === "wait") {
        if (this.stepTime >= (st.seconds || 1)) this.next();
      } else this.next();   // flywheel / intake switch instantly
      return inp;
    }
  }
  function tangentEnd(auto, k) {   // end heading (deg) of a tangent path
    const s = auto.steps[k]; let prev = auto.start;
    for (let j = k - 1; j >= 0; j--) if (auto.steps[j].type === "path") { prev = auto.steps[j]; break; }
    const pts = [{ x: prev.x, y: prev.y }, ...(s.controls || []), { x: s.x, y: s.y }], d = bezD(pts, 0.999);
    return Math.atan2(d.y, d.x) / D2R;
  }

  // Put the robot (and the preloads sitting against its front) at the auto's start pose.
  function placeRobot(sim, start) {
    const r = sim.rob, near = [];
    for (const b of sim.balls) if (b.state === "free" && Math.hypot(b.x - r.x, b.y - r.y) < 0.4) near.push([b, sim.toBody(b.x, b.y)]);
    const w = toWorld(start.x, start.y, start.h * D2R);
    Object.assign(r, { x: w.x, y: w.y, th: w.th, vx: 0, vy: 0, w: 0 });
    for (const [b, bp] of near) { const [x, y] = sim.toWorld(bp[0], bp[1]); b.x = x; b.y = y; b.vx = b.vy = b.vz = 0; }
    sim.sense = null; sim.mv = null; sim.solved = null; sim.solveAt = 0;
  }

  return { PathTrack, AutoRunner, placeRobot, toPedro, toWorld, bez, bezD, tangentEnd, wrap, obstacleAt, pathBlock, BARS, FLOWERS, FLOWER_R, HW, HL, IN, F, D2R };
});
