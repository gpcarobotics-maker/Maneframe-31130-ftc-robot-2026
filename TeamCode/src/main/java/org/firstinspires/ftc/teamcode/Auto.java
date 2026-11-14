package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/**
 * AUTO FULL: spin up the flywheel while leaving the wall, shoot the preloaded balls,
 * then do the same park as "Auto" (strafe left, back up partway, end OFF the wall).
 *
 * File must be named AutoFull.java and live in:
 *   TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
 *
 * The old "Auto" OpMode is NOT changed. It is confirmed working on the field, so keep it as the
 * fallback. Pick "Auto Full" on the Driver Station only after testing it on the practice field.
 *
 * Sequence:
 *   0. (INIT)  Driver 1 picks the start delay and whether to shoot. See the INIT MENU below.
 *   1. Flywheel starts spinning up (full-power boost, then hub PID) and STAYS spinning for the
 *      whole drive, so it is ready by the time the robot stops.
 *   2. Drive FORWARD off the wall.
 *   3. SHOOT: for each shot, wait until the flywheel is at speed, pulse the feeder, let the RPM
 *      recover. If it never reaches speed it fires anyway after a timeout so auto never stalls.
 *   4. Flywheel off. Strafe LEFT. Back up PARTWAY (ends off the wall = the parking point).
 *
 * INIT MENU (controller 1, before pressing PLAY):
 *   D-pad UP / DOWN  -> start delay +1 s / -1 s (0 to 10 s), to stay out of a partner's way
 *   Cross (X)        -> turn shooting ON / OFF (OFF = same drive as the old "Auto")
 *
 * Uses the same Robot Configuration names and motor directions as "Mecanum Drive (PS5)":
 *   frontLeft, backLeft, frontRight, backRight, intake, servo (feeder), flywheel
 *
 * It is TIMED (no encoders for driving), so distances depend on battery charge.
 * Run it on a charged battery. After it finishes the Driver Station loads "Mecanum Drive (PS5)".
 */
@Autonomous(name = "Auto Full", group = "Auto", preselectTeleOp = "Mecanum Drive (PS5)")
public class AutoFull extends LinearOpMode {

    // =====================================================================
    // DRIVE (same numbers as the field-tested "Auto": 0.7 / 0.8 / 0.35)
    // =====================================================================
    private static final double OFF_WALL_POWER   = 0.4;
    private static final double OFF_WALL_SEC     = 0.7;

    private static final double STRAFE_POWER     = 0.5;
    private static final double STRAFE_LEFT_SEC  = 0.8;

    private static final double ZONE_POWER       = 0.4;
    private static final double ZONE_SEC         = 0.35; // only about halfway back, so we end OFF the wall
    // -1 = BACK toward the wall (zone is against the wall). Change to 1 if the zone is in FRONT.
    private static final int    ZONE_DIRECTION   = -1;

    private static final double PAUSE_SEC        = 0.2;  // short stop between moves

    // Battery compensation. The timings above were tuned at whatever voltage the battery had
    // that day. Leave this false to behave exactly like "Auto". Set it true (after measuring a
    // good NOMINAL_VOLTAGE on the practice field) to scale drive power up when the battery is low.
    private static final boolean COMPENSATE_BATTERY = false;
    private static final double  NOMINAL_VOLTAGE    = 13.0; // battery voltage the timings were tuned at

    // =====================================================================
    // SHOOTING  (TUNE THESE ON THE PRACTICE FIELD)
    // =====================================================================
    private static final boolean SHOOT_DEFAULT_ON  = true;  // starting value of the INIT toggle

