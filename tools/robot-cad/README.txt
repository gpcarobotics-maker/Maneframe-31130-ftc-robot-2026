BioBuzz robot 3D model (from the BioBuzz Test Drive sim's CAD), for Onshape

All files are in MILLIMETERS. Robot center is at (0,0), floor at z=0, front faces -Y, up is +Z.
Size: 454 x 440 x 447 mm. Turret faces the robot's right (+X), hood at 25 degrees.

EASIEST: import BioBuzz_robot_whole.stl (the whole robot, one gray mesh).
  Onshape: open a Document > "+" at the bottom left > Import > pick the file.
  If it asks for units, choose Millimeter.

IN SECTIONS (so you can color them or move the turret):
  BioBuzz_1_chassis.stl      frame, wheels, electronics, lift
  BioBuzz_2_turret_base.stl  deck plate, turntable ring, turret motor
  BioBuzz_3_intake.stl       intake shaft and flaps
  BioBuzz_4_turret.stl       turret body, Limelight, motors on the turret
  BioBuzz_5_flywheel.stl     flywheel and its gear
  BioBuzz_6_hood.stl         hood
  All sections share the same origin, so they line up when placed at the origin.

WITH COLORS: BioBuzz_robot_color.obj (+ .mtl). Use it if your app reads OBJ colors.

It's a triangle mesh: good for looking, measuring and checking fit, not for editing features.
Made by export_robot.py (tools/robot-cad in the robot repo) from robotData.json.
