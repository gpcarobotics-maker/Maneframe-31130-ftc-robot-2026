"""Build the BioBuzz Final Build page from the team's Test Drive sim (docs/BioBuzz Test Drive.html).

The physics (SimCore) is untouched. Changes are in the view only:
- the CAD's generic turntable is replaced by the AndyMark Robits am-5470, drawn from AndyMark's STEP sizes (robits.js)
- an X-ray switch shows balls moving through the intake, lift, booster and flywheel
- an Inspect view orbits the robot; the other views are the sim's own
"""
import sys, re
src, robits, out = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(src).read()
rb = open(robits).read()

def rep(old, new, count=1):
    global s
    n = s.count(old)
    assert n == count, f"expected {count} of {old[:60]!r}, found {n}"
    s = s.replace(old, new)

# the published skeleton comes off: the Artifact tool adds its own
s = s[s.index("<title>"):]
s = s[:s.rindex("</body></html>")].rstrip() + "\n"

rep("<title>BioBuzz Test Drive</title>", "<title>BioBuzz Final Build</title>")
rep("<h1>BioBuzz Test Drive</h1>", "<h1>BioBuzz Final Build</h1>")
rep('<button id="camBtn" type="button">View: Driver</button>',
    '<button id="camBtn" type="button">View: Driver</button>\n  <button id="xrayBtn" type="button" aria-pressed="false">X-ray: off</button>')
rep('filename: "BioBuzz_Test_Drive.html"', 'filename: "BioBuzz_Final_Build.html"')

# side panel: what this page is
rep("  <h2>Drive team</h2>", """  <h2>Final build</h2>
  <p class="note" style="margin-top:0">The robot from your CAD on the full-size field, with the real <b>AndyMark Robits 5.5 in. ID turntable</b> (am-5470) in place of the CAD's generic ring. Drive it, collect, and shoot.</p>
  <div style="display:flex;gap:6px;flex-wrap:wrap">
    <button id="inspBtn" type="button">Inspect the robot</button>
    <button id="xrayBtn2" type="button">X-ray: off</button>
  </div>
  <p class="note"><b>X-ray</b> makes the robot see-through so you can watch each ball go under the flaps, up the lift bands, past the booster and off the flywheel. In <b>Inspect</b>, drag to turn and scroll to zoom; you can still drive.</p>
  <h2>Drive team</h2>""")
rep("<tr><td>N, V, G</td><td>NECTAR, view, field-centric</td></tr>",
    "<tr><td>N, V, G</td><td>NECTAR, view, field-centric</td></tr><tr><td>X</td><td>X-ray on/off</td></tr><tr><td>Drag, scroll</td><td>Turn and zoom, in Inspect view</td></tr>")
rep("""<details><summary>What's real here</summary>""", """<details><summary>What changed from the CAD</summary>
    <p class="note"><b>Turntable:</b> the mounting plate (254 mm square, 139.7 mm hole), bearing (145 mm inside, 203 mm outside), 200-tooth gear (256.5 mm across) and 50-tooth pinion are drawn from AndyMark's STEP file. The CAD had a 213 mm gear and a 12.6 mm pinion.</p>
    <p class="note"><b>Turret motor:</b> the real pinion sits 158.75 mm from the turret's centre, and no upright motor fits there. It's shown as a REV Core Hex (100 × 41 × 36 mm) lying over the right drive pod, the one pocket with room: 60 mm of height.</p>
    <p class="note"><b>Removed:</b> the CAD's old turret motor, its mount and the turret stop finger, which would hit the real pinion. The robot needs a new hard stop.</p>
    <p class="note"><b>Not changed:</b> the physics. Turret speed, ball path and shots are the Test Drive sim's.</p>
  </details>
  <details><summary>What's real here</summary>""")

