  // ---- AndyMark Robits 5.5 in. ID Turntable (am-5470), drawn to the sizes in AndyMark's own STEP file (mm).
  // z = 0 is the top of the mounting plate (130 mm above the floor); the turret axis is the origin.
  // fixed: the plate, the inner race, the 50T pinion, its bracket and a flat drive motor. spin: the outer race and the 200T gear.
  // The pinion sits 158.75 mm from the axis (4:1). pin = [x, y] of the pinion in this frame.
  function robitsTurntable(fixed, spin, mk, pin) {
    const out = [];
    const put = (geo, m, par, x = 0, y = 0, z = 0) => { const g = new THREE.Mesh(geo, m); g.position.set(x, y, z); g.castShadow = true; par.add(g); out.push(g); return g; };
    const circle = (r, cx = 0, cy = 0) => { const p = new THREE.Path(); p.absarc(cx, cy, r, 0, Math.PI * 2, true); return p; };
    const ext = (s, h, seg = 64) => new THREE.ExtrudeGeometry(s, { depth: h, bevelEnabled: false, curveSegments: seg });
    const ring = (ro, ri, h) => { const s = new THREE.Shape(); s.absarc(0, 0, ro, 0, Math.PI * 2, false); s.holes.push(circle(ri)); return ext(s, h); };
    const gear = (teeth, tip, root, bore, h) => {
      const s = new THREE.Shape(), st = Math.PI * 2 / teeth;
      for (let i = 0; i < teeth; i++) for (const [r, t] of [[root, 0], [root, 0.18], [tip, 0.36], [tip, 0.64], [root, 0.82]]) { const a = (i + t) * st, x = r * Math.cos(a), y = r * Math.sin(a); if (i === 0 && t === 0) s.moveTo(x, y); else s.lineTo(x, y); }
      s.closePath(); s.holes.push(circle(bore)); return ext(s, h, 48);
    };
    const alu = mk("#c3c8cf", { metalness: 0.55, roughness: 0.38 }), race = mk("#9ea5ad", { metalness: 0.6, roughness: 0.35 }), black = mk("#22252a", { roughness: 0.7 }), grey = mk("#aab0b8", { roughness: 0.6 }), motor = mk("#2f3238", { roughness: 0.55 });
    // am-5040 mounting plate: 254 x 254 x 5.59, 139.7 centre hole, #10 holes every 12.7 mm round the edge
    const ps = new THREE.Shape(); ps.moveTo(-127, -127); ps.lineTo(127, -127); ps.lineTo(127, 127); ps.lineTo(-127, 127); ps.closePath(); ps.holes.push(circle(69.85));
    for (let i = -9; i <= 9; i++) for (const [x, y] of [[i * 12.7, -120.65], [i * 12.7, 120.65], [-120.65, i * 12.7], [120.65, i * 12.7]]) if (Math.abs(i) < 9 || y === -120.65 || y === 120.65) ps.holes.push(circle(2.55, x, y));
    put(ext(ps, 5.59, 20), alu, fixed, 0, 0, -5.59);
    // am-5039 bearing: inner race 145 to 173, outer race 175 to 203, both 10 tall
    put(ring(86.5, 72.5, 10), race, fixed);
    put(ring(101.5, 87.5, 10), race, spin);
    // am-5038 turntable gear: 200 teeth, 20 DP, 256.54 over the tips, 250.86 at the roots, 13.4 tall, on the outer race
    put(gear(200, 128.27, 125.43, 88.5, 13.4), black, spin, 0, 0, 10);
    if (pin) {
      const [px, py] = pin, a = Math.atan2(py, px);
      // am-5020_50 pinion: 50 teeth, 66 over the tips, 12.7 wide, 3/8 in hex bore
      const g = put(gear(50, 33.02, 30.2, 4.8, 12.7), grey, fixed, px, py, 10.35); g.rotation.z = a + Math.PI / 50;
      // am-5012 side motor mount under the plate edge, then a REV Core Hex lying along the pod (100 x 41 x 36 mm, from REV's STEP) and its shaft
      const br = put(new THREE.BoxGeometry(70, 58, 3.2), alu, fixed, px - 14 * Math.cos(a), py - 14 * Math.sin(a), -7.2); br.rotation.z = a;
      const mo = put(new THREE.BoxGeometry(100, 41, 36), motor, fixed, px, py, -26.9); mo.rotation.z = a + Math.PI / 2;
      const sh = put(new THREE.CylinderGeometry(2.6, 2.6, 38, 6), race, fixed, px, py, 6); sh.rotation.x = Math.PI / 2;
    }
    return out;
  }
