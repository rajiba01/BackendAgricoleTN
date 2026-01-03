package tn.economic.system.ai;

public class ForecastDemo {
    public static void main(String[] args) throws Exception {
        // Make sure FastAPI is running:
        // uvicorn api:app --reload --host 127.0.0.1 --port 8000
        PythonForecastClient client = new PythonForecastClient("http://127.0.0.1:8000");

        var r7 = client.predict("sfax", 7);
        System.out.println("7 days => " + r7);

        var r30 = client.predict("sfax", 30);
        System.out.println("30 days => " + r30);
    }
}