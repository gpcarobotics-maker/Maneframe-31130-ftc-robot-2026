package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import java.util.List;
import java.util.Set;

/**
 * ENCODER FINDER - a test OpMode that shows which port an encoder signal is arriving on.
 *
 * File must be named EncoderFinder.java and live in:
 *   TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
 *
 * It does NOT power any motor. It only reads every motor's encoder.
 *
 * How to use:
 *   1. Run "Encoder Finder" from the Driver Station (it is in the TeleOp list).
 *   2. Press PLAY, then slowly turn the FLYWHEEL by hand.
 *   3. Whichever line says "<-- MOVING" is the port the flywheel encoder is plugged into.
 *      That line MUST be "flywheel". If it is a different name, the encoder cable is in
 *      the wrong port: move it to the encoder connector with the same number as the
 *      flywheel's power cable, on the same hub.
 *   4. If NO line says MOVING while you turn the flywheel, the signal is not reaching the hub
 *      at all: the encoder cable or the motor's encoder is broken (swap the cable to check).
 *
 * Only ports that have a motor in the Robot Configuration are listed. To check an empty port,
 * add a motor there in the configuration (any name, e.g. "test") and run this again.
 */
@TeleOp(name = "Encoder Finder", group = "Test")
public class EncoderFinder extends LinearOpMode {

    private static final int MOVING_TICKS = 10; // this many ticks of change counts as "moving"

    @Override
    public void runOpMode() {
        List<DcMotorEx> motors = hardwareMap.getAll(DcMotorEx.class);
        String[] names = new String[motors.size()];
        int[] startTicks = new int[motors.size()];

        for (int i = 0; i < motors.size(); i++) {
            DcMotorEx m = motors.get(i);
            Set<String> nameSet = hardwareMap.getNamesOf(m);
            names[i] = nameSet.isEmpty() ? "(no name)" : nameSet.iterator().next();
        }

        telemetry.addData("Motors found", motors.size());
        telemetry.addLine("Press PLAY, then turn the flywheel by hand.");
        telemetry.update();

        waitForStart();

        // Record starting positions at PLAY so only movement after PLAY counts.
        for (int i = 0; i < motors.size(); i++) {
            startTicks[i] = motors.get(i).getCurrentPosition();
        }

        while (opModeIsActive()) {
            telemetry.addLine("Turn the flywheel by hand. Look for <-- MOVING");
            telemetry.addLine();

            for (int i = 0; i < motors.size(); i++) {
                DcMotorEx m = motors.get(i);
                int ticks = m.getCurrentPosition();
                int moved = ticks - startTicks[i];
                String flag = Math.abs(moved) >= MOVING_TICKS ? "   <-- MOVING" : "";

                telemetry.addLine(names[i] + "  (port " + m.getPortNumber() + ")" + flag);
                telemetry.addLine("    hub: " + m.getController().getConnectionInfo());
                telemetry.addLine("    ticks: " + ticks + "   moved: " + moved
                        + "   velocity: " + Math.round(m.getVelocity()) + " ticks/s");
            }

            telemetry.addLine();
            telemetry.addLine("Press A / Cross on controller 1 to reset 'moved' to 0.");
            if (gamepad1.cross) {
                for (int i = 0; i < motors.size(); i++) {
                    startTicks[i] = motors.get(i).getCurrentPosition();
                }
            }

            telemetry.update();
        }
    }
}