    // The best RPM from the spot where the robot stops is still an open item in TEAM_LOG.md.
    // 2000 is the TeleOp's starting RPM. Replace it with the RPM that scores from this spot.
    private static final double  SHOOT_RPM         = 2000.0;
    private static final int     SHOT_COUNT        = 2;     // how many balls are preloaded
    private static final double  FEEDER_POWER      = 1.0;   // same as TeleOp, + = feed toward flywheel
    private static final double  FEED_PULSE_SEC    = 0.6;   // how long the feeder runs per ball
    private static final double  SPINUP_TIMEOUT_SEC   = 3.0; // max wait for READY before the first shot
    private static final double  RECOVER_TIMEOUT_SEC  = 1.5; // max wait for READY between shots
    private static final double  AFTER_SHOT_SEC    = 0.3;   // settle time after the last ball

    // Run the intake while feeding, in case balls need pushing up to the feeder. Off by default
    // because the TeleOp drives the intake and feeder separately.
    private static final boolean INTAKE_WHILE_SHOOTING = false;
    private static final double  INTAKE_POWER      = 1.0;

    // =====================================================================
    // FLYWHEEL CONTROL (copied from the TeleOp so it behaves the same)
    // =====================================================================
    private static final double TICKS_PER_REV      = 28.0;   // HD Hex encoder ticks per rev
    private static final double MEASURED_MAX_RPM   = 4200.0; // measured top speed at full power
    private static final double AT_SPEED_TOLERANCE = 75.0;   // within this many RPM counts as READY
    private static final double BOOST_START_RPM    = 150.0;  // further below target than this -> full power
    private static final double BOOST_STOP_RPM     = 50.0;   // within this of target -> PID takes over

    private static final double MAX_TICKS_PER_SEC  = MEASURED_MAX_RPM / 60.0 * TICKS_PER_REV;
    private static final double FLY_F = 32767.0 / MAX_TICKS_PER_SEC;
    private static final double FLY_P = 0.5 * FLY_F;
    private static final double FLY_I = 0.1 * FLY_P;
    private static final double FLY_D = 0.0;

    // =====================================================================
    // INIT MENU
    // =====================================================================
    private static final double START_DELAY_MAX_SEC = 10.0;

    private DcMotorEx frontLeft, backLeft, frontRight, backRight;
    private DcMotorEx intake, flywheel;
    private CRServo feeder;
    private VoltageSensor battery;

    // flywheel state
    private boolean flywheelRequested = false; // true = we want it spinning at SHOOT_RPM
    private boolean flywheelActive    = false; // true = we have actually commanded it
    private boolean boosting          = false; // true = full power spin-up
    private boolean hubPidActive      = false; // true = RUN_USING_ENCODER (hub PID)
    private boolean atSpeed           = false;
    private double  currentRPM        = 0;

    private double driveScale = 1.0;

