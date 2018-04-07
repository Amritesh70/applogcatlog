package citylink.com.applogcatloglibrary;

import org.json.JSONObject;

/**
 * Created by Amritesh Sinha on 4/3/2018.
 */

public class SaveValues {
    private String id;
    private String status;
    private JSONObject requestString;
    private String date;

    public SaveValues() {

    }

    public SaveValues(JSONObject jsonObject) {
        this.requestString = jsonObject;
    }

    public SaveValues(JSONObject jsonObject, String status) {
        this.status = status;
        this.requestString = jsonObject;
    }

    public SaveValues(String id, JSONObject jsonObject) {
        this.id = id;
        this.requestString = jsonObject;
    }

    public JSONObject getRequestString() {
        return requestString;
    }

    public void setRequestString(JSONObject requestString) {
        this.requestString = requestString;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}

