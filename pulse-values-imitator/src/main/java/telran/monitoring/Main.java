package telran.monitoring;

import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.log4j.BasicConfigurator;

public class Main {
    static Logger logger = new LoggerStandard("imitator");
    static final int MIN_PULSE_VALUE = 40;
    static final int MAX_PULSE_VALUE = 240;
    static final long TIMEOUT_SEND = 500; //2
    static final int TIMEOUT_RESPONSE = 10000;
    static final String DEFAULT_HOST = "pulse-receiver-lb-1a0d7d5e32d12467.elb.us-east-1.amazonaws.com";
    static final int DEFAULT_PORT = 5000;
    static final int DEFAULT_N_PATIENTS = 10;
    static final int DEFAULT_N_PACKETS = 100; //100000000
    static final int JUMP_PROB = 10;
    static final int MIN_JUMP_PERCENT = 10;
    static final int MAX_JUMP_PERCENT = 100;
    static final int JUMP_POSITIVE_PROB = 70;
    static final long PATIENT_ID_FOR_INFO_LOGGING = 3;
    static HashMap<Long, Integer> patientIdPulseValue = new HashMap<>();
    static DatagramSocket socket = null;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT_RESPONSE);
        IntStream.rangeClosed(1, DEFAULT_N_PACKETS).forEach(Main::send);
    }

    static void send(int i) {
        SensorData sensor = getRandomSensorData(i);
        String jsonStr = sensor.toString();
        try {
            udpSend(jsonStr);
            Thread.sleep(TIMEOUT_SEND);
        } catch (Exception e) {
            logger.log("severe", "Error sending data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void udpSend(String jsonStr) throws Exception {
        logger.log("finest", String.format("data to be sent is %s", jsonStr));
        byte[] bufferSend = jsonStr.getBytes();
        DatagramPacket packet = new DatagramPacket(bufferSend, bufferSend.length, 
            InetAddress.getByName(DEFAULT_HOST), DEFAULT_PORT);
        socket.send(packet);
        socket.receive(packet);
        String strReceive = new String(packet.getData(), 0, packet.getLength());
        logger.log("finest", String.format("receive: %s", strReceive));
        if (!jsonStr.equals(strReceive)) {
            throw new Exception("received packet doesn't equal the sent one");
        }
    }

    private static SensorData getRandomSensorData(int i) {
        long patientId = getRandomNumber(1, DEFAULT_N_PATIENTS);
        int value = getRandomPulseValue(patientId);
        long timestamp = System.currentTimeMillis();
        SensorData res = new SensorData(patientId, value, timestamp);
        return res;
    }

    private static int getRandomPulseValue(long patientId) {
        int valueRes = patientIdPulseValue.computeIfAbsent(patientId,
				k -> getRandomNumber(MIN_PULSE_VALUE, MAX_PULSE_VALUE));
		if (chance(JUMP_PROB)) {
			valueRes = getValueWithJump(valueRes);
			patientIdPulseValue.put(patientId, valueRes);
		}

		return valueRes;
    }

    private static boolean chance(int prob) {

		return getRandomNumber(0, 99) < prob;
	}
    
    private static int getValueWithJump(int previousValue) {
		int jumpPercent = getRandomNumber(MIN_JUMP_PERCENT, MAX_JUMP_PERCENT);
		int jumpValue = previousValue * jumpPercent / 100;
		if (!chance(JUMP_POSITIVE_PROB)) {
			jumpValue = -jumpValue;
		}
		int res = previousValue + jumpValue;
		if (res < MIN_PULSE_VALUE) {
			res = MIN_PULSE_VALUE;
		} else if (res > MAX_PULSE_VALUE) {
			res = MAX_PULSE_VALUE;
		}
		return res;
	}

    private static int getRandomNumber(int minValue, int maxValue) {
        return new Random().nextInt(minValue, maxValue + 1);
    }
}