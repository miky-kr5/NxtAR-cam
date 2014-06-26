package ve.ucv.ciens.ccg.networkdata;

import java.io.Serializable;

public class MotorEvent implements Serializable{
	private static final long serialVersionUID = 9989L;

	public enum motor_t {NONE, MOTOR_A, MOTOR_B, MOTOR_C, MOTOR_AC, RECENTER, ROTATE_90};

	private motor_t motor;
	private byte power;

	public MotorEvent(){
		motor = motor_t.NONE;
		power = 0;
	}

	public void setMotor(motor_t motor){
		this.motor = motor;
	}

	public void setPower(byte power) throws IllegalArgumentException{
		if(power > 100 || power < -100){
			throw new IllegalArgumentException("Motor power must be a number between -100 and 100");
		}else{
			this.power = power;
		}
	}

	public motor_t getMotor(){
		return this.motor;
	}

	public byte getPower(){
		return this.power;
	}
}
