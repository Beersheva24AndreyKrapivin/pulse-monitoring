package telran.monitoring.api;

import org.json.JSONObject;

public record NotificationData(long patientId, 
                                String email, 
                                String notificationText,
                                long timestamp) { 
                                    
    public static NotificationData of(String jsonStr) {
        JSONObject jsonObj = new JSONObject(jsonStr);
        long patientId = jsonObj.getLong("patientId");
        String email = jsonObj.getString("email");
        String notificationText = jsonObj.getString("notificationText");
        long timestamp = jsonObj.getLong("timestamp");
        return new NotificationData(patientId, email, notificationText, timestamp);
    }

    @Override
    public String toString() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("patientId", patientId);
        jsonObj.put("email", email);
        jsonObj.put("notificationText", notificationText);
        jsonObj.put("timestamp", timestamp);
        return jsonObj.toString();
    }
}
