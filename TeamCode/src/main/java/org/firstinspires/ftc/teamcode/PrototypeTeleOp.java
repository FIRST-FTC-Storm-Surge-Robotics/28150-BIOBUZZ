package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/*
 * First prototype TeleOp with field-oriented mecanum drive.
 *
 * Controls (gamepad1):
 *   Left stick        - translate (field relative)
 *   Right stick X     - rotate
 *   Options / Back    - reset heading (point robot away from driver first)
 *   Hold left bumper  - robot-centric drive (fallback if IMU misbehaves)
 *   Hold right bumper - slow mode
 */
@TeleOp(name = "Prototype TeleOp", group = "Prototype")
public class PrototypeTeleOp extends LinearOpMode {

    // Change these to match how the Control Hub is mounted on the robot.
    static final RevHubOrientationOnRobot.LogoFacingDirection LOGO_DIRECTION =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    static final RevHubOrientationOnRobot.UsbFacingDirection USB_DIRECTION =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;

    static final double SLOW_MODE_SPEED = 0.4;
    // Mecanum wheels strafe less efficiently than they drive forward.
    static final double STRAFE_CORRECTION = 1.1;

    private DcMotor frontLeftDrive;
    private DcMotor frontRightDrive;
    private DcMotor backLeftDrive;
    private DcMotor backRightDrive;
    private IMU imu;

    @Override
    public void runOpMode() {
        frontLeftDrive = hardwareMap.get(DcMotor.class, "front_left_drive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "front_right_drive");
        backLeftDrive = hardwareMap.get(DcMotor.class, "back_left_drive");
        backRightDrive = hardwareMap.get(DcMotor.class, "back_right_drive");

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);

        for (DcMotor motor : new DcMotor[]{frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive}) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(LOGO_DIRECTION, USB_DIRECTION)));
        imu.resetYaw();

        telemetry.addLine("Initialized. Point robot away from driver, then press START.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.options || gamepad1.back) {
                imu.resetYaw();
            }

            double y = -gamepad1.left_stick_y;
            double x = gamepad1.left_stick_x;
            double rx = gamepad1.right_stick_x;

            boolean robotCentric = gamepad1.left_bumper;
            boolean slowMode = gamepad1.right_bumper;
            double speed = slowMode ? SLOW_MODE_SPEED : 1.0;

            double heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

            double rotX = x;
            double rotY = y;
            if (!robotCentric) {
                rotX = x * Math.cos(-heading) - y * Math.sin(-heading);
                rotY = x * Math.sin(-heading) + y * Math.cos(-heading);
            }
            rotX *= STRAFE_CORRECTION;

            double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(rx), 1);
            double frontLeftPower = (rotY + rotX + rx) / denominator * speed;
            double backLeftPower = (rotY - rotX + rx) / denominator * speed;
            double frontRightPower = (rotY - rotX - rx) / denominator * speed;
            double backRightPower = (rotY + rotX - rx) / denominator * speed;

            frontLeftDrive.setPower(frontLeftPower);
            backLeftDrive.setPower(backLeftPower);
            frontRightDrive.setPower(frontRightPower);
            backRightDrive.setPower(backRightPower);

            telemetry.addData("Heading (deg)", "%.1f", Math.toDegrees(heading));
            telemetry.addData("Drive mode", robotCentric ? "ROBOT" : "FIELD");
            telemetry.addData("Slow mode", slowMode ? "ON" : "OFF");
            telemetry.addData("Front L/R", "%.2f / %.2f", frontLeftPower, frontRightPower);
            telemetry.addData("Back  L/R", "%.2f / %.2f", backLeftPower, backRightPower);
            telemetry.addLine();
            telemetry.addLine("Options/Back: reset heading");
            telemetry.addLine("Hold LB: robot-centric | Hold RB: slow mode");
            telemetry.update();
        }
    }
}
