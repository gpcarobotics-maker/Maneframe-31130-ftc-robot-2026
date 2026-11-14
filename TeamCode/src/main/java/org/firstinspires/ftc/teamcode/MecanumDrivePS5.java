package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/**
 * Mecanum drive + intake/feeder + PID-controlled flywheel for a PS5 / DualSense controller.
 *
 * File must be named MecanumDrivePS5.java and live in:
 *   TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
 *
 * Controller 1 (gamepad1) - DRIVER:
 *   Left stick UP / DOWN     -> drive forward / backward
 *   Left stick LEFT / RIGHT  -> strafe left / right
 *   Right stick LEFT / RIGHT -> turn in place
 *   R1 (right bumper)        -> hold for slow / precision driving
 *
 * Controller 2 (gamepad2) - OPERATOR:
 *   L2 (left trigger)        -> hold to run intake + feeder together
 *   L1 (left bumper)         -> hold to outtake (intake + feeder BACKWARD)
 *   R2 (right trigger)       -> hold to spin flywheel at the target RPM
 *   D-pad UP / DOWN          -> raise / lower target RPM (1000 to 3000, steps of 100)
 *   R1 (right bumper)        -> hold to spin flywheel BACKWARD (unjam). Overrides R2.
 *
 *   Controller 2 rumbles once when the flywheel reaches the target RPM.
 *
 * Robot Configuration names (must match the Driver Station exactly):
 *   frontLeft, backLeft, frontRight, backRight  -> drive motors
 *   intake   -> REV Core Hex motor
 *   servo    -> feeder, configured as a "Continuous Rotation Servo"
 *   flywheel -> REV HD Hex motor (1:1, adapter plate only)
 */
@TeleOp(name = "Mecanum Drive (PS5)", group = "Drive")
public class MecanumDrivePS5 extends LinearOpMode {

    // ---------- Intake / feeder ----------
    private static final double INTAKE_POWER = 1.0;  // Core Hex motor
    private static final double FEEDER_POWER = 1.0;  // continuous servo, -1.0 to 1.0

    // ---------- Flywheel motor (REV HD Hex, 1:1) ----------
    private static final double TICKS_PER_REV = 28.0; // HD Hex encoder ticks per revolution
    // Top speed YOUR flywheel reached at full power (measured on the robot: about 4200).
    // The PID uses this to guess how much power each target RPM needs.
    private static final double MEASURED_MAX_RPM = 4200.0;

    // Device whose encoder port the flywheel's encoder cable is plugged into ("flywheel" normally).
    private static final String  FLYWHEEL_ENCODER_NAME     = "flywheel";
    private static final boolean FLYWHEEL_ENCODER_REVERSED = false; // set true if RPM reads NEGATIVE

    // ---------- Target RPM range ----------
    private static final double RPM_MIN   = 1000.0;
    private static final double RPM_MAX   = 3000.0;
    private static final double RPM_STEP  = 100.0;
    private static final double RPM_START = 2000.0;
    private static final double AT_SPEED_TOLERANCE = 75.0; // RPM within target counts as "READY"

    // ---------- Boost: full power to spin up fast, then PID holds the speed ----------
    // More than BOOST_START_RPM below target -> full power.
    // Within BOOST_STOP_RPM of target       -> PID takes over and holds the exact speed.
    private static final double BOOST_START_RPM = 150.0;
    private static final double BOOST_STOP_RPM  = 50.0;

    // ---------- Flywheel reverse (controller 2 R1) ----------
    private static final double FLYWHEEL_REVERSE_POWER = 0.5; // 0.0 to 1.0, how fast it spins backward

    // ---------- PID (runs on the hub when the encoder is in the flywheel's own port) ----------
    //   F = base power for the target speed (from your measured top speed)
    //   P = how hard it pushes when off target. If RPM bounces up and down, lower 0.5 (try 0.3).
    //       If it sits below target, raise it (try 1.0).
    //   I = slowly removes any small error that is left over.
    private static final double MAX_TICKS_PER_SEC = MEASURED_MAX_RPM / 60.0 * TICKS_PER_REV;
    private static final double FLY_F = 32767.0 / MAX_TICKS_PER_SEC;
    private static final double FLY_P = 0.5 * FLY_F;
    private static final double FLY_I = 0.1 * FLY_P;
    private static final double FLY_D = 0.0;

