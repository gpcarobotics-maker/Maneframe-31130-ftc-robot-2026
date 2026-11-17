"""Write the robot's custom parts (CNC and 3D printed) as one STL in mm, in the same frame as BioBuzz_real_parts.step.
Everything that is a bought part in the CAD is left out, because the real STEP files replace it:
  drive pods' wheels and motors (mesh 15, outside |x| = 100), Control Hub (7 to 14), Expansion Hub, pods and Pinpoint (17),
  battery and switch (4), lift motor and intake servo (18), turret motors (42, 43), hood servo (46),
  and the generic turntable (26, 27, 28, 44, 45, 49, the 213 mm gear in 30, the stop finger in 31).
"""
import base64, json, struct, sys
import numpy as np
src, out = sys.argv[1], sys.argv[2]
d = json.load(open(src)); ax, ra = d["ax"], d["rollerAxis"]
OFF = {"chassis": (0, 0, 0), "fixed": (0, 0, 0), "turret": (0, 0, 0), "hood": (ax[0], 0, ax[1]), "wheel": (ax[0], 0, ax[1]), "roller": (0, ra[0], ra[1])}
DROP = {4, 7, 8, 9, 10, 11, 12, 13, 14, 17, 18, 26, 27, 28, 42, 43, 44, 45, 46, 49}
CUT = {30: (0, 6584), 31: (5984, 6354)}
keep = []
for i, q in enumerate(d["meshes"]):
    if i in DROP: continue
    m = q["m"]; a = np.frombuffer(base64.b64decode(m["d"]), dtype="<u2").astype(np.float64).reshape(-1, 3)
    t = (np.asarray(m["o"]) + a * np.asarray(m["s"]) + np.asarray(OFF[q["group"]]) + np.array([0, -21.0, 130.0])).reshape(-1, 3, 3)
    if i in CUT: lo, hi = CUT[i]; t = np.concatenate([t[:lo], t[hi:]])
    if i == 15: t = t[(np.abs(t[:, :, 0]) < 100).all(axis=1)]
    keep.append(t)
t = np.concatenate(keep)
n = np.cross(t[:, 1] - t[:, 0], t[:, 2] - t[:, 0]); L = np.linalg.norm(n, axis=1, keepdims=True); L[L == 0] = 1
rec = np.zeros(len(t), dtype=[("n", "<f4", 3), ("v", "<f4", (3, 3)), ("a", "<u2")]); rec["n"] = n / L; rec["v"] = t
with open(out, "wb") as f: f.write(b"BioBuzz custom parts (mm)".ljust(80, b" ")); f.write(struct.pack("<I", len(t))); f.write(rec.tobytes())
lo, hi = t.reshape(-1, 3).min(0), t.reshape(-1, 3).max(0)
print("wrote", out, len(t), "triangles; box", lo.round(1), hi.round(1))
