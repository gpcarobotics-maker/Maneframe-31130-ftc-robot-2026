"""Place the makers' STEP files where the robot's CAD has each part and write one STEP assembly for Onshape.

Robot frame, mm: origin = robot centre on the floor, +X right, -Y front, +Z up. Turret drawn facing +X (CAD pose), hood at 25 degrees.
Each part is turned by its own bounding box: its longest side goes along L, its shortest along S. A motor's shaft end is found
by which end of the box is thinner. Needs OpenCascade: pip install cadquery-ocp.

    python assemble.py <folder with the STEP files> <turntable STEP> <output.step>
"""
import sys, os, time
import numpy as np
from OCP.STEPControl import STEPControl_Reader, STEPControl_AsIs
from OCP.STEPCAFControl import STEPCAFControl_Writer, STEPCAFControl_Reader
from OCP.TDF import TDF_LabelSequence
from OCP.XCAFDoc import XCAFDoc_ShapeTool
from OCP.Bnd import Bnd_Box
from OCP.BRepBndLib import BRepBndLib
from OCP.BRep import BRep_Tool
from OCP.TopExp import TopExp_Explorer
from OCP.TopAbs import TopAbs_VERTEX, TopAbs_SOLID, TopAbs_SHELL, TopAbs_FACE
from OCP.BRep import BRep_Builder
from OCP.TopoDS import TopoDS_Compound
from OCP.TopoDS import TopoDS
from OCP.gp import gp_Trsf
from OCP.TopLoc import TopLoc_Location
from OCP.TDocStd import TDocStd_Document
from OCP.TCollection import TCollection_ExtendedString
from OCP.XCAFDoc import XCAFDoc_DocumentTool, XCAFDoc_ColorGen
from OCP.TDataStd import TDataStd_Name
from OCP.Quantity import Quantity_Color, Quantity_TOC_RGB
from OCP.Interface import Interface_Static

folder, turntable, out = sys.argv[1], sys.argv[2], sys.argv[3]
LITE = int(sys.argv[4]) if len(sys.argv) > 4 else 0   # lite: keep only each part's biggest bodies, up to this many faces per part
X, Y, Z = np.eye(3)
doc = TDocStd_Document(TCollection_ExtendedString("XmlXCAF"))
st = XCAFDoc_DocumentTool.ShapeTool_s(doc.Main()); ct = XCAFDoc_DocumentTool.ColorTool_s(doc.Main())
def free():
    q = TDF_LabelSequence(); st.GetFreeShapes(q); return [q.Value(i) for i in range(1, q.Length() + 1)]

def bbox(sh):
    b = Bnd_Box(); BRepBndLib.Add_s(sh, b, False); x0, y0, z0, x1, y1, z1 = b.Get()
    return np.array([x0, y0, z0]), np.array([x1, y1, z1])

class Part:
    def __init__(self, path):
        t = time.time()
        if LITE: r = STEPControl_Reader(); r.ReadFile(path); r.TransferRoots(); self.shape = r.OneShape()
        else:
            n0 = len(free()); r = STEPCAFControl_Reader(); r.SetColorMode(True); r.SetNameMode(True); r.ReadFile(path); r.Transfer(doc)
            roots = free()[n0:]; assert len(roots) == 1, (path, len(roots))
            self.label = roots[0]; self.shape = XCAFDoc_ShapeTool.GetShape_s(self.label)
        if LITE:   # outer bodies only: biggest first, until the face budget is spent. Loses the part's own colours and inner detail.
            bodies = []
            for kind, avoid in ((TopAbs_SOLID, None), (TopAbs_SHELL, TopAbs_SOLID)):
                e = TopExp_Explorer(self.shape, kind) if avoid is None else TopExp_Explorer(self.shape, kind, avoid)
                while e.More():
                    b = e.Current(); lo, hi = bbox(b); nf = 0; fe = TopExp_Explorer(b, TopAbs_FACE)
                    while fe.More(): nf += 1; fe.Next()
                    bodies.append((float(np.linalg.norm(hi - lo)), nf, b)); e.Next()
            bodies.sort(key=lambda t: -t[0]); comp = TopoDS_Compound(); bb = BRep_Builder(); bb.MakeCompound(comp); used = 0; kept = 0
            for diag, nf, b in bodies:
                if kept >= 2 and used + nf > LITE: continue
                bb.Add(comp, b); used += nf; kept += 1
            self.shape = comp; self.label = st.AddShape(comp, False); self.lite = (kept, len(bodies), used)
        self.lo, self.hi = bbox(self.shape); self.size = self.hi - self.lo; self.ctr = (self.lo + self.hi) / 2
        order = np.argsort(self.size); self.iS, self.iM, self.iL = order
        pts = []; e = TopExp_Explorer(self.shape, TopAbs_VERTEX)
        while e.More(): p = BRep_Tool.Pnt_s(TopoDS.Vertex_s(e.Current())); pts.append((p.X(), p.Y(), p.Z())); e.Next()
        self.pts = np.array(pts)
        print(f"read {os.path.basename(path)[:44]:44} {self.size.round(1)}  {time.time()-t:.0f}s", flush=True)
    def thin_end(self, axis):
        """+1 or -1: which end of this axis has the smaller cross-section (a shaft sticks out there)."""
        o = [k for k in range(3) if k != axis]; span = []
        for sgn in (-1, 1):
            edge = self.hi[axis] if sgn > 0 else self.lo[axis]
            m = np.abs(self.pts[:, axis] - edge) < 0.06 * self.size[axis]
            q = self.pts[m][:, o]; span.append(np.ptp(q, axis=0).max() if len(q) else 0)
        return -1 if span[0] < span[1] else 1
    def solids(self):
        e = TopExp_Explorer(self.shape, TopAbs_SOLID); res = []
        while e.More(): res.append(bbox(e.Current())); e.Next()
        return res

