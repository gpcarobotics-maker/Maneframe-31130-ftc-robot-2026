"""Rebuild the BioBuzz sim robot from robotData.json and write STL files (mm) for Onshape.

Placement matches the sim's three.js scene (view3d.js): every mesh is in mm; hood and
flywheel meshes sit at the hood axis ax, roller meshes at rollerAxis, and the whole model
sits at (0, turretY, turretZ) = (0, -21, 130) mm in the robot frame. Robot frame: floor
z = 0, centre (0, 0), front -Y, +X right, +Z up. Turret at 0 deg (shooter toward +X),
hood at 25 deg.
"""
import base64, json, struct, sys, os
import numpy as np

src, out = sys.argv[1], sys.argv[2]
d = json.load(open(src))
ax, ra = d["ax"], d["rollerAxis"]
OFF = {"chassis": (0, 0, 0), "fixed": (0, 0, 0), "turret": (0, 0, 0),
       "hood": (ax[0], 0, ax[1]), "wheel": (ax[0], 0, ax[1]), "roller": (0, ra[0], ra[1])}
BASE = np.array([0, -21.0, 130.0])
# Onshape-friendly sections: what moves together
SECTION = {"chassis": "1_chassis", "fixed": "2_turret_base", "roller": "3_intake",
           "turret": "4_turret", "wheel": "5_flywheel", "hood": "6_hood"}

def unpack(m):
    a = np.frombuffer(base64.b64decode(m["d"]), dtype="<u2").astype(np.float64).reshape(-1, 3)
    return np.asarray(m["o"]) + a * np.asarray(m["s"])

parts = []
for i, q in enumerate(d["meshes"]):
    v = unpack(q["m"]) + np.asarray(OFF[q["group"]]) + BASE
    parts.append((i, q["group"], q["color"], v.reshape(-1, 3, 3)))

def write_stl(path, tris, name):
    n = np.cross(tris[:, 1] - tris[:, 0], tris[:, 2] - tris[:, 0])
    L = np.linalg.norm(n, axis=1, keepdims=True); L[L == 0] = 1; n = n / L
    rec = np.zeros(len(tris), dtype=[("n", "<f4", 3), ("v", "<f4", (3, 3)), ("a", "<u2")])
    rec["n"] = n; rec["v"] = tris
    with open(path, "wb") as f:
        f.write(name.encode()[:80].ljust(80, b" ")); f.write(struct.pack("<I", len(tris))); f.write(rec.tobytes())

os.makedirs(out, exist_ok=True)
allt = []
for g, sec in SECTION.items():
    t = np.concatenate([p[3] for p in parts if p[1] == g])
    write_stl(os.path.join(out, f"BioBuzz_{sec}.stl"), t, f"BioBuzz {sec} (mm)")
    allt.append(t)
    lo, hi = t.reshape(-1, 3).min(0), t.reshape(-1, 3).max(0)
    print(f"{sec:14} {len(t):7} tris  x {lo[0]:7.1f}..{hi[0]:6.1f}  y {lo[1]:7.1f}..{hi[1]:6.1f}  z {lo[2]:6.1f}..{hi[2]:6.1f}")
allt = np.concatenate(allt)
write_stl(os.path.join(out, "BioBuzz_robot_whole.stl"), allt, "BioBuzz robot (mm)")
lo, hi = allt.reshape(-1, 3).min(0), allt.reshape(-1, 3).max(0)
print(f"{'WHOLE':14} {len(allt):7} tris  size {hi[0]-lo[0]:.1f} x {hi[1]-lo[1]:.1f} x {hi[2]-lo[2]:.1f} mm, z from {lo[2]:.1f}")

# OBJ + MTL with the sim's colours, one object per CAD part (for the preview, and for apps that read colour)
with open(os.path.join(out, "BioBuzz_robot_color.mtl"), "w") as m:
    for c in sorted({p[2] for p in parts}):
        r, g, b = (int(c[k:k + 2], 16) / 255 for k in (1, 3, 5))
        m.write(f"newmtl c{c[1:]}\nKd {r:.3f} {g:.3f} {b:.3f}\n\n")
with open(os.path.join(out, "BioBuzz_robot_color.obj"), "w") as o:
    o.write("# BioBuzz robot, mm, front -Y, up +Z\nmtllib BioBuzz_robot_color.mtl\n")
    base = 1
    for i, g, c, t in parts:
        o.write(f"o {SECTION[g]}_{c[1:]}_{i:02d}\nusemtl c{c[1:]}\n")
        o.write("".join(f"v {x:.2f} {y:.2f} {z:.2f}\n" for x, y, z in t.reshape(-1, 3)))
        o.write("".join(f"f {base + 3*k} {base + 3*k + 1} {base + 3*k + 2}\n" for k in range(len(t))))
        base += 3 * len(t)