# the robot: drop the generic turntable parts, add the Robits assembly, keep every material for the X-ray switch
old_loop = """  for (const q of robotData.meshes) { const m = new THREE.Mesh(unpack(q.m), mat(q.color, { metalness: 0.2, roughness: 0.6, side: THREE.DoubleSide })); m.castShadow = true; m.receiveShadow = q.group === "chassis"; groups[q.group].add(m); }"""
new_loop = rb + """
  // Final build: these CAD meshes were a generic turntable. 26 deck plate, 27 ring, 28 motor mount and stop post, 44/45 turret motor, 49 pinion.
  const DROP = new Set([26, 27, 28, 44, 45, 49]);
  // triangle ranges inside two meshes: the 213 mm gear on the turret (mesh 30) and the stop finger that would hit the real pinion (mesh 31)
  const CUT = { 30: [0, 6584], 31: [5984, 6354] };
  const robotMeshes = [];
  robotData.meshes.forEach((q, i) => {
    if (DROP.has(i)) return;
    const geo = unpack(q.m);
    if (CUT[i]) { const p = geo.attributes.position.array, [a, b] = CUT[i], keep = new Float32Array(p.length - (b - a) * 9); keep.set(p.subarray(0, a * 9)); keep.set(p.subarray(b * 9), a * 9); geo.setAttribute("position", new THREE.BufferAttribute(keep, 3)); geo.computeVertexNormals(); }
    const m = new THREE.Mesh(geo, mat(q.color, { metalness: 0.2, roughness: 0.6, side: THREE.DoubleSide })); m.castShadow = true; m.receiveShadow = q.group === "chassis"; groups[q.group].add(m); robotMeshes.push(m);
  });
  // pinion over the right drive pod, 15 degrees ahead of straight right: the pocket with 60 mm of free height (see the Design Review)
  const PIN_A = -15 * D2R;
  robotMeshes.push(...robitsTurntable(chassisG, turretG, mat, [158.75 * Math.cos(PIN_A), 158.75 * Math.sin(PIN_A)]));
  let xray = false;
  function setXray(on) {
    xray = on;
    for (const m of robotMeshes) { const mm = m.material; mm.transparent = on; mm.opacity = on ? 0.16 : 1; mm.depthWrite = !on; mm.needsUpdate = true; m.castShadow = !on; }
    for (const id of ["xrayBtn", "xrayBtn2"]) { const b = $(id); b.textContent = "X-ray: " + (on ? "on" : "off"); b.setAttribute("aria-pressed", on); }
  }
  for (const id of ["xrayBtn", "xrayBtn2"]) $(id).onclick = e => { e.target.blur(); setXray(!xray); };
  // A held ball in the turret rides between the booster roller and its pad, 17.5 mm off the turret's centre line toward the flywheel.
  // SimCore keeps it on the centre line, which would draw it through the roller, so the picture (not the physics) is nudged.
  function heldNudge(b) {
    const s = b.s, k = s < 0.165 ? 0 : s < 0.205 ? (s - 0.165) / 0.04 : s <= R.sT ? 1 : Math.max(0, 1 - (s - R.sT) / sim.pathExtra(b)), e = k * k * (3 - 2 * k) * 0.0175, a = sim.yaw + sim.rob.th;
    return [Math.cos(a) * e, Math.sin(a) * e];
  }"""
rep(old_loop, new_loop)
rep("""seen.add(b.id); m.visible = b.state !== "out"; m.position.set(b.x, b.y, b.z);""",
    """seen.add(b.id); m.visible = b.state !== "out"; m.position.set(b.x, b.y, b.z);
      if (b.state === "held") { const o = heldNudge(b); m.position.x += o[0]; m.position.y += o[1]; }""")

# views: add Inspect, an orbit round the robot
rep('if (code === "KeyV") nextCam(); }', 'if (code === "KeyV") nextCam(); if (code === "KeyX") setXray(!xray); }')
rep('const cams = ["Driver", "Chase", "Top"];', 'const cams = ["Driver", "Chase", "Top", "Inspect"];')
rep("""$("camBtn").textContent = "View: " + cams[cam]; $("camBtn").onclick = e => { e.target.blur(); nextCam(); };""",
    """$("camBtn").textContent = "View: " + cams[cam]; $("camBtn").onclick = e => { e.target.blur(); nextCam(); };
  $("inspBtn").onclick = e => { e.target.blur(); cam = cams.indexOf("Inspect"); store.set("cam", "Inspect"); $("camBtn").textContent = "View: Inspect"; };
  const orb = { az: -2.2, el: 0.42, dist: 1.25, drag: null };
  stage.addEventListener("pointerdown", e => { if (cams[cam] !== "Inspect" || e.target !== renderer.domElement) return; orb.drag = [e.clientX, e.clientY]; try { renderer.domElement.setPointerCapture(e.pointerId); } catch (err) {} });
  stage.addEventListener("pointermove", e => { if (!orb.drag) return; orb.az -= (e.clientX - orb.drag[0]) * 0.008; orb.el = Math.max(0.05, Math.min(1.5, orb.el + (e.clientY - orb.drag[1]) * 0.006)); orb.drag = [e.clientX, e.clientY]; });
  for (const t of ["pointerup", "pointercancel"]) stage.addEventListener(t, () => { orb.drag = null; });
  stage.addEventListener("wheel", e => { if (cams[cam] !== "Inspect") return; e.preventDefault(); orb.dist = Math.max(0.55, Math.min(3.2, orb.dist * Math.exp(e.deltaY * 0.0012))); }, { passive: false });""")
rep("""    else { px = 0; py = -0.12; pz = 5.0; lx = 0; ly = 0; lz = 0; }""",
    """    else if (cams[cam] === "Inspect") { lx = r.x; ly = r.y; lz = 0.2; px = lx + orb.dist * Math.cos(orb.el) * Math.cos(orb.az); py = ly + orb.dist * Math.cos(orb.el) * Math.sin(orb.az); pz = lz + orb.dist * Math.sin(orb.el); }
    else { px = 0; py = -0.12; pz = 5.0; lx = 0; ly = 0; lz = 0; }""")
rep("""const k = firstCam ? 1 : Math.min(1, dt * (cams[cam] === "Chase" ? 5 : 3)); firstCam = false;""",
    """const k = firstCam ? 1 : Math.min(1, dt * (cams[cam] === "Inspect" ? 14 : cams[cam] === "Chase" ? 5 : 3)); firstCam = false;""")
rep("#stage canvas { display:block; width:100%; height:100%; }", "#stage canvas { display:block; width:100%; height:100%; touch-action:none; }")

open(out, "w").write(s)
print("wrote", out, len(s))
