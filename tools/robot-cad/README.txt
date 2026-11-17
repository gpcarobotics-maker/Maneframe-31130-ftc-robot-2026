BioBuzz robot 3D files for Onshape (updated 2026-11-16)

All files are in MILLIMETERS and share one origin: the robot's center on the floor.
+X is the robot's right, -Y is the front, +Z is up. The turret is drawn facing the robot's right, hood at 25 degrees.

START HERE
  BioBuzz_real_parts.step        121 MB  Every bought part that has a maker's STEP file, placed where the CAD has it.
                                         22 parts, each named (for example "Drive motor, front left"), with the makers' colors.
  BioBuzz_real_parts_lite.step    67 MB  The same, with only each part's outer bodies. Use it if the full one is slow.
  BioBuzz_custom_parts.stl        6 MB   Everything you make yourself (CNC and 3D printed), in the same position.

IN ONSHAPE
  1. Open a Document. Click "+" at the bottom left, then Import.
  2. Pick BioBuzz_real_parts_lite.step. If it asks, choose "Import to a single document" and keep Y-up OFF (the file is Z-up).
  3. Import BioBuzz_custom_parts.stl the same way. If it asks for units, choose Millimeter.
  4. Make an Assembly, click Insert, pick both, and click the green check without clicking in the picture. They land on the origin and line up.

WHAT'S IN THE STEP (maker's part number, how many)
  REV-41-1600  HD Hex Motor with UltraPlanetary Gearbox  5   (4 drive, 1 flywheel)
  REV-41-1656 / REV-41-1657  75 mm Mecanum Wheel         2 + 2
  REV-31-1595  Control Hub                               1
  REV-31-1153  Expansion Hub                             1
  REV-31-1302  12V Slim Battery                          1
  REV-31-1387  Switch                                    1
  REV-41-3334  Smart Robot Servo V2                      2   (intake, hood)
  REV-41-1300  Core Hex Motor                            3   (lift, booster, turret)
  goBILDA 3110-0001-0001  Swingarm Odometry Pod          2
  goBILDA 3110-0002-0001  Pinpoint                       1
  AndyMark am-5470  Robits 5.5 in. ID Turntable          1

THINGS TO KNOW
  - The turret motor's spot is my proposal: over the right drive pod. The CAD's old spot doesn't work with the real turntable.
  - The flywheel motor is drawn with the full gearbox kit, so it's longer than the real one (which uses one 3:1 cartridge).
  - Wheels: left-hand ones are at front left and rear right. That's a guess. Seen from above the rollers must make an X.
  - Odometry pods may be upside down or turned end for end. Flip them in Onshape if the wheel isn't on the floor.
  - Not in the STEP (no maker's STEP link I could confirm): Limelight 3A, the 50-tooth pinion and its hex adapter,
    the 90 mm flywheel wheels, the 60T/24T gears, pulleys, belts, shafts, bearings and screws. The Limelight and flywheel
    wheels are still in the custom-parts STL as the CAD drew them.
  - The custom-parts STL is a triangle mesh: good for looking and measuring, not for editing.

OTHER FILES
  real-parts/                     the makers' STEP files, one per part, as downloaded
  am-5470 Robits Turntable (AndyMark).STEP   AndyMark's own file for the turntable
  preview_real_parts.png, preview_real_and_custom.png   what the result should look like
  BioBuzz_robot_whole.stl, BioBuzz_1..6_*.stl, BioBuzz_robot_color.obj   the first export: the whole CAD as a mesh,
                                  with the CAD's generic turntable and stand-in motors. Kept for reference.
  robotData.json                  the CAD meshes from the Test Drive sim

MADE BY
  tools/robot-cad in the robot repo: assemble.py (needs: pip install cadquery-ocp), custom_parts.py, export_robot.py
  Part numbers, STEP links and the design problems: https://claude.ai/artifact/HtVqgeSpsn3mLcW48VT9JT
  Drive the robot: https://claude.ai/artifact/Xb9AfAQjssNirSPoWULm61