    @Override
    public void runOpMode() {
        // --- Hardware (same names as the TeleOp) ---
        frontLeft  = hardwareMap.get(DcMotorEx.class, "frontLeft");
        backLeft   = hardwareMap.get(DcMotorEx.class, "backLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backRight  = hardwareMap.get(DcMotorEx.class, "backRight");
        intake     = hardwareMap.get(DcMotorEx.class, "intake");
        feeder     = hardwareMap.get(CRServo.class, "servo");
        flywheel   = hardwareMap.get(DcMotorEx.class, "flywheel");
        battery    = hardwareMap.voltageSensor.iterator().next();

        // --- Directions (confirmed working, see TEAM_LOG.md) ---
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.REVERSE);
        intake.setDirection(DcMotor.Direction.REVERSE);
        feeder.setDirection(DcMotorSimple.Direction.REVERSE);
        flywheel.setDirection(DcMotor.Direction.FORWARD);

        for (DcMotorEx m : new DcMotorEx[]{frontLeft, backLeft, frontRight, backRight}) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // stop sharply between moves
        }
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        flywheel.setVelocityPIDFCoefficients(FLY_P, FLY_I, FLY_D, FLY_F);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // --- INIT MENU: runs until PLAY is pressed ---
        double startDelay = 0.0;
        boolean shootEnabled = SHOOT_DEFAULT_ON;
        boolean lastUp = false, lastDown = false, lastCross = false;

        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.dpad_up && !lastUp) {
                startDelay = Range.clip(startDelay + 1.0, 0.0, START_DELAY_MAX_SEC);
            }
            if (gamepad1.dpad_down && !lastDown) {
                startDelay = Range.clip(startDelay - 1.0, 0.0, START_DELAY_MAX_SEC);
            }
            if (gamepad1.cross && !lastCross) {
                shootEnabled = !shootEnabled;
            }
            lastUp = gamepad1.dpad_up;
            lastDown = gamepad1.dpad_down;
            lastCross = gamepad1.cross;

            telemetry.addLine("AUTO FULL ready. Press PLAY.");
            telemetry.addData("Start delay (D-pad up/down)", "%.0f s", startDelay);
            telemetry.addData("Shooting (Cross toggles)", shootEnabled
                    ? "ON: " + SHOT_COUNT + " shots at " + (int) SHOOT_RPM + " RPM" : "OFF (drive only)");
            telemetry.addData("Battery", "%.2f V", battery.getVoltage());
            if (battery.getVoltage() < 12.0) {
                telemetry.addLine("WARNING: battery is low. Timed auto will go short. Swap it.");
            }
            telemetry.update();
        }

        if (isStopRequested()) return;

        // Measure the battery once at the start (not per loop, so it doesn't wobble).
        if (COMPENSATE_BATTERY) {
            driveScale = Range.clip(NOMINAL_VOLTAGE / Math.max(battery.getVoltage(), 9.0), 1.0, 1.25);
        }

        // ---------- Start delay (flywheel off, robot still) ----------
        pause("Waiting to start", startDelay);

        // ---------- Step 1: flywheel on; it keeps spinning during the drive ----------
        if (shootEnabled) {
            flywheelRequested = true;
        }

        // ---------- Step 2: forward, off the wall ----------
        move("1: Forward off the wall", OFF_WALL_POWER, 0, OFF_WALL_SEC);
        pause("Pause", PAUSE_SEC);

        // ---------- Step 3: shoot ----------
        if (shootEnabled) {
            shoot();
            flywheelRequested = false;   // spin down before parking
            serviceFlywheel();
        }

        // ---------- Step 4: park (same as "Auto") ----------
        move("3: Strafing left", 0, -STRAFE_POWER, STRAFE_LEFT_SEC); // negative strafe = left
        pause("Pause", PAUSE_SEC);
        move("4: Backing up partway", ZONE_DIRECTION * ZONE_POWER, 0, ZONE_SEC);

        stopEverything();
        telemetry.addLine("Done. Parked off the wall.");
        telemetry.update();

        // Stay stopped until the auto period ends.
        while (opModeIsActive()) {
            idle();
        }
    }

    // =====================================================================
    // SHOOTING
    // =====================================================================

    /** Fire SHOT_COUNT balls: wait for READY, pulse the feeder, let the RPM recover. */
    private void shoot() {
        for (int shot = 1; shot <= SHOT_COUNT && opModeIsActive(); shot++) {
            // Wait until the flywheel is up to speed (or give up after the timeout and fire anyway).
            double timeout = (shot == 1) ? SPINUP_TIMEOUT_SEC : RECOVER_TIMEOUT_SEC;
            ElapsedTime wait = new ElapsedTime();
            while (opModeIsActive() && !atSpeed && wait.seconds() < timeout) {
                serviceFlywheel();
                telemetry.addData("Shoot", "%d/%d: waiting for flywheel", shot, SHOT_COUNT);
                addFlywheelTelemetry();
                telemetry.update();
            }

            // Feed one ball.
            feeder.setPower(FEEDER_POWER);
            if (INTAKE_WHILE_SHOOTING) intake.setPower(INTAKE_POWER);
            ElapsedTime pulse = new ElapsedTime();
            while (opModeIsActive() && pulse.seconds() < FEED_PULSE_SEC) {
                serviceFlywheel();
                telemetry.addData("Shoot", "%d/%d: FIRING", shot, SHOT_COUNT);
                addFlywheelTelemetry();
                telemetry.update();
            }
            feeder.setPower(0);
            intake.setPower(0);
        }

        // Let the last ball clear before moving.
        ElapsedTime settle = new ElapsedTime();
        while (opModeIsActive() && settle.seconds() < AFTER_SHOT_SEC) {
            serviceFlywheel();
        }
    }

    /**
     * Keeps the flywheel running. Called inside EVERY wait loop so it holds speed while driving.
     * Same boost-then-hub-PID logic as the TeleOp.
     */
    private void serviceFlywheel() {
        currentRPM = flywheel.getVelocity() / TICKS_PER_REV * 60.0;

        if (!flywheelRequested) {
            if (flywheelActive) {
                flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                flywheel.setPower(0);   // coast down
                flywheelActive = false;
                hubPidActive = false;
                boosting = false;
            }
            atSpeed = false;
            return;
        }

        if (!flywheelActive) {
            flywheelActive = true;
            boosting = false;
            hubPidActive = false;
        }

        double error = SHOOT_RPM - currentRPM;
        if (!boosting && error > BOOST_START_RPM) {
            boosting = true;
        } else if (boosting && error < BOOST_STOP_RPM) {
            boosting = false;
        }

        if (boosting) {
            // Full power, no PID, to get up to speed as fast as possible.
            if (hubPidActive) {
                flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                hubPidActive = false;
            }
            flywheel.setPower(1.0);
        } else {
            // Hub's built-in PID holds the target (the TeleOp does the same).
            if (!hubPidActive) {
                flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                hubPidActive = true;
            }
            flywheel.setVelocity(SHOOT_RPM / 60.0 * TICKS_PER_REV);
        }

        atSpeed = Math.abs(error) < AT_SPEED_TOLERANCE;
    }

    private void addFlywheelTelemetry() {
        telemetry.addData("Target RPM", "%.0f", SHOOT_RPM);
        telemetry.addData("Actual RPM", "%.0f", currentRPM);
        telemetry.addData("Flywheel", !flywheelRequested ? "OFF"
                : (atSpeed ? "READY" : (boosting ? "BOOST (full power)" : "spinning up...")));
    }

    // =====================================================================
    // DRIVING (same mecanum math as "Auto" and the TeleOp)
    // =====================================================================

    /**
     * Drive for a number of seconds.
     * drive: + forward, - backward
     * strafe: + right, - left
     */
    private void move(String label, double drive, double strafe, double seconds) {
        double denominator = Math.max(Math.abs(drive) + Math.abs(strafe), 1.0);
        double s = driveScale / denominator;
        setDrive((drive + strafe) * s, (drive - strafe) * s, (drive - strafe) * s, (drive + strafe) * s);

        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < seconds) {
            serviceFlywheel();
            telemetry.addLine(label);
            telemetry.addData("Time", "%.1f / %.1f s", timer.seconds(), seconds);
            addFlywheelTelemetry();
            telemetry.update();
        }
        stopDrive();
    }

    /** Stand still for a number of seconds (the flywheel keeps running if it was requested). */
    private void pause(String label, double seconds) {
        stopDrive();
        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < seconds) {
            serviceFlywheel();
            telemetry.addLine(label);
            telemetry.addData("Time", "%.1f / %.1f s", timer.seconds(), seconds);
            telemetry.update();
        }
    }

    private void setDrive(double fl, double bl, double fr, double br) {
        frontLeft.setPower(Range.clip(fl, -1.0, 1.0));
        backLeft.setPower(Range.clip(bl, -1.0, 1.0));
        frontRight.setPower(Range.clip(fr, -1.0, 1.0));
        backRight.setPower(Range.clip(br, -1.0, 1.0));
    }

    private void stopDrive() {
        setDrive(0, 0, 0, 0);
    }

    private void stopEverything() {
        stopDrive();
        flywheelRequested = false;
        serviceFlywheel();
        feeder.setPower(0);
        intake.setPower(0);
    }
}
