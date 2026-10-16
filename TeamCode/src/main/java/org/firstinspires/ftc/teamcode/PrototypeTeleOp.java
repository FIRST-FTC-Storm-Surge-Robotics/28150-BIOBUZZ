package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
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
public class PrototypeTeleOp extends LinearOpMode {

    static final double SLOW_MODE_SPEED = 0.4;
    // Mecanum wheels strafe less efficiently than they drive forward.
    static final double STRAFE_CORRECTION = 1.1;

    private DcMotor frontLeftDrive;
    private DcMotor frontRightDrive;
    private DcMotor backLeftDrive;
    private DcMotor backRightDrive;

    @Override
    public void runOpMode() {
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
        }

        telemetry.addLine("Initialized. Press START.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            double y = -gamepad1.left_stick_y;
            // Negated because this drivetrain strafes opposite to the standard mecanum layout.
            double x = -gamepad1.left_stick_x * STRAFE_CORRECTION;
            double rx = gamepad1.right_stick_x;

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
            telemetry.addData("Front L/R", "%.2f / %.2f", frontLeftPower, frontRightPower);
            telemetry.addData("Back  L/R", "%.2f / %.2f", backLeftPower, backRightPower);
            telemetry.addLine();
            telemetry.addLine("Hold RB: slow mode");
            telemetry.update();
        }
    }
}
