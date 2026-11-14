package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * AUTO: get off the wall, strafe left, then drive into the human player zone.
 *
 * File must be named AutoOutAndBack.java and live in:
 *   TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
 *
 *   Step 1: drive FORWARD off the wall
 *   Step 2: strafe LEFT
 *   Step 3: drive into the human player zone (BACKWARD toward the wall by default)
 *
 * It is TIMED (no encoders), so distances depend on battery charge.
 * Test on the practice field and adjust the numbers below.
 *
 * Uses the same drive motor names and directions as "Mecanum Drive (PS5)".
 * After it finishes, the Driver Station automatically loads "Mecanum Drive (PS5)".
 */
@Autonomous(name = "Auto", group = "Auto", preselectTeleOp = "Mecanum Drive (PS5)")
public class Auto extends LinearOpMode {

    // ---------- TUNE THESE ON THE PRACTICE FIELD ----------
    private static final double START_DELAY_SEC = 0.0;  // wait before moving (to stay out of a partner's way)

    // Step 1: forward, off the wall
    private static final double OFF_WALL_POWER  = 0.4;
    private static final double OFF_WALL_SEC    = 0.7;  // was 0.4, +0.3 after testing

    // Step 2: strafe left
    private static final double STRAFE_POWER    = 0.5;  // strafing is slower than driving, so a bit more power
    private static final double STRAFE_LEFT_SEC = 0.8;  // was 0.5, needed more left after testing

    // Step 3: into the human player zone
    private static final double ZONE_POWER      = 0.4;
    private static final double ZONE_SEC        = 0.35; // was 0.8, which ended back ON the wall (no point); only back up about halfway
    // -1 = BACK toward the wall (zone is against the wall). Change to 1 if the zone is in FRONT of you.
    private static final int    ZONE_DIRECTION  = -1;

    private static final double PAUSE_SEC       = 0.2;  // short stop between moves so they don't blend together

    private DcMotorEx frontLeft, backLeft, frontRight, backRight;

    @Override
    public void runOpMode() {
        frontLeft  = hardwareMap.get(DcMotorEx.class, "frontLeft");
        backLeft   = hardwareMap.get(DcMotorEx.class, "backLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backRight  = hardwareMap.get(DcMotorEx.class, "backRight");

        // Same directions as the TeleOp, so forward and left mean the same thing.
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotorEx m : new DcMotorEx[]{frontLeft, backLeft, frontRight, backRight}) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // stop sharply between moves
        }

        telemetry.addLine("Auto ready: off wall -> strafe left -> into zone. Press PLAY.");
        telemetry.update();

        waitForStart();

        pause("Waiting to start", START_DELAY_SEC);

        // Step 1: forward off the wall
        move("1: Forward off the wall", OFF_WALL_POWER, 0, OFF_WALL_SEC);
        pause("Pause", PAUSE_SEC);

        // Step 2: strafe left (negative strafe = left, same as the left stick)
        move("2: Strafing left", 0, -STRAFE_POWER, STRAFE_LEFT_SEC);
        pause("Pause", PAUSE_SEC);

        // Step 3: into the human player zone
        move("3: Into the human player zone", ZONE_DIRECTION * ZONE_POWER, 0, ZONE_SEC);

        stopDrive();
        telemetry.addLine("Done. Parked in the human player zone.");
        telemetry.update();

        // Stay stopped until the auto period ends.
        while (opModeIsActive()) {
            idle();
        }
    }

    /**
     * Drive for a number of seconds.
     *   drive:  + forward, - backward
     *   strafe: + right,   - left
     * Uses the same mecanum math as the TeleOp.
     */
    private void move(String label, double drive, double strafe, double seconds) {
        double denominator = Math.max(Math.abs(drive) + Math.abs(strafe), 1.0);
        frontLeft.setPower((drive + strafe) / denominator);
        backLeft.setPower((drive - strafe) / denominator);
        frontRight.setPower((drive - strafe) / denominator);
        backRight.setPower((drive + strafe) / denominator);

        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < seconds) {
            telemetry.addLine(label);
            telemetry.addData("Time", "%.1f / %.1f s", timer.seconds(), seconds);
            telemetry.update();
        }
        stopDrive();
    }

    /** Stand still for a number of seconds. */
    private void pause(String label, double seconds) {
        stopDrive();
        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < seconds) {
            telemetry.addLine(label);
            telemetry.addData("Time", "%.1f / %.1f s", timer.seconds(), seconds);
            telemetry.update();
        }
    }

    private void stopDrive() {
        frontLeft.setPower(0);
        backLeft.setPower(0);
        frontRight.setPower(0);
        backRight.setPower(0);
    }
}