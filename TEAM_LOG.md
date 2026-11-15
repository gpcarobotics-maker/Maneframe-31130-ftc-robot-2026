# FTC Robot — Team Log

**FTC team 31130, Rose City League, 2026-27 BIOBUZZ season.** The single source of truth for this robot's code. Read this first. Every change gets a line in the **Change log** at the bottom.

## 1. How the agents work together

- **Captain = "Claude A" in the Doc:** the Claude Code session in this folder (`~/Desktop/Robotics FTC`). It holds the code files and this log.
- **Doc agent = "Claude B" in the Doc:** a Claude agent on a **different computer (a Windows laptop) and a different account**. It can only be reached through the Google Doc, not by direct message, and it sees the code only through GitHub.
  - **Not Claude B:** the local Mac session titled "FTC tank drive robot code". That was a separate, third agent. It confirmed TeamCode and agreed to the rules earlier today, but it isn't Claude B.
- **GitHub repo:** https://github.com/gpcarobotics-maker/ftc-robot-2026 (private, team account `gpcarobotics-maker`). This TEAM_LOG.md lives at the **repo root** and is the master copy.
  - **GitHub accounts:** `gpcarobotics-maker` (team account, owns the repo) and `bored5644533` (a **team member**, confirmed by the team lead 2026-11-14).
  - **Merge rule** (agreed by both agents, 2026-11-14): wait for the other agent's AGREE before merging to main. If the team lead wants it merged sooner, write "review skipped, team lead OK" in the Doc's Decisions log.
  - **Doc ground rules:** one branch per change (`claude-a/...` or `claude-b/...`), every commit tagged `[Claude A]` or `[Claude B]`, `git pull` first, and merge only after it builds.
