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
 */
@TeleOp(name = "Prototype TeleOp", group = "Prototype")
public class PrototypeTeleOp extends OpMode {

    static final double SLOW_MODE_SPEED = 0.4;
    // Mecanum wheels strafe less efficiently than they drive forward.
    static final double STRAFE_CORRECTION = 1.1;
    // Sticks rarely rest at exactly 0. Readings smaller than this are treated as "hands off."
    static final double STICK_DEADZONE = 0.1;

    private DcMotor frontLeftDrive;
    private DcMotor frontRightDrive;
    private DcMotor backLeftDrive;
    private DcMotor backRightDrive;

    // Some controllers treat "center" as wherever the sticks were when they were plugged in.
    // If a stick was held, it keeps reporting full press. Stay stopped until we see center.
    private boolean sticksHaveCentered = false;

    @Override
    public void init() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeftDrive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRightDrive");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeftDrive");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRightDrive");

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.REVERSE);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        for (DcMotor motor : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive}) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setPower(0);
        }

        telemetry.addLine("Initialized. Press START.");
    }

    @Override
    public void start() {
        sticksHaveCentered = false;
    }

    @Override
    public void loop() {
        double rawForward = -gamepad1.left_stick_y;
        double rawStrafe = gamepad1.left_stick_x;
        double rawTurn = gamepad1.right_stick_x;

        if (Math.abs(rawForward) < STICK_DEADZONE
                && Math.abs(rawStrafe) < STICK_DEADZONE
                && Math.abs(rawTurn) < STICK_DEADZONE) {
            sticksHaveCentered = true;
        }

        double y = 0;
        double x = 0;
        double rx = 0;
        if (sticksHaveCentered) {
            // Pushing the left stick forward reports a negative number, so rawForward is already flipped.
            y = applyDeadzone(rawForward);
            // Negated because this drivetrain strafes opposite to the standard mecanum layout.
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

        telemetry.addData("Slow mode", slowMode ? "ON" : "OFF");
        telemetry.addData("Left stick x / y", "%.2f / %.2f", gamepad1.left_stick_x, gamepad1.left_stick_y);
        telemetry.addData("Right stick x", "%.2f", gamepad1.right_stick_x);
        telemetry.addData("Front L/R", "%.2f / %.2f", frontLeftPower, frontRightPower);
        telemetry.addData("Back  L/R", "%.2f / %.2f", backLeftPower, backRightPower);
        telemetry.addLine();
        if (sticksHaveCentered) {
            telemetry.addLine("Hold RB: slow mode");
        } else {
            telemetry.addLine("Stick is reading as fully pressed, so the wheels are held off.");
            telemetry.addLine("Let go of both sticks. If the numbers stay near 1 or -1, unplug the controller and plug it back in.");
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        stopAllMotors();
    }

    private void stopAllMotors() {
        for (DcMotor motor : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive}) {
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