def placement(part, centre, L, S, sL=1, sS=1, anchor=None):
    """Rotation that sends the part's long axis (sign sL) to L and short axis (sign sS) to S; then puts `anchor` (default the box centre) at `centre`."""
    a = np.eye(3)[part.iL] * sL; c = np.eye(3)[part.iS] * sS; b = np.cross(c, a)
    A = np.asarray(L, float); C = np.asarray(S, float); B = np.cross(C, A)
    R = np.column_stack([A, B, C]) @ np.column_stack([a, b, c]).T
    t = np.asarray(centre, float) - R @ (part.ctr if anchor is None else anchor)
    tr = gp_Trsf(); tr.SetValues(R[0, 0], R[0, 1], R[0, 2], t[0], R[1, 0], R[1, 1], R[1, 2], t[1], R[2, 0], R[2, 1], R[2, 2], t[2])
    return tr

f = lambda n: os.path.join(folder, n)
P = {
    "drive": Part(f("REV-41-1600.STEP")), "wheelL": Part(f("REV-41-1656.STEP")), "wheelR": Part(f("REV-41-1657.STEP")),
    "chub": Part(f("REV-31-1595.STEP")), "ehub": Part(f("REV-31-1153.STEP")), "batt": Part(f("REV-31-1302.STEP")), "switch": Part(f("REV-31-1387.STEP")),
    "servo": Part(f("REV-41-3334.STEP")), "core": Part(f("REV-41-1300.STEP")),
    "pod": Part(f("3110-0001-0001 goBILDA Swingarm Odometry Pod.step")), "pin": Part(f("3110-0002-0001 goBILDA Pinpoint.step")),
    "table": Part(turntable),
}
NAME = {"drive": "REV-41-1600 HD Hex Motor with UltraPlanetary Gearbox", "wheelL": "REV-41-1656 75mm Mecanum Wheel", "wheelR": "REV-41-1657 75mm Mecanum Wheel",
        "chub": "REV-31-1595 Control Hub", "ehub": "REV-31-1153 Expansion Hub", "batt": "REV-31-1302 12V Slim Battery", "switch": "REV-31-1387 Switch",
        "servo": "REV-41-3334 Smart Robot Servo V2", "core": "REV-41-1300 Core Hex Motor", "pod": "goBILDA 3110-0001-0001 Swingarm Odometry Pod",
        "pin": "goBILDA 3110-0002-0001 Pinpoint", "table": "AndyMark am-5470 Robits Turntable"}
COLOR = {"drive": (.25, .26, .28), "wheelL": (.55, .57, .6), "wheelR": (.55, .57, .6), "chub": (.9, .45, .1), "ehub": (.9, .45, .1), "batt": (.12, .12, .13),
         "switch": (.15, .15, .16), "servo": (.2, .2, .22), "core": (.3, .3, .32), "pod": (.2, .22, .25), "pin": (.1, .35, .2), "table": (.7, .72, .75)}

# the motor's shaft end, and the turntable's plate side
dr = P["drive"]; dr_s = dr.thin_end(dr.iL)
tb = P["table"]; plate = min(tb.solids(), key=lambda s: abs((s[1] - s[0])[tb.iS] - 5.59) + abs(np.sort(s[1] - s[0])[2] - 254))
plate_ctr = (plate[0] + plate[1]) / 2; plate_side = np.sign(plate_ctr[tb.iS] - tb.ctr[tb.iS]) or 1
plate_top = plate_ctr.copy(); plate_top[tb.iS] -= plate_side * 2.795          # the plate face that the bearing sits on
print("drive shaft end:", dr_s, "| turntable plate side:", plate_side, "| plate box", (plate[1] - plate[0]).round(2))

