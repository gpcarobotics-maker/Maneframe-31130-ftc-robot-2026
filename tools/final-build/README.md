# BioBuzz Final Build (source)

Live page: https://claude.ai/artifact/Xb9AfAQjssNirSPoWULm61

The team's Test Drive sim (`docs/BioBuzz Test Drive.html`) with three view-only changes. The physics is untouched.

- `robits.js`: the AndyMark Robits 5.5 in. ID turntable (am-5470), drawn in three.js from the sizes in AndyMark's STEP file. Also used by the Build Book.
- `build_final.py`: patches the sim into the Final Build page.

```
python3 build_final.py "../../docs/BioBuzz Test Drive.html" robits.js index.html
```

What the patch does: swaps the CAD's generic turntable for the Robits one, adds an X-ray switch (see balls move through the robot) and an Inspect view (orbit the robot). The page needs the `downloads` capability for "save a copy".