- **The agents talk IN the Google Doc** (the user's instruction, 2026-11-14), not by direct message: **"Agent chat for FTC ROBOTICS"**
  (https://docs.google.com/document/d/16yzDLhIDrrXNoFa-xa64l6UUgUqjrSV6gd48D3MoBtk/edit). It's titled "FTC Robot: Claude Agent Coordination".
  - **Sections:** Purpose, Ground rules, How we use this doc, Robot overview, Code and GitHub, Change proposals (table), Decisions log (table), Open questions.
  - **Sign every entry** with [Claude A] or [Claude B] and the date.
  - **Reading it:** the editor view hides the text from page-text tools. Open `.../document/d/<id>/mobilebasic` to read it as plain text.
  - **The Doc's ground rules also mention GitHub branches and commits,** but there's no git repo yet. That's an open question for the team lead.
- **Rule: no code change unless both agents agree.** The flow:
  1. One agent posts a **PROPOSAL** in the Doc: what to change, which file, and why.
  2. The other replies **AGREE** or **DISAGREE** with a reason.
  3. Only after AGREE does the captain make the change. Then the captain logs it here and posts **DONE** in the Doc.
  - The human team can always override.
- At an event, keep code changes to a minimum, and test in the pits before a match.

## 2. Current robot (the "basic bot")

- **Drive:** mecanum, 4 motors. Robot Configuration names `frontLeft`, `backLeft`, `frontRight`, `backRight`.
- **Intake:** REV **Core Hex** motor, named `intake`.
- **Feeder:** **continuous rotation servo**, named `servo`. Configure it as "Continuous Rotation Servo".
- **Flywheel:** REV **HD Hex** motor, named `flywheel`.
  - **1:1:** UltraPlanetary adapter plate only, **no cartridges**.
  - On the **Expansion Hub**. Its encoder cable must be in the encoder port with the same number as its power cable.
  - **Measured top speed about 4200 RPM** at full power (not the 6000 rated free speed).
- **Controllers:** two PS5 DualSense pads. Controller 1 is the driver, controller 2 the operator.

### Motor directions (confirmed working on the robot)

| Motor | Direction |
|---|---|
| frontLeft | FORWARD |
| backLeft | REVERSE |
| frontRight | FORWARD |
| backRight | REVERSE |
| intake | REVERSE |
| feeder (servo) | REVERSE |
| flywheel | FORWARD |

## 3. Code files

### Where the build project is

- **Android Studio project:** `~/AndroidStudioProjects/FtcRobotController-12.0`. It moved out of iCloud on 2026-11-13 because Gradle timed out there.
- **TeamCode folder:** `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`
- **What's in TeamCode as of 2026-11-14, ~10:00:**

  | File | OpMode | Matches the captain's copy? |
  |---|---|---|
  | `MecanumDrivePS5.java` | TeleOp "Mecanum Drive (PS5)" | **Yes, identical** |
  | `Auto.java` | Autonomous **"Auto"** (class `Auto`) | **Yes**, apart from the name. Timings updated to **0.7 / 0.8 / 0.8** on 2026-11-14 |
  | `readme.md` | — | — |

- `EncoderFinder.java` isn't in TeamCode. That's fine; it's only a test tool.
- The old `fullcode.java` (TeleOp "Creeper Mecanum Drive") is **gone**. It had wrong directions and no flywheel.

### What else is in the repo (not built onto the robot)

| Path | What it is |
|---|---|
| `TEAM_LOG.md` | This log, the master copy |
| `docs/BioBuzz Test Drive.html` | The team's robot sim of the **next** robot. Open it in Chrome. See section 5. |
| `docs/field-layout.png` | Labeled picture of the BIOBUZZ field: HIVEs, FLOWERs, GARDENs, LOADING ZONEs, alliance areas |
| `tools/EncoderFinder.java` | Test OpMode that shows which port an encoder signal arrives on. It's kept **outside TeamCode** so it isn't built. To use it, copy it into TeamCode (PROPOSAL first). |

### Captain's older copies (`~/Desktop/Robotics FTC` on the Mac; not in git)

| File | OpMode name | What it does |
|---|---|---|
| `MecanumDrivePS5.java` | TeleOp **"Mecanum Drive (PS5)"** | Mecanum drive, intake and feeder, and a PID flywheel with boost |
| `AutoOutAndBack.java` | Autonomous **"Auto: Out and Back"** | Timed: forward off the wall, strafe left, back into the human player (loading) zone |
| `EncoderFinder.java` | TeleOp **"Encoder Finder"** | Test tool: shows which port an encoder signal arrives on. It doesn't power any motor. |

### TeleOp controls

- **Controller 1 (driver):**
  - Left stick: drive and strafe
  - Right stick: turn
  - R1 (hold): slow mode (40%)
- **Controller 2 (operator):**
  - L2 (hold): **intake** in
  - L1 (hold): **intake** out
  - Cross / X (hold): **feeder servo** in
  - Circle / O (hold): **feeder servo** out
  - The intake and feeder run **separately** (since 2026-11-14; before that L2/L1 ran both together)
  - R2 (hold): flywheel to the target RPM
  - R1 (hold): flywheel backward at 0.5 power to unjam; overrides R2
  - D-pad up/down: target RPM from 1000 to 3000, in steps of 100 (starts at 2000)
  - Controller 2 rumbles when the flywheel reaches the target (within 75 RPM)

### Flywheel control (TeleOp)

- **Boost:** full power when more than 150 RPM below target; it hands over to the PID within 50 RPM.
- **PID:** the hub's built-in velocity PIDF.
  - F = 32767 / (4200 / 60 × 28) ≈ 16.7
  - **P = 0.5 × F**, I = 0.1 × P, D = 0
- **Tuning:**
  - If it sits below target, raise P's multiplier to 1.0.
  - If the RPM bounces up and down, lower it to 0.3.
- **Settings to keep:** `FLYWHEEL_ENCODER_NAME = "flywheel"`, `FLYWHEEL_ENCODER_REVERSED = false`, `TICKS_PER_REV = 28`, `MEASURED_MAX_RPM = 4200`.
- **Screen shows:** target, actual RPM, error (target − actual), status, last spin-up time, and battery voltage.

### Auto settings (tested at the scrimmage, 2026-11-14)

- **Step 1:** forward 0.4 power for **0.7 s**
- **Step 2:** strafe left 0.5 power for **0.8 s**
- **Step 3:** backward 0.4 power for **0.35 s**, only partway back (`ZONE_DIRECTION = -1`)
- 0.2 s pause between steps. After auto, the Driver Station preselects the TeleOp.
- **Start position:** robot's back against the alliance wall, facing into the field. The same auto works for red and blue (the field is mirrored).
- **✅ Confirmed working on the field (2026-11-14), and merged into main.**
- **The robot must end OFF the wall to score the point.** With a 0.8 s back-up it ended back on the wall and scored nothing.
- The auto is timed, so it goes shorter on a low battery. Run it on a charged battery.

## 4. Lessons learned (don't repeat these)

- **Pull before pushing.** Other people (Claude B, teammates) can push to GitHub too. If a push is rejected, use Git > Pull, or click **Merge** in Android Studio's "Push Rejected" window, then push again. Command-line git on the Mac has no GitHub login, so pulls and pushes go through Android Studio.

- **Drive:** stick up strafing meant one diagonal pair (backLeft and frontRight) was reversed. After fixing that, everything was inverted, so all four were flipped. The final directions are in the table above.
- **Flipping every direction at once** also reversed the flywheel. That jammed balls and stalled it. Change one mechanism at a time.
- **Flywheel RPM read 0:** the encoder cables were crossed with the intake Core Hex's at the hub. Fixed by putting each encoder in the encoder port with the same number as its own motor.
- **Swapped configuration names:** `flywheel` and `intake` were swapped once, so R2 ran the intake. Fixed in the Robot Configuration.
- **GEAR_RATIO:** briefly set to 8.35 by mistake, which made the RPM read about 240. It's 1:1; there's only an adapter plate.
- **The old PID stopped about 200 RPM short:** it assumed a 6000 RPM top speed. Using the measured 4200 RPM plus boost fixed it.
- **"Error" on the screen** is target − actual, not a speed.
- **Pasting:** select all and replace the whole file. Pasting inside the Android Studio class template causes "class or interface expected" and "Duplicate class" errors. The file name must match the class name.

## 5. Next robot (from the BioBuzz Test Drive sim, `~/Downloads/BioBuzz Test Drive.html`)

The team has all the parts but hasn't built it yet. The sim models:

- **Drive:** mecanum, HD Hex with UltraPlanetary 5:1 + 4:1 (about 1.2 m/s), with **field-centric** driving
- **Intake:** front flaps, servo with a 2.5:1 belt
- **Lift:** Core Hex bands, holds 2 POLLEN
- **Booster:** Core Hex roller that only runs while firing
- **Turret:** −220° to +127°
- **Hood:** 25–65°
- **Flywheel:** HD Hex with 3:1 and a 60T→24T step-up, up to about 5100 RPM, 90 mm wheel
- **Sensors:** Limelight 3A on the turret, odometry pods and the IMU
- **Auto aim:** solves turret, hood and RPM, including on the move

Sim controls (Xbox names):
- **Driver:** LT slow, Y field-centric toggle
- **Operator:**
  - RT intake, LT reverse
  - RB fire (hold), LB flywheel toggle
  - A auto aim
  - Right stick turret
  - D-pad up/down hood, left/right RPM

**Still needed to write its TeleOp:**
- Configuration names for the turret, hood, lift, booster and intake servo
- Whether the turret is a motor with an encoder or a servo
- The odometry type (goBILDA Pinpoint, or direct encoder ports)

The plan is manual turret and hood plus field-centric driving first, and auto aim after the parts are built and tested. **Not started; this waits until after the scrimmage.**

## 6. Open items

- [x] New auto timings (0.7 / 0.8 / 0.8) are in TeamCode `Auto.java`
- [x] Auto confirmed on the field with forward 0.7 / left 0.8 / back 0.35
- [x] Google Doc set up as the agent chat. Claude A filled in the Robot overview and Code sections, answered the open questions and added the first Decisions log row
- [x] GitHub repo created (private): https://github.com/gpcarobotics-maker/ftc-robot-2026
- [x] Pushed to GitHub (2026-11-14): `main` (default) and `claude-a/auto-stay-off-wall`. DONE posted in the Doc.
  - **How pushing works on the Mac:** Android Studio is signed in to GitHub, but command-line git on the Mac **is not**, so Claude A can't push. The team lead pushes with **⌘⇧K → Push** in Android Studio, which pushes the branch that's checked out. To push another branch, Claude A switches the checkout first (`git switch`), then switches back.
- [ ] Claude B clones the repo on the Windows laptop (it needs a GitHub sign-in as gpcarobotics-maker, for example GitHub Desktop) and confirms in the Doc.
- [x] The auto (back-up 0.35 s) worked on the field. `claude-a/auto-stay-off-wall` merged into main (fast-forward)
- [ ] After the scrimmage: Claude B's proposal to scale the auto drive times by battery voltage.
- [ ] Confirm the PID holds the target (Error near 0) and tune P if needed
- [ ] Find the best shooting RPM from the usual spot and write it here
- [ ] New robot TeleOp (section 5), after the scrimmage
- [ ] Build the new robot using the **BioBuzz Build Book** (https://claude.ai/artifact/73s526dSvjrwGTtVFrTdqW): 3D steps plus a shared checklist; each step unlocks when the last is fully checked

## 7. Change log

| Date | Who | Change |
|---|---|---|
| 2026-11-13 | Captain | Created the mecanum TeleOp for PS5 |
| 2026-11-13 | Captain | Added intake, flywheel and feeder. Feeder is a CR servo `servo`; the flywheel PID has D-pad RPM |
| 2026-11-13 | Captain | Moved L2, L1, R2 and the D-pad to controller 2 |
| 2026-11-13 | Captain | Fixed drive directions (diagonal pair, then all four flipped) |
| 2026-11-13 | Captain | Added `EncoderFinder.java`; the flywheel encoder can be read from any port |
| 2026-11-13 | Captain | Encoder fixed on the robot (cables were crossed); the RPM now reads correctly |
| 2026-11-13 | Captain | PID: switched to the hub's built-in velocity PID, then added full-power boost |
| 2026-11-13 | Captain | Measured top speed of 4200 RPM. PID F now uses 4200, P = 0.5F, range 1000–3000 |
| 2026-11-14 | Captain | TeleOp: controller 2 R1 spins the flywheel backward (0.5 power) |
| 2026-11-14 | Captain | Created `AutoOutAndBack.java` (timed). Changed it to: off wall, strafe left, back into the loading zone |
| 2026-11-14 | Captain | Auto times after the field test: forward 0.7 s, left 0.8 s, back 0.8 s |
| 2026-11-14 | Captain | Created this log and set up captain plus Doc agent collaboration |
| 2026-11-14 | Doc agent | AGREED to the rules. Reported fullcode.java as a conflict and proposed deleting it and copying the captain's files in |
| 2026-11-14 | Captain | Checked TeamCode: fullcode.java is already gone and the TeleOp matches. DISAGREED with the delete/copy (not needed). Auto.java still needs the new timings pasted. No files changed |
| 2026-11-14 | Doc agent | Rechecked TeamCode, confirmed it, and WITHDREW the delete/copy proposal |
| 2026-11-14 | Captain | At the user's request, set TeamCode `Auto.java` to OFF_WALL_SEC 0.7, STRAFE_LEFT_SEC 0.8, ZONE_SEC 0.8 (only those 3 lines). Not deployed yet |
| 2026-11-14 | Captain | The user said the Doc agent was confused. Sent it a full status recap (roles, robot, what's built, open items) and asked for "Clear" or the specific confusion |
| 2026-11-14 | Doc agent | Replied "Clear". It independently confirmed TeamCode (TeleOp, Auto at 0.7/0.8/0.8, readme). It's only waiting on the user's OK for the Google Doc |
| 2026-11-14 | Captain (Claude A) | The user said to talk in the Google Doc. Found it already structured. Filled in the Robot overview and Code and GitHub sections, answered Claude B's 2 open questions (no git repo; SDK 12.0.0, no path library), added a GitHub question for the team lead, and added the first Decisions log row. A direct message to Claude B failed because its session isn't running |
| 2026-11-14 | Claude B (in the Doc) | Said it's on a Windows laptop with no code. Pointed out the Decisions log wrongly credited it with the TeamCode check. Proposed a private repo plus a [Claude A] initial import |
| 2026-11-14 | Captain (Claude A) | At the team lead's request: `git init` in FtcRobotController-12.0 (existing SDK .gitignore, 115 files), first commit "[Claude A] Initial commit" with TEAM_LOG.md at the root. Created the private repo gpcarobotics-maker/ftc-robot-2026 on github.com. **The push failed: no GitHub login saved on the Mac.** |
| 2026-11-14 | Captain (Claude A) | In the Doc: AGREE to Claude B's repo proposal, posted the repo URL and clone steps, corrected the Decisions log (the TeamCode check was the separate Mac session), and added a Decisions log row for the repo. TEAM_LOG.md at the repo root is now the master copy |
| 2026-11-14 | Captain (Claude A) | The team lead said "get everything on the github" before a match. Added `docs/BioBuzz Test Drive.html`, `docs/field-layout.png` and `tools/EncoderFinder.java` (outside TeamCode, so robot code is unchanged). Push still waiting on a GitHub sign-in on the Mac |
| 2026-11-14 | Captain (Claude A) | Field test: the auto ended back ON the wall, which scores no point. The team lead chose "back up only halfway": `Auto.java` ZONE_SEC 0.8 → 0.35. On branch `claude-a/auto-stay-off-wall`; merge to main after it works on the field |
| 2026-11-14 | Claude B (in the Doc) | AGREE on `claude-a/auto-stay-off-wall` as a scrimmage fix: test it on a charged battery before merging. Its Windows laptop isn't signed in to GitHub either. Plans a later proposal to scale auto drive times by battery voltage |
| 2026-11-14 | Captain (Claude A) | Comments-only update to Auto.java on the branch. The team lead said "push this to github". **The push failed again (no GitHub sign-in on the Mac).** Posted the status and the sign-in steps in the Doc |
| 2026-11-14 | Team lead + Captain (Claude A) | The team lead signed in to GitHub in Android Studio and pushed `main`, then `claude-a/auto-stay-off-wall` (Claude A switched the checkout between pushes). Checked on github.com: main is the default, and the branch is 3 commits ahead. Posted DONE and clone steps in the Doc |
| 2026-11-14 | Team lead + Captain (Claude A) | The team lead reported "the auto worked". Merged `claude-a/auto-stay-off-wall` into main (fast-forward; Claude B had agreed, conditional on a field test). Auto settings marked confirmed |
| 2026-11-14 | Captain (Claude A) | The team lead asked for the intake and feeder to run separately. TeleOp: intake stays on controller 2 L2 (in) / L1 (out), the feeder servo moves to Cross (in) / Circle (out), and the flywheel stays on R2/R1. Branch `claude-a/split-intake-servo`. Not tested on the robot yet; merge after it works |
| 2026-11-14 | Team lead + Captain (Claude A) | The team lead reported the intake/servo split "works" on the robot. Merged `claude-a/split-intake-servo` into main (fast-forward). Claude B's code review is still welcome after the fact |
| 2026-11-14 | Team lead + Captain (Claude A) | The push was rejected: GitHub main had an extra commit `c20d3a8` from account **bored5644533** (one comment line at the top of README.md, untagged, straight to main). The team lead merged it in Android Studio (merge commit `e9e2429`) and pushed. main on GitHub = the Mac |
| 2026-11-14 | Team lead | Confirmed `bored5644533` is a team member |
| 2026-11-14 | Claude B (in the Doc) | Reviewed the intake/servo split after the fact: AGREE. Proposed the merge rule above; Claude A agreed |
| 2026-11-14 | Team lead | First scrimmage finished. Team 31130, Rose City League. Claude B is unavailable for now (just Claude A and the team lead). Added a "Claude A context" tab to the Google Doc as a full project summary |
| 2026-11-14 | Captain (Claude A) | Team decided to rebuild the robot like the BioBuzz sim (goBILDA pods + Pinpoint, hex motor 3:1, Limelight). Published the BioBuzz Build Book artifact: "Before you build" checklist + 13 steps with 3D from the sim's CAD, shared checks |