INST = [  # key, label, transform
    ("drive", "Drive motor, front left",  placement(dr, (-167.5, -77.5, 37.5), -Y, X, dr_s)),
    ("drive", "Drive motor, front right", placement(dr, (167.5, -77.5, 37.5), -Y, X, dr_s)),
    ("drive", "Drive motor, rear left",   placement(dr, (-167.5, 52.5, 37.5), Y, X, dr_s)),
    ("drive", "Drive motor, rear right",  placement(dr, (167.5, 52.5, 37.5), Y, X, dr_s)),
    ("drive", "Flywheel motor (on the turret)", placement(dr, (137, -31, 214), -Y, X, dr_s)),
    ("wheelL", "Wheel, front left",  placement(P["wheelL"], (-168, -176, 37.5), Z, X)),
    ("wheelL", "Wheel, rear right",  placement(P["wheelL"], (168, 176, 37.5), Z, X)),
    ("wheelR", "Wheel, front right", placement(P["wheelR"], (168, -176, 37.5), Z, X)),
    ("wheelR", "Wheel, rear left",   placement(P["wheelR"], (-168, 176, 37.5), Z, X)),
    ("chub", "Control Hub", placement(P["chub"], (-113, 125, 87), Y, Z)),
    ("ehub", "Expansion Hub", placement(P["ehub"], (113, 125, 87), Y, Z)),
    ("batt", "Battery", placement(P["batt"], (0, 124, 82), Y, Z)),
    ("switch", "Power switch", placement(P["switch"], (196, 190, 106), Z, X)),
    ("servo", "Intake servo", placement(P["servo"], (-99, -161, 73), Y, Z)),
    ("servo", "Hood servo (on the turret)", placement(P["servo"], (44, 86, 243), Z, X)),
    ("core", "Lift motor", placement(P["core"], (-97, -53, 56), Z, X)),
    ("core", "Booster motor (on the turret)", placement(P["core"], (-31, 69, 239), Z, X)),
    ("core", "Turret motor (proposed spot)", placement(P["core"], (153, -62, 100), Y, Z)),
    ("pod", "Odometry pod, forward", placement(P["pod"], (-20, 139, 25), Y, X)),
    ("pod", "Odometry pod, strafe", placement(P["pod"], (52, 145, 25), X, Y)),
    ("pin", "Pinpoint", placement(P["pin"], (52, 145, 60), Y, Z)),
    ("table", "Turntable", placement(tb, (0, -21, 130), X, Z, 1, -plate_side, anchor=plate_top)),
]

top = st.NewShape(); TDataStd_Name.Set_s(top, TCollection_ExtendedString("BioBuzz robot, real parts"))
lab = {}
for k, p in P.items():
    l = p.label; TDataStd_Name.Set_s(l, TCollection_ExtendedString(NAME[k])); lab[k] = l
    if LITE: ct.SetColor(l, Quantity_Color(*COLOR[k], Quantity_TOC_RGB), XCAFDoc_ColorGen); print(f"lite {k:7} kept {p.lite[0]:3} of {p.lite[1]:3} bodies, {p.lite[2]} faces")
for k, label, tr in INST:
    c = st.AddComponent(top, lab[k], TopLoc_Location(tr)); TDataStd_Name.Set_s(c, TCollection_ExtendedString(label))
st.UpdateAssemblies()
# report where each one ended up
for k, label, tr in INST:
    lo, hi = bbox(P[k].shape.Moved(TopLoc_Location(tr)))
    print(f"{label:34} x {lo[0]:7.1f}..{hi[0]:6.1f}  y {lo[1]:7.1f}..{hi[1]:6.1f}  z {lo[2]:6.1f}..{hi[2]:6.1f}")
t = time.time(); Interface_Static.SetCVal_s("write.step.schema", "AP214IS"); Interface_Static.SetCVal_s("write.step.unit", "MM"); Interface_Static.SetIVal_s("write.step.assembly", 1); Interface_Static.SetIVal_s("write.surfacecurve.mode", 0)   # keep repeated parts as instances of one shape
w = STEPCAFControl_Writer(); w.SetNameMode(True); w.SetColorMode(True); w.Transfer(doc, STEPControl_AsIs); stat = w.Write(out)
print("wrote", out, os.path.getsize(out) // 1_000_000, "MB", f"{time.time()-t:.0f}s", stat)