    // ---------- PID in this code (only used if the encoder is on a DIFFERENT port) ----------
    private static final double SW_kF    = 1.0 / MEASURED_MAX_RPM;
    private static final double SW_kP    = 0.0005;
    private static final double SW_kI    = 0.0002;
    private static final double SW_MAX_I = 1000.0;

    private DcMotorEx frontLeft, backLeft, frontRight, backRight;
    private DcMotorEx intake, flywheel, flywheelEncoder;
    private CRServo feeder;

    @Override
    public void runOpMode() {
        // --- Drive motors ---
        frontLeft  = hardwareMap.get(DcMotorEx.class, "frontLeft");
        backLeft   = hardwareMap.get(DcMotorEx.class, "backLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backRight  = hardwareMap.get(DcMotorEx.class, "backRight");

        // --- Mechanism motors ---
        intake          = hardwareMap.get(DcMotorEx.class, "intake");   // Core Hex motor
        feeder          = hardwareMap.get(CRServo.class, "servo");      // continuous rotation servo
        flywheel        = hardwareMap.get(DcMotorEx.class, "flywheel"); // HD Hex motor
        flywheelEncoder = hardwareMap.get(DcMotorEx.class, FLYWHEEL_ENCODER_NAME);
        boolean encoderOnOwnPort = (flywheelEncoder == flywheel);

        // --- Directions: swap REVERSE <-> FORWARD on anything that spins the wrong way ---
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        intake.setDirection(DcMotor.Direction.REVERSE);
        feeder.setDirection(DcMotorSimple.Direction.REVERSE);
        flywheel.setDirection(DcMotor.Direction.FORWARD);

        // Drive and intake brake so they stop right away (servos stop on their own at power 0).
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Flywheel: coast when off; give the hub's built-in PID our gains.
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        flywheel.setVelocityPIDFCoefficients(FLY_P, FLY_I, FLY_D, FLY_F);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        VoltageSensor battery = hardwareMap.voltageSensor.iterator().next();

        double targetRPM = RPM_START;
        double integral = 0;
        boolean flywheelWasOn = false;
        boolean boosting = false;       // true = full power spin-up
        boolean hubPidActive = false;   // true = flywheel is in RUN_USING_ENCODER (hub PID)
        boolean rumbledAtSpeed = false;
        boolean lastDpadUp = false;
        boolean lastDpadDown = false;
        boolean flywheelWasReversing = false;
        double lastSpinupSeconds = 0;
        ElapsedTime loopTimer = new ElapsedTime();
        ElapsedTime spinupTimer = new ElapsedTime();

        telemetry.addLine("Initialized. Press PLAY.");
        telemetry.update();

        waitForStart();
        loopTimer.reset();

        while (opModeIsActive()) {
            double dt = Math.max(loopTimer.seconds(), 0.001);
            loopTimer.reset();

            // ================= DRIVE =================
            double drive  = -gamepad1.left_stick_y;        // up/down    -> forward/back
            double strafe =  gamepad1.left_stick_x * 1.1;  // left/right -> strafe
            double turn   =  gamepad1.right_stick_x;       // right stick -> turn

            double speed = gamepad1.right_bumper ? 0.4 : 1.0; // hold R1 for slow mode

            double denominator = Math.max(Math.abs(drive) + Math.abs(strafe) + Math.abs(turn), 1.0);
            frontLeft.setPower((drive + strafe + turn) / denominator * speed);
            backLeft.setPower((drive - strafe + turn) / denominator * speed);
            frontRight.setPower((drive - strafe - turn) / denominator * speed);
            backRight.setPower((drive + strafe - turn) / denominator * speed);

            // ================= INTAKE + FEEDER =================
            if (gamepad2.left_trigger > 0.2) {          // Controller 2 L2: run both forward
                intake.setPower(INTAKE_POWER);
                feeder.setPower(FEEDER_POWER);
            } else if (gamepad2.left_bumper) {          // Controller 2 L1: outtake (both backward)
                intake.setPower(-INTAKE_POWER);
                feeder.setPower(-FEEDER_POWER);
            } else {
                intake.setPower(0);
                feeder.setPower(0);
            }

            // ================= TARGET RPM (Controller 2 D-pad) =================
            if (gamepad2.dpad_up && !lastDpadUp) {
                targetRPM = Range.clip(targetRPM + RPM_STEP, RPM_MIN, RPM_MAX);
            }
            if (gamepad2.dpad_down && !lastDpadDown) {
                targetRPM = Range.clip(targetRPM - RPM_STEP, RPM_MIN, RPM_MAX);
            }
            lastDpadUp = gamepad2.dpad_up;
            lastDpadDown = gamepad2.dpad_down;

            // ================= FLYWHEEL SPEED =================
            double ticksPerSec = flywheelEncoder.getVelocity();
            if (FLYWHEEL_ENCODER_REVERSED) ticksPerSec = -ticksPerSec;
            double currentRPM = ticksPerSec / TICKS_PER_REV * 60.0;
            double error = targetRPM - currentRPM;

            // ================= FLYWHEEL PID =================
            boolean flywheelReverse = gamepad2.right_bumper;                       // Controller 2 R1
            boolean flywheelOn = gamepad2.right_trigger > 0.2 && !flywheelReverse; // Controller 2 R2

            if (flywheelOn && !flywheelWasOn) {
                // Just pressed R2.
                integral = 0;
                boosting = false;
                spinupTimer.reset();
            } else if (!flywheelOn && flywheelWasOn) {
                // Just released R2: coast down.
                flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                flywheel.setPower(0);
                hubPidActive = false;
                boosting = false;
                rumbledAtSpeed = false;
            }
            flywheelWasOn = flywheelOn;

            // R1: spin the flywheel backward while held (no PID).
            if (flywheelReverse) {
                if (hubPidActive) {
                    flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    hubPidActive = false;
                }
                flywheel.setPower(-FLYWHEEL_REVERSE_POWER);
            } else if (flywheelWasReversing && !flywheelOn) {
                flywheel.setPower(0); // just let go of R1: stop
            }
            flywheelWasReversing = flywheelReverse;

            if (flywheelOn) {
                // Boost on when far below target, off once close to target.
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
                    integral = 0;
                } else if (encoderOnOwnPort) {
                    // Hub's built-in PID: give it the target in ticks per second.
                    if (!hubPidActive) {
                        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        hubPidActive = true;
                    }
                    flywheel.setVelocity(targetRPM / 60.0 * TICKS_PER_REV);
                } else {
                    // Encoder is on another port, so the hub can't do the PID. Do it here.
                    integral = Range.clip(integral + error * dt, -SW_MAX_I, SW_MAX_I);
                    double power = SW_kF * targetRPM + SW_kP * error + SW_kI * integral;
                    flywheel.setPower(Range.clip(power, 0.0, 1.0));
                }
            }

            boolean atSpeed = flywheelOn && Math.abs(error) < AT_SPEED_TOLERANCE;

            // Rumble once when we first reach target speed, and remember how long it took.
            if (atSpeed && !rumbledAtSpeed) {
                gamepad2.rumble(200);
                rumbledAtSpeed = true;
                lastSpinupSeconds = spinupTimer.seconds();
            }

            // ================= TELEMETRY =================
            telemetry.addData("Target RPM", "%.0f", targetRPM);
            telemetry.addData("Actual RPM", "%.0f", currentRPM);
            telemetry.addData("Error (target - actual)", "%.0f", error);
            telemetry.addData("Flywheel", flywheelReverse ? "REVERSE (R1)" : !flywheelOn ? "OFF"
                    : (atSpeed ? "READY" : (boosting ? "BOOST (full power)" : "spinning up...")));
            telemetry.addData("Last spin-up to READY", "%.1f sec", lastSpinupSeconds);
            telemetry.addData("Battery", "%.2f V", battery.getVoltage());
            telemetry.addData("Intake / Feeder", "%.2f / %.2f", intake.getPower(), feeder.getPower());
            telemetry.addData("Slow mode", gamepad1.right_bumper ? "ON" : "off");
            telemetry.update();
        }
    }
}