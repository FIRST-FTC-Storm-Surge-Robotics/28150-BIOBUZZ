package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

/*
 * First prototype TeleOp with robot-centric mecanum drive.
 *
 * Controls (gamepad1):
 *   Left stick         - drive forward/back and strafe (relative to the robot)
 *   Right stick X      - rotate
 *   Hold right bumper  - slow mode
 *   B                  - toggle the intake on or off
 */
@TeleOp(name = "Prototype TeleOp", group = "Prototype")
public class PrototypeTeleOp extends OpMode {

    static final double SLOW_MODE_SPEED = 0.4;
    // Mecanum wheels strafe less efficiently than they drive forward.
    static final double STRAFE_CORRECTION = 1.1;
    // Sticks rarely rest at exactly 0. Readings smaller than this are treated as "hands off."
    static final double STICK_DEADZONE = 0.1;
    // The first loops often report 0 before the real stick positions arrive.
    // Don't count those toward "the sticks are centered."
    static final int STARTUP_LOOPS_TO_IGNORE = 10;
    // Sticks must stay inside the deadzone this many loops in a row before the wheels can move.
    static final int CENTERED_LOOPS_NEEDED = 10;
    // Power used while the intake is toggled on.
    static final double INTAKE_POWER = .3;

    private DcMotor frontLeftDrive;
    private DcMotor frontRightDrive;
    private DcMotor backLeftDrive;
    private DcMotor backRightDrive;
    private DcMotor intakeMotor;

    private boolean intakeOn = false;
    // B stays true for many loops while it is held. Only the first loop should flip the intake.
    private boolean bWasPressed = false;

    // Some controllers treat "center" as wherever the sticks were when they were plugged in.
    // If a stick was held, it keeps reporting a full push. Stay stopped until we see a real center.
    private boolean sticksHaveCentered = false;
    private int centeredLoopCount = 0;
    private int loopsSinceStart = 0;

    @Override
    public void init() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeftDrive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRightDrive");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeftDrive");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRightDrive");
        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        // Right side was spinning backward, so forward and turn were swapped.
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.REVERSE);

        intakeMotor.setDirection(DcMotor.Direction.FORWARD);
        for (DcMotor motor : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive, intakeMotor}) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setPower(0);
        }

        telemetry.addLine("Initialized. Wheels are stopped. Press START.");
    }

    @Override
    public void init_loop() {
        // Keep commanding 0 the whole time the robot sits on the init screen.
        stopAllMotors();
        telemetry.addLine("Initialized. Wheels are stopped. Press START.");
    }

    @Override
    public void start() {
        sticksHaveCentered = false;
        centeredLoopCount = 0;
        loopsSinceStart = 0;
        intakeOn = false;
        bWasPressed = false;
    }

    @Override
    public void loop() {
        loopsSinceStart++;

        double rawForward = -gamepad1.left_stick_y;
        double rawStrafe = gamepad1.left_stick_x;
        double rawTurn = gamepad1.right_stick_x;

        boolean sticksLookCentered = Math.abs(rawForward) < STICK_DEADZONE
                && Math.abs(rawStrafe) < STICK_DEADZONE
                && Math.abs(rawTurn) < STICK_DEADZONE;

        if (!sticksHaveCentered) {
            // One early reading of 0 is not enough. A stuck stick shows up right after that
            // and would otherwise send the robot off at full power.
            if (loopsSinceStart > STARTUP_LOOPS_TO_IGNORE && sticksLookCentered) {
                centeredLoopCount++;
                if (centeredLoopCount >= CENTERED_LOOPS_NEEDED) {
                    sticksHaveCentered = true;
                }
            } else {
                centeredLoopCount = 0;
            }
        }

        double y = 0;
        double x = 0;
        double rx = 0;
        if (sticksHaveCentered) {
            // Pushing the left stick forward reports a negative number, so rawForward is already flipped.
            y = applyDeadzone(rawForward);
            // Negated so the robot strafes the same way the left stick is pushed.
            x = -applyDeadzone(rawStrafe) * STRAFE_CORRECTION;
            rx = applyDeadzone(rawTurn);
        }

        boolean slowMode = gamepad1.right_bumper;
        double speed = slowMode ? SLOW_MODE_SPEED : 1.0;

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
        double frontLeftPower = (y + x + rx) / denominator * speed;
        double backLeftPower = (y - x + rx) / denominator * speed;
        double frontRightPower = (y - x - rx) / denominator * speed;
        double backRightPower = (y + x - rx) / denominator * speed;
        frontLeftDrive.setPower(frontLeftPower);
        backLeftDrive.setPower(backLeftPower);
        frontRightDrive.setPower(frontRightPower);
        backRightDrive.setPower(backRightPower);

        boolean bPressed = gamepad1.b;
        if (bPressed && !bWasPressed) {
            intakeOn = !intakeOn;
        }
        bWasPressed = bPressed;
        intakeMotor.setPower(intakeOn ? INTAKE_POWER : 0);

        telemetry.addData("Slow mode", slowMode ? "ON" : "OFF");
        telemetry.addData("Intake", intakeOn ? "ON" : "OFF");
        telemetry.addData("Left stick x / y", "%.2f / %.2f", gamepad1.left_stick_x, gamepad1.left_stick_y);
        telemetry.addData("Right stick x", "%.2f", gamepad1.right_stick_x);
        telemetry.addData("Front L/R", "%.2f / %.2f", frontLeftPower, frontRightPower);
        telemetry.addData("Back  L/R", "%.2f / %.2f", backLeftPower, backRightPower);
        telemetry.addLine();
        if (sticksHaveCentered) {
            telemetry.addLine("Hold RB: slow mode. Press B: intake");
        } else if (sticksLookCentered) {
            telemetry.addLine("Wheels are held off until the sticks stay centered.");
            telemetry.addData("Centered readings", "%d / %d", centeredLoopCount, CENTERED_LOOPS_NEEDED);
        } else {
            telemetry.addLine("A stick still looks pushed, so the wheels are held off.");
            telemetry.addLine("Let go of both sticks. If a number stays near 1 or -1, unplug the controller and plug it back in with the sticks centered.");
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        stopAllMotors();
    }

    private void stopAllMotors() {
        for (DcMotor motor : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive, intakeMotor}) {
            motor.setPower(0);
        }
    }

    // Tiny stick readings become 0. The rest of the stick is stretched so a full push is still full power.
    private static double applyDeadzone(double value) {
        double magnitude = Math.abs(value);
        if (magnitude < STICK_DEADZONE) {
            return 0;
        }
        double scaled = (magnitude - STICK_DEADZONE) / (1.0 - STICK_DEADZONE);
        return Math.copySign(scaled, value);
    }
}
